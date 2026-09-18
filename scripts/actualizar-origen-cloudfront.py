"""Reapunta el origen /api/* de CloudFront a la IP publica actual del task de ECS.

Fargate asigna una IP publica nueva en cada despliegue, asi que despues de actualizar el
servicio hay que mover el origen del backend en CloudFront. El origen del frontend (el bucket
S3) no se toca: se detecta el del backend como el unico cuyo dominio no es de S3.

Uso:
    python actualizar-origen-cloudfront.py <ip-publica-del-task>

La IP se obtiene asi:
    aws ecs list-tasks --cluster erp-cluster --service-name erp-backend-service --region us-east-1
    aws ecs describe-tasks --cluster erp-cluster --tasks <task> --region us-east-1
    aws ec2 describe-network-interfaces --network-interface-ids <eni> --region us-east-1
"""
import json
import subprocess
import sys
from pathlib import Path

REGION = 'us-east-1'
DISTRIBUCION = 'E391I4EET1AU1X'
SUFIJO = '.compute-1.amazonaws.com'
ARCHIVO_CONFIG = Path('cloudfront-distribution-config.json')


def aws(*args):
    resultado = subprocess.run(
        ['aws', '--region', REGION, '--no-cli-pager', *args],
        capture_output=True, text=True, shell=True)
    if resultado.returncode != 0:
        raise SystemExit('AWS fallo:\n' + resultado.stdout + resultado.stderr)
    return resultado.stdout


def main():
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)

    dominio = 'ec2-' + sys.argv[1].strip().replace('.', '-') + SUFIJO

    datos = json.loads(aws('cloudfront', 'get-distribution-config',
                           '--id', DISTRIBUCION, '--output', 'json'))
    etag = datos['ETag']
    configuracion = datos['DistributionConfig']

    origen_anterior = None
    for origen in configuracion['Origins']['Items']:
        if 's3' not in origen['DomainName']:
            origen_anterior = origen['Id']
            origen['Id'] = dominio
            origen['DomainName'] = dominio

    if origen_anterior is None:
        raise SystemExit('No se encontro el origen del backend en la distribucion')

    for comportamiento in configuracion.get('CacheBehaviors', {}).get('Items', []):
        if comportamiento['TargetOriginId'] == origen_anterior:
            comportamiento['TargetOriginId'] = dominio

    ARCHIVO_CONFIG.write_text(json.dumps(configuracion), encoding='utf-8')
    aws('cloudfront', 'update-distribution', '--id', DISTRIBUCION,
        '--distribution-config', 'file://' + ARCHIVO_CONFIG.resolve().as_posix(),
        '--if-match', etag, '--output', 'json')
    ARCHIVO_CONFIG.unlink(missing_ok=True)

    print('origen anterior:', origen_anterior)
    print('origen nuevo:   ', dominio)
    print('CloudFront propaga el cambio en unos minutos; verificar /api/health')


if __name__ == '__main__':
    main()
