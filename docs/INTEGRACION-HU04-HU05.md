# Integración de HU-04 (clientes) y HU-05 (catálogo de artículos)

Ramas integradas: `dev-javier` (commit `1e8248d`) y `feature/HU-05-catalogo` (`d36f392`), sobre
`dev-alessandro` con CU01/CU02/CU03/CU06 ya integrados.

## Dónde quedó cada cosa

| Traído de | Ubicación final |
|---|---|
| HU-04 backend | `modulo/comercial_y_preventa/gestion_de_clientes` (se agregó `mapper/ClienteMapper`) |
| HU-04 frontend | `frontend/src/app/modulos/comercial-y-preventa/gestion-de-clientes` |
| HU-05 backend | `modulo/inventario_y_almacenes/catalogo_de_articulos` (se agregaron `dto/` y `mapper/ArticuloMapper`) |
| HU-05 frontend | `frontend/src/app/modulos/inventario-y-almacenes/catalogo-de-articulos` |
| `Categoria` (la comparten HU-05 y HU-06) | `modulo/inventario_y_almacenes/compartido` |

Los paquetes se normalizaron a minúsculas (`modulo/Comercial_y_Preventa/Gestion_de_Clientes` →
`modulo/comercial_y_preventa/gestion_de_clientes`) para que las rutas coincidan en Linux y en CI.

## Autorización

Los endpoints nuevos usan el catálogo de la CU03 con `hasPermission`:

- Clientes: `COMERCIAL` con `CONSULTAR`, `CREAR` y `MODIFICAR`.
- Catálogo de artículos: `INVENTARIO` con `CONSULTAR`, `CREAR` y `MODIFICAR`.
- Portal del cliente (`/api/perfil`): conserva `CLIENTE:PERFIL:LECTURA`, un permiso de la tabla
  legacy de CU01, porque el cliente externo no es un empleado de la empresa y no está en la
  matriz de 7 roles. **Pendiente:** decidir si el portal entra al catálogo como módulo propio.

## Base de datos

Las migraciones de las ramas traídas se renumeraron con versión fechada porque chocaban con las
existentes (dos `V2`, dos `V3`):

| Migración | Qué hace |
|---|---|
| `V20260918_2__comercial_clientes.sql` | Crea `clientes` (HU-04). Reemplaza al `ddl-auto=update` que traía la rama de Javier. |
| `V20260918_3__inventario_catalogo_articulos.sql` | Crea `articulos` (HU-05). No recrea `categorias`: ya la crea `V4`. |
| `V20260918_4__inventario_catalogo_seed.sql` | Datos de demostración del catálogo; idempotente. |

Se retiró `spring.jpa.hibernate.ddl-auto=update` de `application-local.properties`: el esquema lo
administra Flyway y la aplicación arranca con `validate`.

## Pendientes de decisión (no de código)

1. **HU-05 y HU-06 modelan lo mismo dos veces**: `articulos` (HU-05) y `productos` (HU-06) con
   `categorias` compartida. Hay que decidir si el catálogo maestro es `productos` o `articulos`
   antes de conectar ventas o facturación, que consumirán uno solo.
2. **Portal del cliente**: pasar `CLIENTE:PERFIL:LECTURA` al catálogo de CU03 (implica decidir si
   aparece en la matriz de roles).
3. Los datos de demostración de HU-05 agregan tres categorías nuevas (`Electrónica`, `Oficina`,
   `Alimentos`) además de las tres de `V4`; conviene unificar el vocabulario en una sola lista.

## Verificación

- Backend: `gradlew clean test bootJar` con perfil `local` → 17 suites, 144 pruebas en verde.
- Frontend: `npm test -- --watch=false` → 9 archivos, 50 pruebas en verde; `npm run build` (AOT)
  aprobado.
