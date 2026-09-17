# Correcciones de CU-01: Iniciar sesión

Cambios preparados en `revision-ramaERPNico` e incorporados en `dev-prueba` tras integrar `main` y `ramaERPNico`. No se ejecutaron migraciones ni se conectó con RDS. No se ha integrado esta rama en `main` ni se ha desplegado el backend.

## Backend

- Login busca el identificador sin espacios exteriores y conserva la contraseña exactamente como se envía.
- Un bloqueo vencido reinicia el contador antes del siguiente intento: ya no vuelve a bloquear con un único error.
- La búsqueda para login utiliza un bloqueo de escritura dentro de la transacción, para serializar intentos concurrentes sobre una cuenta. La persistencia de los contadores y el comportamiento del bloqueo SQL siguen pendientes de verificación con PostgreSQL real.
- Refresh rechaza usuarios deshabilitados y mantiene la validación de sesión abierta y no expirada.
- El filtro JWT verifica firma/expiración una sola vez y comprueba que el usuario existe y sigue activo. Usa sus permisos actuales de base de datos para autorizar peticiones.
- Las rutas protegidas devuelven 401 JSON cuando falta autenticación, distinguiéndolo del 403 por permisos insuficientes.
- Se mantiene el contrato de logout: cierra la sesión de refresh. Un access token ya emitido puede durar hasta su expiración (15 minutos por defecto); no se implementó una lista de revocación por sesión.

## Frontend

- Detecta expiración del access token y renueva antes de entrar a la ruta privada o enviar una petición a la API.
- Comparte una única petición de refresh entre solicitudes simultáneas.
- Ante un 401 de la API, refresca y reintenta una sola vez. Un 403 de la petición de negocio no cierra sesión.
- Un rechazo de refresh limpia la sesión; un error temporal de red conserva el refresh token para permitir un intento posterior.
- Una respuesta tardía de refresh no restaura una sesión cerrada ni reemplaza un login nuevo.
- Solo adjunta Bearer a peticiones de la API configurada, excluyendo endpoints de autenticación.
- «Recordarme» usa localStorage; sin marcarlo utiliza sessionStorage. Ambos se limpian al salir.
- La comprobación de permisos de interfaz utiliza las authorities del JWT. La autorización efectiva se mantiene en el backend; las authorities de interfaz se actualizan con el siguiente token.
- El formulario diferencia errores de conexión/servidor de credenciales incorrectas y respeta el mensaje de cuenta bloqueada.
- `/app` y sus rutas hijas se renderizan en el navegador. Se evita generar durante el build una redirección estática a `/login` para todos los usuarios.
- Se corrigió la prueba antigua del título inicial de Angular para verificar el contenedor de rutas actual.

## Verificación

- Backend: 21 pruebas pasan (10 de servicio, 4 de filtro JWT y 7 de endpoints). Se genera `backend/build/libs/app.jar`.
- Frontend: 20 pruebas pasan; build de producción correcto.
- No se ejecutó la prueba integral `BackendApplicationTests.contextLoads`, porque requiere PostgreSQL. Los repositorios están simulados en las pruebas de CU-01; las pruebas de endpoints sí usan la cadena de seguridad y MVC.
- No se validaron login ni migraciones contra la base desplegada, ni la integración del sitio público con un backend desplegado.

Comandos usados:

```text
backend: gradlew.bat test --tests '*AuthServiceTest' --tests '*JwtAuthenticationFilterTest' --tests '*AuthControllerTest' bootJar --no-daemon
frontend: npm.cmd test -- --watch=false
frontend: npm.cmd run build
```

## Pendientes antes de usar AWS

1. Exportar el esquema real de `erp_database` y comprobar compatibilidad de las entidades y migraciones. `ERPDB.sql` es referencia del sprint, no instalador oficial.
2. Validar los flujos contra una base de prueba con ese esquema, incluyendo persistencia de los intentos fallidos y concurrencia.
3. Configurar credenciales y una clave JWT privada en el entorno real; completar la ruta del certificado RDS del `.env.example`.
4. Confirmar los permisos presentes en RDS. CU-01 los consume, pero estas correcciones no inventan un catálogo ni implementan HU-02/HU-03.
5. Desplegar el backend y conectar `/api` del frontend con él. La ruta privada requiere que el hosting sirva el documento de la aplicación para navegación directa, en lugar de responder 404.

La configuración de AWS en `.env.example` documenta el endpoint, puerto 5432, base `erp_database` y origen del frontend. No se alteraron el esquema, los usuarios ni las credenciales reales de la base.
