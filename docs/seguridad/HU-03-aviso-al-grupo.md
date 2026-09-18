# HU-03 (Roles y Permisos) — Aviso para el grupo

Rama: **`dev-santiago`**. Esto es lo que hay que saber antes de integrarla, porque toca
territorio que le corresponde a otras historias.

---

## 1. La rama se puso al día con `main`

`dev-santiago` estaba 3 commits atrás y seguía con **Maven**, mientras `main` ya había
migrado a **Gradle + JDK 21**. Se actualizó por *fast-forward* (sin divergencia, sin
conflictos) antes de empezar. Si alguien tenía esa rama clonada de antes, conviene que la
vuelva a traer.

## 2. Territorio compartido con otras historias

### `empresa` y `usuario` — CU-02 (Leonardo)

La HU-03 necesita saber **quién** pregunta y **de qué empresa** es, así que creó las dos
tablas con lo mínimo:

- `empresa`: id, nombre, nit, activo, creado_en
- `usuario`: id, empresa_id, email (único), password_hash, nombre, activo, creado_en

Cuando la CU-02 traiga el CRUD real, lo más probable es que necesite agregar columnas. **Se
puede extender sin tocar la autorización**: el RBAC solo usa `usuario.id`, `usuario.empresa_id`
y `usuario.activo`.

Ojo con un detalle: la landing ya tiene un modal de empresas con datos **mockeados en
`localStorage`** (`CompanyService`). Eso y la tabla `empresa` todavía no se hablan entre sí;
hay que unificarlos cuando se implemente el registro de empresas.

### El paquete `auth` — CU-01 (autenticación)

`com.uagrm.erp.backend.auth` es **provisional y está pensado para que lo reemplacen**. Tiene
su propio `LEEME.md` con las instrucciones. Lo importante:

> El RBAC no depende del paquete `auth`, solo del tipo `UsuarioPrincipal`
> (`usuarioId`, `empresaId`, `nombre`, `email`). Si la CU-01 sigue dejando un
> `UsuarioPrincipal` en el `SecurityContext`, la autorización sigue funcionando sin que haya
> que tocar nada de `com.uagrm.erp.backend.seguridad`.

Lo que **no** tiene, y le corresponde a la CU-01: refresh tokens, cierre de sesión del lado
del servidor, bloqueo por intentos fallidos, recuperación de contraseña y política de
complejidad.

### `bitacora_auditoria` — CU-04 (Sprint 3)

Se creó la tabla y se escribe en ella (asignar y quitar roles, editar la matriz, cambiar el
estado de un rol). **No** hay endpoint de consulta: eso es la CU-04. Las acciones que ya se
registran están como constantes en `BitacoraEvento`.

## 3. Cosas que cambiaron para todos

### `SecurityConfig` ahora deniega por omisión

Antes hacía `permitAll()` a todo. Ahora solo quedan abiertos `/api/auth/login`,
`/api/health/**`, `/api/public/**` y `/error`; **todo el resto exige token**.

**Esto afecta a los endpoints nuevos de cualquiera:** un endpoint que se agregue sin
protección queda cerrado en lugar de abierto. Para exigir un permiso concreto:

```java
@PreAuthorize("hasPermission('COMERCIAL','ANULAR')")
```

Los módulos válidos son `SEGURIDAD`, `COMERCIAL`, `INVENTARIO`, `CONTABILIDAD`,
`FACTURACION` y `REPORTES`, y las acciones `CREAR`, `CONSULTAR`, `MODIFICAR` y `ANULAR`.
Si el código no existe en el catálogo, **el acceso se deniega** y queda un aviso en el log:
es deliberado, para que un typo no abra una puerta.

### Migraciones con versión fechada

La migración se llama `V20260918_1__seguridad_rbac.sql` en lugar de `V2__`. **Conviene que
todos hagamos lo mismo**: si dos integrantes suben una `V2__` distinta, Flyway aborta el
arranque por versión repetida.

### CORS

Se agregó CORS para `http://localhost:4200` y `http://localhost:4000`, configurable con
`erp.cors.origenes`. Sin eso el Angular de desarrollo no puede consumir la API.

### El catálogo y la matriz viven en Java, no en la semilla SQL

`CatalogoPermisos` (los 24 permisos) y `MatrizRolesDeSistema` (los 7 roles) son definiciones
inmutables en código. `ArranqueSeguridad` las aplica en cada arranque, de forma idempotente.

La razón: los roles son **por empresa** y las empresas se crean en tiempo de ejecución, así
que sembrarlos en SQL solo habría servido para la empresa demo — una empresa nueva nacería
sin ningún rol. Con esto, **cualquier empresa que cree la CU-02 nace con sus 7 roles**, sin
que nadie corra un script.

Si hay que cambiar la matriz, se cambia en `MatrizRolesDeSistema` y **se actualiza el
documento**: hay pruebas que verifican los totales exactos de la tabla entregada y se van a
caer si una y otro dejan de coincidir.

## 4. Datos de demostración

Están sembrados desde Java y **solo si la base no tiene ninguna empresa**:

| Usuario | Contraseña | Rol |
|---|---|---|
| admin@demo.bo | `Admin123*` | Administrador |
| cajero@demo.bo | `Cajero123*` | Cajero |
| auditor@demo.bo | `Auditor123*` | Auditor Interno |

**En producción hay que poner `erp.seguridad.demo.habilitado=false`.** Y el secreto del JWT
sale de la variable de entorno `ERP_JWT_SECRETO`; el valor por defecto es solo para
desarrollo local.

## 5. Una corrección al documento del sprint

Las reglas de negocio de la HU-03 ponen como ejemplo el código `VENTA_ANULAR`, pero "VENTA"
no es ninguno de los seis módulos de la matriz (ahí figuran *Comercial y Ventas* y
*Facturación*). En el código quedó como **`COMERCIAL_ANULAR`**. Hay que corregir esa línea
del documento y regenerar la Figura 3 (el diagrama de secuencia), que también dice
`VENTA_ANULAR`.

## 6. Pendiente de conversación, no del código

El Sprint Backlog del grupo es por tarea y no por historia: Santiago figura solo en las
filas 05 (2 h) y 08 (2,5 h), o sea **4,5 h**, contra las **24 h** que la HU-03 estima para
sus 7 tareas. Esa diferencia sigue sin resolverse.
