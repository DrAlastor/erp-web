# HU-03 — Roles y Permisos — Qué se verificó

**Fecha:** 18 de septiembre de 2026
**Rama:** `dev-santiago`
**Entorno:** Windows 11 · JDK 21.0.4 · Node 22.23.2 · PostgreSQL 16 en Docker (puerto 5433)

---

## Pruebas automáticas

| Suite | Pruebas | Resultado |
|---|---|---|
| Backend (`./gradlew test`) | **103** | Todas en verde |
| Frontend (`ng test`) | **38** | Todas en verde |
| Build de producción (`ng build`) | — | Limpio; prerenderiza solo la landing |

Desglose del backend:

| Clase | Pruebas | Qué fija |
|---|---|---|
| `MatrizRolesDeSistemaTest` | 16 | Los totales exactos de la matriz del documento y sus reglas |
| `EndpointsDeSeguridadTest` | 14 | 200 con permiso, 403 sin permiso, 401 sin token, 409, 404 |
| `ServicioAutorizacionTest` | 11 | Unión de roles, rol inactivo, caché e invalidación |
| `ServicioRolesTest` | 11 | Edición de matriz, estado, aislamiento entre empresas |
| `CatalogoPermisosTest` | 9 | Los 24 permisos y sus códigos |
| `ServicioAprovisionamientoTest` | 9 | Sembrado idempotente del catálogo y los roles |
| `EvaluadorDePermisosTest` | 9 | El enganche con `@PreAuthorize`, fallando cerrado |
| `ServicioAsignacionesTest` | 7 | Las validaciones del diagrama de actividad |
| `ServicioAutenticacionTest` | 5 | Credenciales, usuario inactivo, normalización del email |
| `ControladorAuthTest` | 5 | Contrato HTTP del login |
| `ServicioJwtTest` | 6 | Ida y vuelta del token, firma, vencimiento, y que **no lleve permisos** |
| `BackendApplicationTests` | 1 | Arranque real: Flyway + `validate` del mapeo |

> Nota: `BackendApplicationTests.contextLoads` **fallaba antes de esta rama** porque no había
> base de datos levantada. Con `docker compose up -d postgres` pasa.

---

## Verificación manual contra la base de datos real

Todo lo de abajo se ejecutó contra el backend corriendo (`./gradlew bootRun`) y PostgreSQL 16
en Docker, no contra dobles de prueba.

### Esquema y sembrado

- Flyway aplicó `20260918.1` (`success = t`).
- `ddl-auto=validate` aprobó el mapeo de las 7 entidades contra el esquema real.
- Quedaron sembrados los **24 permisos**, 4 por cada uno de los 6 módulos.
- Se sembró la empresa demo con los tres usuarios y sus roles.

### Los 7 roles con los totales del documento

`GET /api/seguridad/roles` con el token del administrador devolvió:

| Rol | Permisos | Documento |
|---|---|---|
| ADMINISTRADOR | 24 | 24 ✓ |
| CONTADOR | 9 | 9 ✓ |
| CAJERO | 6 | 6 ✓ |
| AUDITOR_INTERNO | 6 | 6 ✓ |
| ENCARGADO_ALMACEN | 5 | 5 ✓ |
| GERENTE_GENERAL | 5 | 5 ✓ |
| PREVENTISTA | 3 | 3 ✓ |

### Autenticación

| Caso | Resultado |
|---|---|
| Login del administrador | `200` con token |
| Login del cajero | `200` con token |
| Contraseña equivocada | `401 CREDENCIALES_INVALIDAS` |

### Control de acceso — los criterios de aprobación de la HU

| Caso | Resultado |
|---|---|
| `GET /api/seguridad/roles` **sin token** | `401 NO_AUTENTICADO` |
| `GET /api/seguridad/roles` con token del **administrador** | `200` con los 7 roles |
| `GET /api/seguridad/roles` con token del **cajero** | **`403 ACCESO_DENEGADO`** |

### Recálculo de permisos sin volver a iniciar sesión

Esta es la prueba del diagrama de actividad ("recalcula los permisos efectivos"):

1. `GET /mis-permisos` con el token del cajero → **6 permisos**.
2. El administrador le agrega `INVENTARIO_MODIFICAR` a la matriz del rol Cajero.
3. `GET /mis-permisos` **con el mismo token de antes** → **7 permisos**, incluido el nuevo.

El cambio tuvo efecto en la petición siguiente, sin cerrar ni volver a abrir sesión. Es la
consecuencia de que los permisos no viajen dentro del JWT.

### Reglas de negocio

| Intento | Resultado |
|---|---|
| Quitarle a Administrador los permisos de seguridad | `409 ADMINISTRADOR_SIN_SEGURIDAD` |
| Desactivar el rol Administrador | `409 ADMINISTRADOR_NO_SE_DESACTIVA` |
| Asignar al cajero un rol que ya tiene | `409 ROL_YA_ASIGNADO` |
| Guardar el permiso inexistente `VENTA_ANULAR` | `409 PERMISO_DESCONOCIDO` |

### Bitácora de auditoría

Cada edición dejó su fila en `bitacora_auditoria` con la acción, el detalle y la hora:

```
MATRIZ_MODIFICADA | Matriz del rol CAJERO actualizada a 7 permisos | 10:40:11
MATRIZ_MODIFICADA | Matriz del rol CAJERO actualizada a 6 permisos | 10:40:26
```

Al terminar, la matriz del Cajero se restauró a los 6 permisos del documento.

### Frontend

- `ng serve` levanta y sirve `200` en `/`, `/login` y `/app/seguridad/roles`.
- `ng build` compila sin errores y prerenderiza **solo** la landing: el área logueada se
  resuelve en el cliente, que es lo que corresponde porque depende de la sesión.

---

## Lo que NO se verificó, y hay que hacer a mano

**El recorrido visual con clics.** Esta sesión no tenía forma de manejar un navegador ni de
sacar capturas, así que la parte de "ver la pantalla" queda pendiente. La lógica está
cubierta por las pruebas de componente (el menú filtrado y las 24 casillas de la matriz),
pero conviene verlo y sacar las capturas para la defensa.

Pasos, con todo ya levantado:

1. `docker compose up -d postgres`
2. `cd backend && ./gradlew bootRun`
3. `cd frontend && npm run dev` → `http://localhost:4200/login`
4. Entrar como **admin@demo.bo / Admin123&#42;**
   - El menú tiene que mostrar todos los módulos, incluido *Roles y permisos*.
   - La pantalla de roles muestra los 7 roles y la matriz de 24 casillas.
   - Marcar una casilla, guardar, y ver el aviso con el total nuevo.
5. Cerrar sesión y entrar como **cajero@demo.bo / Cajero123&#42;**
   - El menú **no** debe mostrar *Roles y permisos* ni *Contabilidad*.
   - Escribir a mano `http://localhost:4200/app/seguridad/roles` debe llevar a *Sin acceso*.
6. Entrar como **auditor@demo.bo / Auditor123&#42;**
   - Ve *Roles y permisos* (tiene `SEGURIDAD_CONSULTAR`), pero la matriz está en modo
     consulta: casillas deshabilitadas y sin botón de guardar.

Ese último caso es el más elocuente para la defensa: el mismo usuario ve la pantalla y no
puede modificarla, y si forzara la petición el backend respondería 403.
