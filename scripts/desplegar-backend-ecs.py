#!/usr/bin/env python3
"""
Automatiza el despliegue del backend en AWS ECS Fargate y actualiza CloudFront:
1. Registra una nueva revisión de la Task Definition con la imagen indicada.
2. Actualiza el servicio ECS 'erp-backend-service' forzando nuevo despliegue.
3. Espera a que el nuevo task esté RUNNING y obtiene su IP pública.
4. Actualiza el origen /api/* de CloudFront (E391I4EET1AU1X) con la nueva IP.
5. Invalida la caché de CloudFront para /api/*.
"""
import argparse
import json
import subprocess
import sys
import time
from pathlib import Path

REGION = "us-east-1"
CLUSTER = "erp-cluster"
SERVICE = "erp-backend-service"
TASK_FAMILY = "erp-backend-task"
DISTRIBUTION_ID = "E391I4EET1AU1X"
SUFIJO_DNS = ".compute-1.amazonaws.com"


def run_aws(*args):
    cmd = ["aws", "--region", REGION, "--no-cli-pager", *args]
    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0:
        raise RuntimeError(f"Error ejecutando AWS CLI ({' '.join(args)}):\n{res.stderr}\n{res.stdout}")
    return res.stdout.strip()


def registrar_nueva_revision(image_uri):
    print(f"[*] Obteniendo task definition actual de {TASK_FAMILY}...")
    datos = json.loads(run_aws("ecs", "describe-task-definition", "--task-definition", TASK_FAMILY))
    td = datos["taskDefinition"]

    # Eliminar campos de solo lectura no admitidos en register-task-definition
    for campo in [
        "taskDefinitionArn", "revision", "status", "requiresAttributes",
        "compatibilities", "registeredAt", "registeredBy"
    ]:
        td.pop(campo, None)

    print(f"[*] Actualizando imagen a: {image_uri}")
    td["containerDefinitions"][0]["image"] = image_uri

    temp_file = Path("td-temp.json")
    try:
        temp_file.write_text(json.dumps(td, indent=2), encoding="utf-8")
        nuevo = json.loads(run_aws("ecs", "register-task-definition", "--cli-input-json", f"file://{temp_file.resolve().as_posix()}"))
        nueva_arn = nuevo["taskDefinition"]["taskDefinitionArn"]
        nueva_revision = nuevo["taskDefinition"]["revision"]
        print(f"[+] Nueva revisión registrada: {TASK_FAMILY}:{nueva_revision} ({nueva_arn})")
        return nueva_arn
    finally:
        if temp_file.exists():
            temp_file.unlink()


def actualizar_servicio(task_def_arn):
    print(f"[*] Actualizando servicio {SERVICE} en clúster {CLUSTER}...")
    run_aws(
        "ecs", "update-service",
        "--cluster", CLUSTER,
        "--service", SERVICE,
        "--task-definition", task_def_arn,
        "--force-new-deployment"
    )
    print("[+] Despliegue forzado iniciado.")


def esperar_nuevo_task_y_obtener_ip(max_espera_seg=360):
    print("[*] Esperando a que el nuevo task de ECS inicie y obtenga IP pública...")
    inicio = time.time()
    task_visto = None

    while time.time() - inicio < max_espera_seg:
        tasks = json.loads(run_aws("ecs", "list-tasks", "--cluster", CLUSTER, "--service-name", SERVICE))
        task_arns = tasks.get("taskArns", [])
        if task_arns:
            desc = json.loads(run_aws("ecs", "describe-tasks", "--cluster", CLUSTER, "--tasks", *task_arns))
            for t in desc.get("tasks", []):
                last_status = t.get("lastStatus")
                attachments = t.get("attachments", [])
                eni_id = None
                for att in attachments:
                    if att.get("type") == "ElasticNetworkInterface":
                        for detail in att.get("details", []):
                            if detail.get("name") == "networkInterfaceId":
                                eni_id = detail.get("value")
                                break
                if last_status == "RUNNING" and eni_id:
                    # Obtener IP pública desde EC2
                    eni_info = json.loads(run_aws("ec2", "describe-network-interfaces", "--network-interface-ids", eni_id))
                    nis = eni_info.get("NetworkInterfaces", [])
                    if nis:
                        assoc = nis[0].get("Association", {})
                        public_ip = assoc.get("PublicIp")
                        if public_ip:
                            print(f"[+] Task {t['taskArn']} está RUNNING con IP pública: {public_ip}")
                            return public_ip
        time.sleep(10)
        print("    ...esperando despliegue de ECS (10s)")

    raise TimeoutError("Se agotó el tiempo esperando que el task de ECS obtenga IP pública.")


def actualizar_cloudfront(public_ip):
    dominio = f"ec2-{public_ip.strip().replace('.', '-')}{SUFIJO_DNS}"
    print(f"[*] Actualizando origen de CloudFront ({DISTRIBUTION_ID}) a {dominio}...")

    datos = json.loads(run_aws("cloudfront", "get-distribution-config", "--id", DISTRIBUTION_ID))
    etag = datos["ETag"]
    configuracion = datos["DistributionConfig"]

    origen_anterior = None
    for origen in configuracion["Origins"]["Items"]:
        if "s3" not in origen["DomainName"]:
            origen_anterior = origen["Id"]
            origen["Id"] = dominio
            origen["DomainName"] = dominio

    if origen_anterior is None:
        raise RuntimeError("No se encontró el origen del backend en CloudFront")

    for comportamiento in configuracion.get("CacheBehaviors", {}).get("Items", []):
        if comportamiento["TargetOriginId"] == origen_anterior:
            comportamiento["TargetOriginId"] = dominio

    temp_file = Path("cloudfront-temp.json")
    try:
        temp_file.write_text(json.dumps(configuracion), encoding="utf-8")
        run_aws(
            "cloudfront", "update-distribution",
            "--id", DISTRIBUTION_ID,
            "--distribution-config", f"file://{temp_file.resolve().as_posix()}",
            "--if-match", etag
        )
        print(f"[+] CloudFront actualizado exitosamente: origen {origen_anterior} -> {dominio}")
    finally:
        if temp_file.exists():
            temp_file.unlink()

    print("[*] Creando invalidación de CloudFront para /api/*...")
    run_aws("cloudfront", "create-invalidation", "--distribution-id", DISTRIBUTION_ID, "--paths", "/api/*")
    print("[+] Invalidación solicitada.")


def main():
    parser = argparse.ArgumentParser(description="Desplegar backend en AWS ECS Fargate y apuntar CloudFront.")
    parser.add_argument("--image", required=True, help="URI completa de la imagen Docker en ECR")
    args = parser.parse_args()

    nueva_td_arn = registrar_nueva_revision(args.image)
    actualizar_servicio(nueva_td_arn)
    nueva_ip = esperar_nuevo_task_y_obtener_ip()
    actualizar_cloudfront(nueva_ip)
    print("\n========================================================")
    print(f"¡DESPLIEGUE FINALIZADO CON ÉXITO!")
    print(f"Backend corriendo en: http://{nueva_ip}:8080/api/health")
    print(f"API disponible en:   https://erp-contable-comercial.online/api/health")
    print("========================================================\n")


if __name__ == "__main__":
    main()
