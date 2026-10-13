# Despliegue del backend en AWS

Estado actual del despliegue del backend ERP (verificado el 18 de septiembre de 2026).

## Arquitectura

```text
Usuario
  │  https://erp-contable-comercial.online
  ▼
CloudFront  (distribución E391I4EET1AU1X, cuenta 621203631700)
  ├── /       → S3  erp-contable-comercial.s3-website-us-east-1.amazonaws.com (frontend Angular)
  └── /api/*  → task de ECS Fargate: ec2-<ip>.compute-1.amazonaws.com:8080 (backend Spring Boot)
                     │
                     ▼
                Amazon RDS PostgreSQL 18.6  erp-database.coxoq4mwa6sg.us-east-1.rds.amazonaws.com
```

| Pieza | Recurso |
|---|---|
| Frontend | Bucket S3 `erp-contable-comercial` + CloudFront `E391I4EET1AU1X` |
| Backend | ECS Fargate: clúster `erp-cluster`, servicio `erp-backend-service`, task `erp-backend-task` (0.5 vCPU, 1 GB) |
| Imagen | ECR `621203631700.dkr.ecr.us-east-1.amazonaws.com/erp-backend` |
| Logs | CloudWatch Logs `/ecs/erp-backend` (retención 7 días) |
| Base de datos | RDS `erp-database` (PostgreSQL 18.6, `db.t4g.micro`, 20 GB) |
| Migraciones | Flyway, dueño del esquema (`spring.jpa.hibernate.ddl-auto=validate`) |

El despliegue del **frontend** es automático: todo `push` a `main` que toque `frontend/**` dispara
`.github/workflows/deploy-frontend.yml` (build, `aws s3 sync` e invalidación de CloudFront).
El **backend** se despliega a mano con los pasos de abajo.

## Desplegar una versión nueva del backend

```powershell
# 1. Construir y verificar el jar (la suite incluye el arranque real con Flyway + validate)
cd backend
.\gradlew.bat clean test bootJar

# 2. Publicar la imagen en ECR (tag único por versión)
cd ..
docker build -t erp-backend:<version> ./backend          # usa backend/Dockerfile (multi-stage)
$reg = '621203631700.dkr.ecr.us-east-1.amazonaws.com'
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin $reg
docker tag erp-backend:<version> "$reg/erp-backend:<version>"
docker push "$reg/erp-backend:<version>"

# 3. Registrar la nueva revisión de la task definition apuntando a esa imagen
#    (copiar la revisión actual, cambiar solo containerDefinitions[0].image y registrar)
aws ecs describe-task-definition --task-definition erp-backend-task --region us-east-1
aws ecs register-task-definition --cli-input-json file://td-nuevo.json --region us-east-1

# 4. Redesplegar el servicio
aws ecs update-service --cluster erp-cluster --service erp-backend-service `
  --task-definition erp-backend-task:<revision-nueva> --force-new-deployment --region us-east-1

# 5. Ver el arranque (Flyway, Tomcat, errores)
aws logs describe-log-streams --log-group-name /ecs/erp-backend --region us-east-1
aws logs get-log-events --log-group-name /ecs/erp-backend --log-stream-name <stream> --region us-east-1

# 6. IMPRESCINDIBLE: reapuntar el origen /api/* a la IP pública nueva del task
python scripts/actualizar-origen-cloudfront.py <ip-publica-nueva>

# 7. Verificar
curl https://erp-contable-comercial.online/api/health
```

Para obtener la IP pública del task:

```powershell
$t = aws ecs list-tasks --cluster erp-cluster --service-name erp-backend-service --region us-east-1 --query 'taskArns[0]' --output text
$eni = aws ecs describe-tasks --cluster erp-cluster --tasks $t --region us-east-1 --query 'tasks[0].attachments[0].details[?name==`networkInterfaceId`]|[0].value' --output text
aws ec2 describe-network-interfaces --network-interface-ids $eni --region us-east-1 --query 'NetworkInterfaces[0].Association.PublicIp' --output text
```

## Cosas que hay que saber

1. **La IP pública del task cambia en cada despliegue.** Fargate no admite IP fija, así que
   CloudFront apunta a `ec2-<ip>.compute-1.amazonaws.com`. Si se olvida el paso 6, el sitio queda
   cargando y la API responde 404/502. Solución estable (opcional): poner un **Application Load
   Balancer** delante del servicio (~16-20 USD/mes) o automatizar el paso 6 en un workflow.
2. **El esquema lo administra Flyway**: `ddl-auto=validate`. Si una entidad no coincide con las
   migraciones, el task no arranca (es lo deseado: avisa antes de corromper datos).
3. **Variables de entorno del task**: `SPRING_DATASOURCE_URL/USERNAME/PASSWORD`,
   `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`, `JWT_SECRET`, `CORS_ORIGIN`, `SEED_ADMIN_EMAIL`,
   `SEED_ADMIN_PASSWORD`, `TZ`. Están en texto plano en la task definition; conviene moverlas a
   Secrets Manager o Parameter Store.
4. **El usuario semilla** lo crea `InitializerSeeder` con `SEED_ADMIN_EMAIL` y
   `SEED_ADMIN_PASSWORD` (por defecto `admin@erp.com` / `Admin123!`). Cambiar la contraseña en
   producción y luego actualizar el rol/permisos desde la pantalla de Roles.
5. **El Dockerfile del backend** necesitaba `bash` (Alpine solo trae `sh`) y normalizar los
   finales de línea del wrapper (`gradlew` viene con CRLF desde Windows). Ambas cosas están
   corregidas; si se construye desde Linux no hace falta el `sed`.
6. **Categorías duplicadas**: la migración de inventario (`V4`) siembra tres categorías y la de
   HU-05 (`V20260918_4`) otras tres. Es la decisión de modelo pendiente entre `articulos` y
   `productos` (ver `INTEGRACION-HU04-HU05.md`).
