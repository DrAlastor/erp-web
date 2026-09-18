# HU-03 — Roles y Permisos (RBAC) — Plan de Implementación

> **Ejecución:** este plan lo ejecuta Claude en la misma sesión, de forma inline, tarea por tarea con TDD y un commit por tarea. Los pasos usan casillas (`- [ ]`) para seguimiento.

**Objetivo:** Implementar el control de acceso basado en roles del ERP: 24 permisos, 7 roles con su matriz, verificación en cada petición del backend y restricción real del menú y las acciones en el frontend Angular.

**Arquitectura:** `ServicioAutorizacion` es la única fuente de verdad de los permisos efectivos (unión de los permisos de los roles activos del usuario, con caché invalidable). Se expone como `PermissionEvaluator` de Spring Security, así los endpoints se protegen con `@PreAuthorize("hasPermission(...)")` y el 403 lo emite el framework. El JWT lleva identidad y empresa, nunca permisos.

**Stack:** Java 21 / Spring Boot 3.3.5 / Gradle 8.11 / PostgreSQL 16 / Flyway / JJWT 0.12.6 · Angular 22 SSR / TypeScript 6 / Vitest.

**Spec:** `docs/superpowers/specs/2026-09-18-hu03-roles-permisos-design.md`

## Restricciones globales

- Rama **`dev-santiago`**, ya actualizada a `main` por fast-forward. Nada se sube hasta que Santiago lo pruebe.
- Migraciones Flyway con **versión fechada** (`V20260918_1__`, `V20260918_2__`) para no colisionar con una `V2__` de otro integrante.
- Módulos: `SEGURIDAD`, `COMERCIAL`, `INVENTARIO`, `CONTABILIDAD`, `FACTURACION`, `REPORTES`. Acciones: `CREAR`, `CONSULTAR`, `MODIFICAR`, `ANULAR`. Código = `MODULO_ACCION`.
- Totales exactos de la matriz del documento: ADMINISTRADOR 24, CONTADOR 9, AUDITOR_INTERNO 6, CAJERO 6, GERENTE_GENERAL 5, ENCARGADO_ALMACEN 5, PREVENTISTA 3.
- Los 7 roles son **de sistema**: no se crean ni se borran roles. Se edita su matriz y se activa/desactiva.
- `spring.jpa.hibernate.ddl-auto=validate` — el esquema lo define Flyway, nunca Hibernate.
- Los permisos **no** viajan en el JWT.
- El backend es la autoridad: el frontend solo oculta o deshabilita.
- Angular renderiza en servidor (SSR): todo acceso a `localStorage` va detrás de una guarda de plataforma, o el build se rompe.
- Nombres de dominio en español (`Rol`, `Permiso`, `UsuarioRol`, `ServicioAutorizacion`), igual que el diagrama de clases del documento.

---

### Tarea 1: Esquema RBAC y semilla de la matriz

**Archivos:**
- Crear: `backend/src/main/resources/db/migration/V20260918_1__seguridad_rbac.sql`
- Crear: `backend/src/main/resources/db/migration/V20260918_2__seguridad_demo.sql`
- Test: `backend/src/test/java/com/uagrm/erp/backend/seguridad/MatrizSemillaTest.java`

**Interfaces:**
- Produce: las tablas `empresa`, `usuario`, `permiso`, `rol`, `rol_permiso`, `usuario_rol`, `bitacora_auditoria`; el catálogo de 24 permisos; los 7 roles con su matriz; la empresa demo con los usuarios `admin@demo.bo`, `cajero@demo.bo`, `auditor@demo.bo`.

La semilla es el punto donde un error de tipeo hace que el sistema contradiga el documento entregado, y no hay base de datos en los tests. Por eso el test **lee el archivo SQL de migración desde el classpath** y verifica la matriz contra los totales del documento.

- [ ] **Paso 1: Escribir el test que falla** — `MatrizSemillaTest` lee `db/migration/V20260918_1__seguridad_rbac.sql` y afirma: hay 24 `INSERT` de permisos, uno por cada par módulo×acción; `ADMINISTRADOR` tiene los 24; los totales de los otros 6 roles son los de las restricciones globales; `AUDITOR_INTERNO` solo tiene permisos que terminan en `_CONSULTAR`; `CAJERO` no tiene ningún permiso de `SEGURIDAD`.
- [ ] **Paso 2: Correr y verificar que falla** — `./gradlew test --tests "*MatrizSemillaTest"` → FALLA porque el archivo no existe.
- [ ] **Paso 3: Escribir las dos migraciones** — DDL con UUID y claves foráneas, único `(empresa_id, codigo)` en `rol`, PK compuesta en las tablas puente; catálogo de permisos; los 7 roles con `es_sistema = true`; la matriz; y en la segunda migración la empresa demo con tres usuarios (hash BCrypt de contraseñas de desarrollo).
- [ ] **Paso 4: Correr los tests** — pasan.
- [ ] **Paso 5: Commit.**

---

### Tarea 2: Entidades JPA y repositorios

**Archivos:**
- Crear: `backend/src/main/java/com/uagrm/erp/backend/seguridad/dominio/{Empresa,Usuario,Permiso,Rol,UsuarioRol,BitacoraEvento}.java`
- Crear: `backend/src/main/java/com/uagrm/erp/backend/seguridad/repositorio/{UsuarioRepositorio,RolRepositorio,PermisoRepositorio,UsuarioRolRepositorio,BitacoraRepositorio}.java`

**Interfaces:**
- Consume: el esquema de la Tarea 1.
- Produce:
  - `PermisoRepositorio.findAllByOrderByModuloAscAccionAsc() : List<Permiso>`
  - `RolRepositorio.findByEmpresaIdOrderByNombre(UUID empresaId) : List<Rol>`
  - `RolRepositorio.findByIdAndEmpresaId(UUID id, UUID empresaId) : Optional<Rol>`
  - `UsuarioRolRepositorio.findPermisosEfectivos(UUID usuarioId) : List<String>` — consulta JPQL que une `usuario_rol → rol → rol_permiso → permiso`, filtrando `rol.activo = true`, y devuelve los **códigos** de permiso.
  - `UsuarioRolRepositorio.existsByUsuarioIdAndRolId(UUID, UUID) : boolean`
  - `UsuarioRepositorio.findByEmailAndActivoTrue(String email) : Optional<Usuario>`

Los tests de repositorio necesitarían base de datos; el contrato de estas consultas queda cubierto por los mocks de la Tarea 3 y por `ddl-auto=validate` más la verificación manual de la Tarea 11.

- [ ] **Paso 1: Escribir las entidades** — mapeo explícito de tablas y columnas, sin `ddl-auto` generando nada. `UsuarioRol` con clave compuesta embebida.
- [ ] **Paso 2: Escribir los repositorios** con las firmas de arriba.
- [ ] **Paso 3: Compilar** — `./gradlew compileJava` → sin errores.
- [ ] **Paso 4: Commit.**

---

### Tarea 3: ServicioAutorizacion y permisos efectivos

**Archivos:**
- Crear: `backend/src/main/java/com/uagrm/erp/backend/seguridad/ServicioAutorizacion.java`
- Test: `backend/src/test/java/com/uagrm/erp/backend/seguridad/ServicioAutorizacionTest.java`

**Interfaces:**
- Consume: `UsuarioRolRepositorio.findPermisosEfectivos`.
- Produce:
  - `Set<String> permisosEfectivos(UUID usuarioId)`
  - `boolean tienePermiso(UUID usuarioId, String codigoPermiso)`
  - `void invalidarCache(UUID usuarioId)` y `void invalidarCacheDeRol(UUID rolId)`

- [ ] **Paso 1: Escribir los tests que fallan** (repositorio simulado con Mockito):
  - la unión de dos roles devuelve los permisos de ambos, sin duplicados
  - `tienePermiso` es `false` para un código que no está
  - un rol inactivo no aporta permisos (el repositorio ya lo filtra: el test fija esa expectativa)
  - dos llamadas seguidas a `permisosEfectivos` consultan el repositorio **una sola vez** (caché)
  - después de `invalidarCache`, la siguiente llamada vuelve a consultar
  - `invalidarCacheDeRol` invalida a todos los usuarios que tienen ese rol
  - el conjunto devuelto es inmutable
- [ ] **Paso 2: Correr y verificar que fallan.**
- [ ] **Paso 3: Implementar** con caché en `ConcurrentHashMap`.
- [ ] **Paso 4: Correr los tests** — pasan.
- [ ] **Paso 5: Commit.**

---

### Tarea 4: Enganche con Spring Security y respuesta 403

**Archivos:**
- Crear: `backend/src/main/java/com/uagrm/erp/backend/seguridad/config/EvaluadorDePermisos.java`
- Crear: `backend/src/main/java/com/uagrm/erp/backend/seguridad/config/ConfiguracionMetodosSeguros.java`
- Crear: `backend/src/main/java/com/uagrm/erp/backend/seguridad/web/ManejadorAccesoDenegado.java`
- Modificar: `backend/src/main/java/com/uagrm/erp/backend/config/SecurityConfig.java`
- Test: `backend/src/test/java/com/uagrm/erp/backend/seguridad/EvaluadorDePermisosTest.java`

**Interfaces:**
- Consume: `ServicioAutorizacion`, `UsuarioPrincipal` (Tarea 5).
- Produce: la expresión `hasPermission('MODULO','ACCION')` utilizable en `@PreAuthorize`, y un cuerpo JSON único de acceso denegado: `{"error":"ACCESO_DENEGADO","mensaje":"No tiene permiso para realizar esta operación","permisoRequerido":"<CODIGO>"}` con estado 403.
- `SecurityConfig` pasa de `permitAll()` a **denegar por defecto**: solo `/api/auth/login`, `/api/health/**`, `/api/public/**` y `/error` quedan abiertos.

- [ ] **Paso 1: Escribir los tests que fallan** — el evaluador compone `MODULO_ACCION` y delega en `ServicioAutorizacion`; devuelve `false` si no hay usuario autenticado; devuelve `false` (nunca excepción) si la expresión trae basura.
- [ ] **Paso 2: Correr y verificar que fallan.**
- [ ] **Paso 3: Implementar** el evaluador, registrarlo en el `MethodSecurityExpressionHandler`, escribir el manejador de acceso denegado y cerrar `SecurityConfig`.
- [ ] **Paso 4: Correr los tests** — pasan.
- [ ] **Paso 5: Commit.**

---

### Tarea 5: Autenticación mínima con JWT (paquete reemplazable)

**Archivos:**
- Crear: `backend/src/main/java/com/uagrm/erp/backend/auth/{ServicioJwt,FiltroJwt,UsuarioPrincipal,ControladorAuth,ServicioAutenticacion}.java`
- Crear: `backend/src/main/java/com/uagrm/erp/backend/auth/LEEME.md` — aviso de que la CU-01 reemplaza este paquete
- Modificar: `backend/build.gradle` (JJWT 0.12.6), `backend/src/main/resources/application.properties` (secreto y expiración por variable de entorno con valor por defecto de desarrollo)
- Test: `backend/src/test/java/com/uagrm/erp/backend/auth/{ServicioJwtTest,ControladorAuthTest}.java`

**Interfaces:**
- Produce:
  - `UsuarioPrincipal` = `record UsuarioPrincipal(UUID usuarioId, UUID empresaId, String nombre, String email)`
  - `ServicioJwt.generar(UsuarioPrincipal) : String` y `ServicioJwt.leer(String token) : Optional<UsuarioPrincipal>`
  - `POST /api/auth/login` con cuerpo `{email, password}` → `200 {token, nombre, empresaId}` o `401 {"error":"CREDENCIALES_INVALIDAS"}`
  - Un ayudante estático `UsuarioActual.obtener() : UsuarioPrincipal` para que los controladores tomen el usuario y la empresa del contexto.

- [ ] **Paso 1: Escribir los tests que fallan** — ida y vuelta del token conservando los cuatro campos; token con firma inválida → vacío; token expirado → vacío; basura → vacío (sin excepción); login correcto → 200 con token; contraseña mala → 401; usuario inactivo → 401.
- [ ] **Paso 2: Correr y verificar que fallan.**
- [ ] **Paso 3: Implementar** con BCrypt y JJWT HS256; el filtro deja el `UsuarioPrincipal` autenticado en el contexto.
- [ ] **Paso 4: Correr los tests** — pasan.
- [ ] **Paso 5: Commit.**

---

### Tarea 6: Endpoints de seguridad y reglas de negocio

**Archivos:**
- Crear: `backend/src/main/java/com/uagrm/erp/backend/seguridad/web/{ControladorRoles,ControladorAsignaciones,ControladorMisPermisos}.java`
- Crear: `backend/src/main/java/com/uagrm/erp/backend/seguridad/web/dto/` — DTOs de rol, permiso, matriz y asignación
- Crear: `backend/src/main/java/com/uagrm/erp/backend/seguridad/ServicioRoles.java`, `ServicioAsignaciones.java`
- Crear: `backend/src/main/java/com/uagrm/erp/backend/seguridad/ExcepcionesSeguridad.java` + `@RestControllerAdvice`
- Test: `backend/src/test/java/com/uagrm/erp/backend/seguridad/{ControladorRolesTest,ControladorAsignacionesTest,ServicioAsignacionesTest}.java`

**Interfaces:**
- Consume: repositorios (T2), `ServicioAutorizacion` (T3), `UsuarioActual` (T5).
- Produce los endpoints de la tabla del spec §4. `GET /mis-permisos` devuelve `{permisos: [...], nombre, roles: [...]}`.

Reglas que los tests fijan: rol inactivo no otorga permisos; no se asigna dos veces el mismo rol; no se asigna un rol de otra empresa; todo cambio de asignación escribe en `bitacora_auditoria`; guardar la matriz de un rol invalida la caché de sus usuarios.

- [ ] **Paso 1: Escribir los tests que fallan** (`@WebMvcTest` con servicios simulados y `spring-security-test`):
  - `GET /api/seguridad/roles` con `SEGURIDAD_CONSULTAR` → 200 con los 7 roles
  - el mismo endpoint sin ese permiso → **403** con cuerpo `ACCESO_DENEGADO`
  - sin token → 401
  - `PUT /api/seguridad/roles/{id}/permisos` con `SEGURIDAD_MODIFICAR` → 200 y se invalida la caché
  - asignar un rol ya asignado → 409 `ROL_YA_ASIGNADO` (no 500)
  - asignar un rol de otra empresa → 404 `ROL_NO_ENCONTRADO`
  - asignar correctamente → 201 y se escribe el evento en la bitácora
  - `GET /api/seguridad/mis-permisos` autenticado sin permisos especiales → 200
- [ ] **Paso 2: Correr y verificar que fallan.**
- [ ] **Paso 3: Implementar** controladores, servicios, DTOs y el manejador de excepciones.
- [ ] **Paso 4: Correr toda la batería del backend** — `./gradlew test` en verde.
- [ ] **Paso 5: Commit.**

---

### Tarea 7: Núcleo de autenticación en el frontend

**Archivos:**
- Crear: `frontend/src/app/core/auth/{auth.service.ts,auth.interceptor.ts,auth.model.ts}`
- Crear: `frontend/src/app/core/permisos/permisos.service.ts`
- Modificar: `frontend/src/app/app.config.ts` (`provideHttpClient(withFetch(), withInterceptors([...]))`)
- Modificar: `frontend/src/environments/environment.development.ts` (`production: false`, `apiUrl: 'http://localhost:8080/api'`)
- Modificar: `frontend/src/app/app.spec.ts` — **está roto de antes**: afirma un `<h1>Hello, frontend</h1>` que ya no existe en `app.html`. Se corrige para que la batería pueda correr.
- Test: `frontend/src/app/core/auth/auth.service.spec.ts`, `frontend/src/app/core/permisos/permisos.service.spec.ts`

**Interfaces:**
- Produce:
  - `AuthService`: `login(email, password) : Observable<Sesion>`, `logout()`, `token() : string | null`, `estaAutenticado : Signal<boolean>`, `nombre : Signal<string | null>`
  - `PermisosService`: `cargar() : Observable<void>`, `permisos : Signal<ReadonlySet<string>>`, `tiene(codigo: string) : boolean`, `limpiar()`
  - `authInterceptor` — agrega `Authorization: Bearer <token>` cuando hay token

- [ ] **Paso 1: Escribir los tests que fallan** — sin `localStorage` (render de servidor) el servicio no explota y `estaAutenticado` es `false`; el login guarda el token y deja `estaAutenticado` en `true`; `logout` lo borra y limpia los permisos; el interceptor agrega la cabecera solo si hay token; `tiene()` es `false` antes de cargar.
- [ ] **Paso 2: Correr y verificar que fallan** — `npm test`.
- [ ] **Paso 3: Implementar** con guardas de `isPlatformBrowser`.
- [ ] **Paso 4: Correr los tests** — pasan.
- [ ] **Paso 5: Commit.**

---

### Tarea 8: Guards y directiva de visibilidad (tareas 6 y 7 de la HU)

**Archivos:**
- Crear: `frontend/src/app/core/guards/{auth.guard.ts,permiso.guard.ts}`
- Crear: `frontend/src/app/shared/si-permiso.directive.ts`
- Test: `frontend/src/app/core/guards/permiso.guard.spec.ts`, `frontend/src/app/shared/si-permiso.directive.spec.ts`

**Interfaces:**
- Produce: `authGuard : CanActivateFn`; `permisoGuard(codigo: string) : CanActivateFn`; directiva `*siPermiso="'CODIGO'"`.

- [ ] **Paso 1: Escribir los tests que fallan** — sin sesión, `authGuard` redirige a `/login`; con el permiso, `permisoGuard` deja pasar; sin el permiso, redirige a `/app/sin-acceso`; la directiva no renderiza el contenido sin el permiso y sí lo renderiza con él.
- [ ] **Paso 2: Correr y verificar que fallan.**
- [ ] **Paso 3: Implementar.**
- [ ] **Paso 4: Correr los tests** — pasan.
- [ ] **Paso 5: Commit.**

---

### Tarea 9: Login y shell logueado con menú según permisos

**Archivos:**
- Crear: `frontend/src/app/pages/login/login.component.{ts,html,scss}`
- Crear: `frontend/src/app/layout/shell.component.{ts,html,scss}`
- Crear: `frontend/src/app/layout/menu.ts` — los ítems del menú, cada uno declarando el permiso que exige
- Crear: `frontend/src/app/pages/sin-acceso/sin-acceso.component.ts`
- Modificar: `frontend/src/app/app.routes.ts` (rutas `/login`, `/app/**` protegidas), `frontend/src/app/app.routes.server.ts` (la zona logueada se renderiza en cliente, no se prerenderiza)
- Test: `frontend/src/app/layout/shell.component.spec.ts`, `frontend/src/app/pages/login/login.component.spec.ts`

**Interfaces:**
- Consume: `AuthService`, `PermisosService`, `authGuard`, `permisoGuard`.
- Produce: `MENU_ITEMS : ReadonlyArray<{ etiqueta, ruta, icono, permiso }>` y el componente `ShellComponent` que filtra ese menú por los permisos efectivos.

Estilo: se reutilizan las variables CSS de `styles.scss` (`--primary`, `--bg-surface`, `--text-main`, `--font-sans`). No se introduce un lenguaje visual nuevo.

- [ ] **Paso 1: Escribir los tests que fallan** — con solo permisos de comercial, el menú **no** muestra "Seguridad"; como administrador sí lo muestra; el login con credenciales malas muestra el mensaje de error y no navega.
- [ ] **Paso 2: Correr y verificar que fallan.**
- [ ] **Paso 3: Implementar** las pantallas, el menú y las rutas.
- [ ] **Paso 4: Correr los tests** — pasan. Y `npm run build` sin errores de SSR.
- [ ] **Paso 5: Commit.**

---

### Tarea 10: Pantalla de Roles y Permisos

**Archivos:**
- Crear: `frontend/src/app/pages/seguridad/roles/roles.component.{ts,html,scss}`
- Crear: `frontend/src/app/pages/seguridad/roles/roles.service.ts`
- Crear: `frontend/src/app/pages/seguridad/roles/asignar-rol.modal.{ts,html}`
- Test: `frontend/src/app/pages/seguridad/roles/roles.component.spec.ts`

**Interfaces:**
- Consume: los endpoints de la Tarea 6.
- Produce: `RolesService` con `listar()`, `catalogoPermisos()`, `guardarMatriz(rolId, codigos)`, `cambiarEstado(rolId, activo)`, `asignarRol(usuarioId, rolId)`.

- [ ] **Paso 1: Escribir los tests que fallan** — se dibuja la matriz con 6 módulos × 4 acciones; al marcar una casilla y guardar, el servicio recibe exactamente los códigos marcados; sin `SEGURIDAD_MODIFICAR` las casillas están deshabilitadas y el botón de guardar no aparece (`*siPermiso`); un 403 del backend se muestra como mensaje de acceso denegado.
- [ ] **Paso 2: Correr y verificar que fallan.**
- [ ] **Paso 3: Implementar.**
- [ ] **Paso 4: Correr toda la batería del frontend** — `npm test` en verde, `npm run build` sin errores.
- [ ] **Paso 5: Commit.**

---

### Tarea 11: Verificación de punta a punta y nota para el grupo

**Archivos:**
- Crear: `docs/seguridad/HU-03-verificacion.md` — qué se probó, con qué usuarios y qué se vio
- Crear: `docs/seguridad/HU-03-aviso-al-grupo.md` — territorio compartido con CU-01, CU-02 y CU-04

- [ ] **Paso 1:** `docker compose up -d postgres`, `./gradlew bootRun` — las migraciones aplican y `ddl-auto=validate` no protesta (esto valida el mapeo de la Tarea 2 contra el esquema real).
- [ ] **Paso 2:** `npm run dev` y entrar como **administrador** — menú completo, matriz con las 24 casillas.
- [ ] **Paso 3:** Editar la matriz de un rol y comprobar que el cambio se refleja **sin reiniciar ni volver a entrar** (la caché se invalidó).
- [ ] **Paso 4:** Entrar como **cajero** — menú recortado, sin la sección de Seguridad.
- [ ] **Paso 5:** Pedir a mano un endpoint restringido con el token del cajero — **403** con el cuerpo de acceso denegado.
- [ ] **Paso 6:** Asignar un rol y verificar el evento en `bitacora_auditoria`.
- [ ] **Paso 7:** Escribir los dos documentos y commitear. **No se sube nada al remoto.**

---

## Autorrevisión del plan

**Cobertura del spec:** §3 modelo de datos → T1, T2. §3 semilla y demo → T1. §4 ServicioAutorizacion y caché → T3. §4 enganche y 403 → T4. §4 endpoints → T6. §4 multi-inquilino → T2 (consultas con `empresaId`), T6 (rol de otra empresa → 404). §5 autenticación mínima → T5. §6 frontend → T7, T8, T9, T10. §7 pruebas → distribuidas en cada tarea. §8 verificación manual → T11. §9 coordinación → T11. §10 riesgos → mitigados en T1 (versión fechada), T5 (paquete aislado), T7/T9 (SSR), T4 (denegar por defecto).

**Huecos encontrados y cerrados:** la corrección de `VENTA_ANULAR` → `COMERCIAL_ANULAR` del spec §3 no tenía tarea propia; queda como parte del catálogo de T1, y la corrección del `.docx` y de la Figura 3 se anota como pendiente aparte, fuera del código.

**Consistencia de tipos:** `UsuarioPrincipal` (T5) es lo que consumen `EvaluadorDePermisos` (T4) y `UsuarioActual` (T6). `findPermisosEfectivos` devuelve `List<String>` de códigos (T2) y `permisosEfectivos` devuelve `Set<String>` (T3). En el frontend, `PermisosService.permisos` es `Signal<ReadonlySet<string>>` y es lo que consultan el guard (T8), la directiva (T8) y el menú (T9).
