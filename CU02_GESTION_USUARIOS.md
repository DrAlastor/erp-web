# CU02 - Gestionar usuarios

Disponible desde Seguridad y Auditoria > Gestion de Usuarios en el panel de control.

## Alcance

Listado paginado (15 cuentas), busqueda por nombre/usuario/correo, filtros por rol y estado, detalle, edicion de nombre completo y correo, activacion y desactivacion con confirmacion. No incluye crear cuentas, cambiar contrasenas o asignar roles. Los campos protegidos enviados a los endpoints se rechazan con 400; las respuestas no incluyen hashes, tokens ni contrasenas.

El backend exige un rol ADMIN o ADMINISTRADOR y permiso SEGURIDAD:USUARIOS:ESCRITURA o ACCESO:USUARIOS:ESCRITURA en cada operacion. Todos los empleados siguen viendo el dashboard; la administracion de usuarios devuelve 403 si no cumple estas condiciones. No se permite desactivar la propia cuenta.

## API

- GET /api/usuarios?search=&role=&enable=&page=0&size=15
- GET /api/usuarios/roles (solo opciones para filtros)
- GET /api/usuarios/{id}
- PUT /api/usuarios/{id}: fullname, email
- PATCH /api/usuarios/{id}/status: enable

## Auditoria y base de datos

La migracion V3 crea el contrato local bitacora_logs y asigna el permiso de escritura a los roles administrativos. Cada cambio efectivo registra accion, fecha, responsable, entidad, id e IP en la misma transaccion. Al desactivar una cuenta se cierran sus sesiones de renovacion; el filtro JWT ya comprueba su estado en cada solicitud.

No se ha conectado ni modificado AWS. Antes de desplegar, comparar este contrato con la BITACORA oficial y el historial Flyway de la base completa. ERPDB.sql permanece como referencia del sprint.

## Prueba local

Reiniciar el backend con npm.cmd run start:dev. Mantener el frontend con npm.cmd run dev. Cerrar sesion e iniciar nuevamente con el administrador para obtener los permisos actualizados en el JWT. Los usuarios disponibles son los que existan en la base local; CU02 no crea cuentas de ejemplo.
