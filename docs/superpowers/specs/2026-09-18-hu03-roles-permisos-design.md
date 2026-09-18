# HU-03 — Gestión de Roles y Permisos (RBAC) — Diseño

**Fecha:** 18 de septiembre de 2026
**Historia:** HU-03 / CU-03 — Gestionar Roles y Permisos (Sprint 1)
**Responsable:** Arteaga Silva Geimbert Santiago — Seguridad / Autorización
**Rama:** `dev-santiago` (actualizada a `main`: Gradle 8.11 / JDK 21)
**Documento de sprint:** `Desktop\HU-03 Roles y Permisos\HU-03 Roles y Permisos.docx` (sección 3.2.1.3.4)

---

## 1. Punto de partida

El repositorio tiene el andamiaje, no el sistema:

- **Backend:** `BackendApplication`, `HealthController`, un `SecurityConfig` con `permitAll()` a todo y una migración `V1__init_baseline.sql` con una única tabla de sello. No existen entidades, ni usuarios, ni autenticación.
- **Frontend:** Angular 22 con SSR y una sola ruta (la landing). El `CompanyService` trabaja con tres empresas *mockeadas en `localStorage`*. No hay login, ni zona logueada, ni cliente HTTP, ni interceptor.
- **Dependencias externas a la HU:** CU-01 (autenticación JWT) y CU-02 (usuarios) son de otros integrantes y todavía no están.

Por eso esta HU no es un cambio acotado sobre un flujo existente: construye el subsistema de autorización y, con él, el mínimo de identidad que necesita para poder demostrarse.

## 2. Alcance

**Entra:**

1. Modelo de datos RBAC completo (permiso, rol, rol_permiso, usuario_rol) más el mínimo de `empresa` y `usuario`.
2. Catálogo de 24 permisos y los 7 roles del documento, con su matriz sembrada.
3. `ServicioAutorizacion` con permisos efectivos y caché invalidable.
4. Protección declarativa de endpoints y respuesta 403 uniforme.
5. Autenticación mínima con JWT, aislada y reemplazable por la CU-01.
6. Frontend: login, layout logueado con menú según permisos, pantalla de Roles y Permisos, guards y directiva de visibilidad.
7. Registro del evento de asignación en una bitácora mínima.

**No entra (y por qué):**

- **Creación de roles nuevos.** Decisión tomada: los 7 roles son fijos. "Gestionar" se cumple editando su matriz de permisos y activándolos o desactivándolos. El modelo queda preparado para agregarlo (un endpoint y un botón).
- **CRUD de usuarios** — es la CU-02.
- **Autenticación definitiva, refresh tokens, recuperación de contraseña** — es la CU-01.
- **Bitácora consultable** — es la CU-04 (Sprint 3). Acá solo se escribe el evento.
- Cualquier refactor no relacionado con la autorización.

## 3. Modelo de datos

Migración **`V20260918_1__seguridad_rbac.sql`**. La versión lleva fecha a propósito: si un compañero sube su propia `V2__` en paralelo, Flyway aborta por versión repetida.

| Tabla | Propósito | Notas |
|---|---|---|
| `empresa` | Inquilino (tenant) | Mínima: id, nombre, nit, activo. Territorio compartido — ver §9 |
| `usuario` | Identidad | id, empresa_id, email, password_hash (BCrypt), nombre, activo |
| `permiso` | Catálogo global | `codigo` único (`MODULO_ACCION`), `modulo`, `accion`, descripción |
| `rol` | Rol por empresa | empresa_id, codigo, nombre, activo, `es_sistema` |
| `rol_permiso` | La matriz | PK compuesta (rol_id, permiso_id) |
| `usuario_rol` | Asignación | PK compuesta, más `asignado_por` y `asignado_en` |
| `bitacora_auditoria` | Evento de auditoría | Mínima: empresa_id, usuario_id, accion, detalle, fecha |

Claves: `empresa_id` en `rol` y `usuario`; índice único `(empresa_id, codigo)` en `rol`. Los ids son UUID (la extensión `uuid-ossp` ya viene habilitada en `V1`).

### Corrección respecto del documento

Las reglas de negocio del `.docx` ejemplifican el código `VENTA_ANULAR`, pero "VENTA" no es uno de los módulos de la matriz. Se adoptan los **6 módulos de la matriz** y el `VENTA_ANULAR` del diagrama de secuencia pasa a ser **`COMERCIAL_ANULAR`**. Pendiente: corregir esa línea del documento y regenerar la Figura 3 para que documento y código coincidan.

### Catálogo de permisos (24)

Módulos: `SEGURIDAD`, `COMERCIAL`, `INVENTARIO`, `CONTABILIDAD`, `FACTURACION`, `REPORTES`.
Acciones: `CREAR`, `CONSULTAR`, `MODIFICAR`, `ANULAR`.
Código = `MODULO_ACCION` (ej. `SEGURIDAD_MODIFICAR`).

### Matriz sembrada (idéntica al documento)

| Rol | SEGURIDAD | COMERCIAL | INVENTARIO | CONTABILIDAD | FACTURACION | REPORTES | Total |
|---|---|---|---|---|---|---|---|
| `ADMINISTRADOR` | C L M A | C L M A | C L M A | C L M A | C L M A | C L M A | 24 |
| `GERENTE_GENERAL` | — | L | L | L | L | L | 5 |
| `CONTADOR` | — | L | L | C L M A | L | C L | 9 |
| `CAJERO` | — | C L | L | — | C L A | — | 6 |
| `ENCARGADO_ALMACEN` | — | L | C L M | — | — | L | 5 |
| `PREVENTISTA` | — | C L | L | — | — | — | 3 |
| `AUDITOR_INTERNO` | L | L | L | L | L | L | 6 |

### Datos de demostración

Migración aparte, **`V20260918_2__seguridad_demo.sql`**, separada para que el grupo la pueda borrar de un golpe: una empresa demo y tres usuarios —administrador, cajero y auditor— con contraseñas evidentes de desarrollo. Sirven para mostrar en vivo cómo cambia el menú y cómo salta el 403.

## 4. Autorización en el backend

**`ServicioAutorizacion`** (la clase de la Figura 1) es la única fuente de verdad:

- `permisosEfectivos(usuarioId)` → unión de los permisos de todos los roles **activos** del usuario, dentro de su empresa.
- `tienePermiso(usuarioId, codigo)` → se apoya en lo anterior.
- **Caché por usuario**, invalidada cuando se modifica la matriz de un rol, se activa/desactiva un rol, o se asigna/quita una asignación. Esto implementa el paso "recalcula los permisos efectivos" de la Figura 4; es la razón por la que los permisos **no** viajan dentro del JWT.

**Enganche a Spring Security:** el servicio se expone como `PermissionEvaluator`, de modo que los endpoints se protegen declarativamente:

```java
@PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
```

Con `@EnableMethodSecurity`, un `AccessDeniedHandler` y un `@RestControllerAdvice` que devuelven siempre el mismo cuerpo JSON de acceso denegado con estado **403**. Esto evita que la autorización dependa de que nadie se olvide de escribir la verificación a mano en un método.

**Multi-inquilino:** el `empresa_id` se toma del JWT y toda consulta lo filtra explícitamente. Sin filtros implícitos de Hibernate, para que el aislamiento sea visible y testeable.

### Endpoints (`/api/seguridad`)

| Método | Ruta | Permiso exigido |
|---|---|---|
| GET | `/roles` | `SEGURIDAD_CONSULTAR` |
| PUT | `/roles/{id}/permisos` | `SEGURIDAD_MODIFICAR` |
| PATCH | `/roles/{id}/estado` | `SEGURIDAD_MODIFICAR` |
| GET | `/permisos` | `SEGURIDAD_CONSULTAR` |
| GET | `/usuarios/{id}/roles` | `SEGURIDAD_CONSULTAR` |
| POST | `/usuarios/{id}/roles` | `SEGURIDAD_CREAR` |
| DELETE | `/usuarios/{id}/roles/{rolId}` | `SEGURIDAD_ANULAR` |
| GET | `/mis-permisos` | solo autenticado |

`/mis-permisos` es lo que consume Angular para armar el menú.

### Reglas de negocio a respetar

- Un rol inactivo no otorga ningún permiso, aunque siga asignado.
- No se puede asignar dos veces el mismo rol al mismo usuario (Figura 4).
- No se puede asignar un rol de otra empresa.
- Todo cambio de asignación deja su evento en `bitacora_auditoria`.

## 5. Autenticación mínima (el punto de sutura)

`POST /api/auth/login` devuelve un JWT HS256 con `usuarioId`, `empresaId` y nombre — **sin permisos adentro**. Contraseñas con BCrypt. Un `JwtAuthFilter` deja un `UsuarioPrincipal` en el contexto de seguridad.

Todo vive aislado en el paquete `auth`, documentado como provisional. **La autorización no depende de cómo se autenticó nadie**, solo de que exista un `UsuarioPrincipal` con id y empresa. Cuando aterrice la CU-01, se reemplaza ese paquete sin tocar el RBAC.

## 6. Frontend

```
core/auth/      AuthService (login, token con guarda de SSR), authInterceptor
core/permisos/  PermisosService — carga /mis-permisos como signal
core/guards/    authGuard, permisoGuard('CODIGO')
shared/         directiva *siPermiso
layout/         shell logueado: menú armado desde ítems que declaran su permiso
pages/login/    pantalla de acceso
pages/seguridad/roles/   lista de roles, editor de matriz, modal de asignación
```

- **Tarea 6 de la HU** → `authGuard` + `permisoGuard` en las rutas.
- **Tarea 7 de la HU** → menú filtrado por permisos efectivos y `*siPermiso` sobre los botones de acción.
- El backend sigue siendo la autoridad: el frontend solo oculta o deshabilita.
- Cuidado con SSR: `localStorage` no existe en el servidor. El `AuthService` debe funcionar sin él en el render del servidor.
- Estilo: se reutilizan los tokens SCSS y la tipografía que ya usa la landing (*Light Tech Premium*, Plus Jakarta Sans). No se introduce un lenguaje visual nuevo.

## 7. Pruebas

TDD sobre lo que se va a defender. Tests sin base de datos, para que no dependan de que Docker esté arriba:

**`ServicioAutorizacion`** (repositorios simulados)
- unión de permisos de varios roles, sin duplicados
- un rol inactivo no otorga permisos
- la caché se invalida al cambiar la matriz de un rol y al asignar o quitar un rol
- un usuario no ve roles de otra empresa

**Endpoints** (`@WebMvcTest` + `spring-security-test`)
- con permiso → 200
- sin permiso → **403** con el cuerpo de acceso denegado
- sin token → 401
- asignar un rol ya asignado → error de negocio, no 500

**Reglas de asignación**
- el evento queda en la bitácora
- no se puede asignar un rol de otra empresa

## 8. Verificación manual (antes de declarar nada terminado)

1. `docker compose up -d postgres`, `./gradlew bootRun`, `npm run dev`.
2. Entrar como **administrador**: menú completo, pantalla de roles con la matriz de las 24 casillas.
3. Editar la matriz de un rol y comprobar que el cambio se refleja sin reiniciar ni volver a entrar.
4. Entrar como **cajero**: menú recortado, sin la sección de seguridad.
5. Pedir a mano un endpoint restringido con el token del cajero: **403** con el mensaje de acceso denegado.
6. Asignar un rol a un usuario y ver el evento en `bitacora_auditoria`.

## 9. Entrega y coordinación con el grupo

- Todo en `dev-santiago`, ya puesta al día con `main`. **No se sube nada hasta que Santiago lo pruebe.**
- Al subir, avisar al grupo que esta rama introduce territorio compartido:
  - **`empresa`** — la landing ya tiene un modal de empresas mockeado; hay que unificar.
  - **`usuario`** y el paquete **`auth`** — CU-01 y CU-02 los reemplazan.
  - **`bitacora_auditoria`** — base para la CU-04.
- El desajuste de horas del Sprint Backlog del grupo (4,5 h asignadas contra 24 h estimadas para esta HU) sigue pendiente de conversación y no afecta este diseño.

## 10. Riesgos

| Riesgo | Mitigación |
|---|---|
| Colisión de versiones de Flyway con otro integrante | Versiones con fecha |
| CU-01 reemplaza el login y rompe la autorización | El RBAC solo depende de `UsuarioPrincipal`; el paquete `auth` está aislado |
| `localStorage` en SSR rompe el build de Angular | Guardas de plataforma en `AuthService`; se verifica con `npm run build` |
| Un endpoint nuevo queda sin proteger | Protección declarativa y denegación por defecto en `SecurityConfig` |
