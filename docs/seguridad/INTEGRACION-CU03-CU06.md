# Integracion en dev-alessandro

Origen: dev-leonardo (f3a5670, HU06) y dev-santiago (e1dd9e7, HU03).

## Carpetas y navegacion

- Backend CU03: `modulo/seguridad_y_auditoria/roles_y_permisos`.
- Frontend CU03: `modulos/seguridad-y-auditoria/roles-y-permisos`.
- Backend CU06: `modulo/inventario_y_almacenes/movimientos_de_inventario`.
- Frontend CU06: `modulos/inventario-y-almacenes/movimientos-de-inventario`.
- Pruebas backend en las mismas rutas relativas bajo `src/test/java`.
- El panel conserva el orden del catalogo compartido. CU03 y CU06 se abren desde sus funciones del menu existente.

## Compatibilidad entre casos de uso

Se conserva el login, refresh y logout de CU01. El principal contiene la identidad necesaria para CU03; no se instala el login alternativo ni las cuentas demo de Santiago.

CU03 lee las cuentas existentes de la tabla `usuarios`, manteniendo IDs Long, nombres, correos, contrasenas y estado de CU01/CU02. Roles y empresas de CU03 conservan IDs UUID. La migracion agrega una empresa inicial para las cuentas actuales, sin crear usuarios duplicados.

El arranque aprovisiona los 24 permisos y siete roles de sistema, e importa una sola vez las asignaciones anteriores: ADMIN se convierte en ADMINISTRADOR y ALMACENERO en ENCARGADO_ALMACEN. El indicador `usuarios.rbac_inicializado` evita restaurar una asignacion eliminada desde CU03 al reiniciar.

CU02 lista, muestra y filtra los roles de CU03. Su parametro `role` y los IDs de las opciones de rol son ahora UUID. La autorizacion real de CU02 usa SEGURIDAD_MODIFICAR. CU06 exige INVENTARIO_CONSULTAR para consultar e INVENTARIO_CREAR o INVENTARIO_MODIFICAR para registrar movimientos. La interfaz deshabilita el registro cuando solo se permite consultar.

Las migraciones de CU01/CU02 existentes se conservan. Se incorpora V4 de inventario y la migracion fechada de CU03. Los documentos HU-03-verificacion y HU-03-aviso-al-grupo describen la rama original; este documento describe la adaptacion integrada.

## Validacion

Las pruebas se ejecutan sobre una base PostgreSQL local separada de la base de desarrollo. Las pruebas integradas comprueban la identidad compartida tras login real, acceso de consulta sin escritura, revocacion con el mismo JWT, rechazo de cuentas desactivadas y roles de CU03 visibles en CU02.

Resultado: 129 pruebas backend y 47 pruebas frontend aprobadas; build Angular de produccion y bootJar aprobados. Se comprobo tambien una base vacia con todas las migraciones y el aprovisionamiento inicial.

No se realiza despliegue ni push como parte de esta integracion local.
