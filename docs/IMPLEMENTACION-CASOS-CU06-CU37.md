# Implementación de CU06 y CU37, y limpieza del modelo de seguridad

Rama: `dev-javier`. Incluye dos casos de uso nuevos y cambios en código de otros CU que hacían
falta para que el backend arranque en local y para que los permisos sean correctos.

| Cambio | Toca código de |
|---|---|
| CU06 Personalizar Apariencia | CU06, `main-layout`, `auth.service` (CU01), `theme.service` |
| Retiro del modelo RBAC viejo y nuevas migraciones | **CU01, CU02, CU03**, migraciones Flyway |
| Corrección de permisos del Almacenero | **CU03** (`ServicioAutorizacion`) |
| CU37 Gestionar Reportes Predefinidos | CU37, `main-layout` |

## Modelo de datos: una base por empresa

El esquema real es el de `backend/Base de Datos/ERP_Asus.txt`: cada empresa (tenant) tiene su
propia base y `ERP_Master` guarda su `DB_NAME`. Por eso los CU nuevos **no filtran por
`empresa_id`**: el aislamiento lo da la base de datos.

Hoy el backend usa un solo `DB_URL` (no hay ruteo por empresa) y el onboarding no crea la base de
la empresa nueva. Queda pendiente para el grupo.

---

## CU06 - Personalizar Apariencia

El administrador de la empresa define cómo ven el sistema todos sus usuarios.

**Qué se personaliza**

- Logo (PNG, JPG, WEBP o SVG, hasta 512 KB, guardado en la base).
- Paleta: color primario, secundario y de acento (con 6 paletas predefinidas).
- Tema claro u oscuro, **impuesto por la empresa** (se quitó el botón de tema del panel).
- Tamaño de fuente, tipografía (Inter, Roboto, Poppins, Open Sans) y bordes.
- Encabezado y pie de factura (solo se guardan; usarlos al imprimir es de CU19 y CU15).

**Quién puede cambiarla:** quien tenga `SEGURIDAD_MODIFICAR` (en la matriz de la HU-03, solo el
Administrador). Cualquier usuario autenticado la ve aplicada.

### Backend — `modulo/seguridad_y_auditoria/cu06_personalizar_apariencia`

| Capa | Clase |
|---|---|
| entity | `ConfiguracionApariencia` (Lombok + Jakarta, tabla `configuracion_empresa`) |
| repository | `ConfiguracionAparienciaRepositorio` (`findFirstByOrderByIdAsc`: tabla de una fila) |
| dto | `AparienciaDto`, `Peticiones.Actualizar` (Bean Validation), `LogoArchivo` |
| mapper | `AparienciaMapper` (completa con la marca NexoERP las columnas en `NULL`) |
| service | `ServicioApariencia` |
| controller | `ControladorApariencia` |

| Endpoint | Permiso |
|---|---|
| `GET /api/seguridad/apariencia` | autenticado |
| `GET /api/seguridad/apariencia/logo` | autenticado (el front lo pide con token y lo muestra como blob) |
| `PUT /api/seguridad/apariencia` | `SEGURIDAD_MODIFICAR` |
| `POST /api/seguridad/apariencia/logo` (multipart, campo `archivo`) | `SEGURIDAD_MODIFICAR` |
| `DELETE /api/seguridad/apariencia/logo` | `SEGURIDAD_MODIFICAR` |
| `POST /api/seguridad/apariencia/restablecer` | `SEGURIDAD_MODIFICAR` |

- El tipo del logo se detecta por su contenido (bytes iniciales), no por el nombre ni el
  `Content-Type`. Se rechazan los SVG con scripts, `on*=`, `foreignObject` o entidades.
- Cada cambio queda en `bitacora_logs` (`APARIENCIA_ACTUALIZADA`, `APARIENCIA_LOGO_SUBIDO`,
  `APARIENCIA_LOGO_ELIMINADO`, `APARIENCIA_RESTABLECIDA`).
- `application.properties`: `spring.servlet.multipart.max-file-size=512KB`.

**Base de datos:** no hay tabla nueva; se agregan columnas a `CONFIGURACION_EMPRESA`
(`color_acento`, `tamano_fuente`, `tipografia`, `bordes`, `logo`, `logo_tipo`, `logo_version`,
`modificado_por`).

- Bases existentes (RDS): ejecutar a mano `backend/Base de Datos/CU06_apariencia.sql`
  (idempotente).
- Bases nuevas: ya vienen en `ERP_Asus.txt` y en la migración `V1`.

### Frontend — `modulos/seguridad-y-auditoria/cu06-personalizar-apariencia`

- `services/apariencia.service.ts`: carga la apariencia al entrar al panel y la aplica con
  variables CSS (`--primary*`, `--secondary*`, `--accent`, `--font-sans`). Solo sobrescribe lo
  que difiere de NexoERP. Guarda una copia en `localStorage` para no parpadear y **la borra al
  cerrar sesión** (`AuthService.clearSession`), para que otra empresa en el mismo navegador no la
  herede.
- `components/personalizar-apariencia.component`: pantalla con vista previa en vivo; solo lectura
  sin permiso.
- `theme.service.ts`: `aplicarSinGuardar()` y `restaurarPreferencia()`, para que el tema de la
  empresa no pise la preferencia de landing y login.

**Limitación:** quedan unos 117 azules escritos a mano en landing, login, catálogo (CU10) y
notificaciones; esos no siguen el color de la empresa.

---

## Retiro del modelo RBAC viejo

Había **dos entidades sobre la misma tabla `rol`**: `compartido/entity/Rol` (id `Integer`, la que
usa el login) y `cu03.../entity/Rol` (id `UUID`, de la migración vieja `V20260918_1`). Con una
base nueva el backend no arrancaba.

Se dejó un solo modelo, el de `ERP_Asus` (`compartido`), que es el que usa el código vivo.

**Eliminado del CU03:**

- entity: `Rol`, `Permiso`, `UsuarioRol`, `UsuarioRolId`, `Usuario` (tabla `usuarios`),
  `Empresa`, `BitacoraEvento`
- repository: `RolRepositorio`, `PermisoRepositorio`, `UsuarioRolRepositorio`,
  `UsuarioRepositorio`, `EmpresaRepositorio`, `BitacoraRepositorio`
- mapper: `RolMapper`, `PermisoMapper`, `UsuarioResumenMapper`, `AsignacionMapper`
- service: `ServicioAprovisionamiento`, `ArranqueSeguridad`
- test: `ServicioAprovisionamientoTest`

**Ajustado:** `UsuarioService` y `UsuarioMapper` (CU02) y `ServicioAutorizacion` (CU03) tenían
referencias sin uso al modelo viejo. Se conservan `compartido` y el `catalogo` del CU03
(`CatalogoPermisos`, `MatrizRolesDeSistema`).

`CatalogoPermisosTest` se movió a `cu03_gestionar_roles_y_permisos/catalogo`, que es el paquete
real de esas clases.

### Migraciones Flyway

Las 8 migraciones viejas creaban el esquema anterior (`usuarios`, `roles`, `rol` con UUID) y ya
no coincidían con el código. Se reemplazaron por:

| Migración | Contenido |
|---|---|
| `V1__esquema_erp_asus.sql` | DDL e índices de `ERP_Asus.txt`, tal cual |
| `V2__datos_iniciales_erp_asus.sql` | Datos de ejemplo de ASUS Bolivia del mismo script |
| `V3__compatibilidad_codigo.sql` | Lo que el código espera y el script no tiene (ver abajo) |
| `V20261021_1__seguridad_apariencia.sql` | Columnas del CU06 (sin efecto sobre `V1`, ya las trae) |
| `V20261021_2__compatibilidad_ventas_clientes.sql` | Default de `clientes.fecha_creacion` y columnas de anulación de `ventas` |

`V3` se obtuvo comparando las entidades JPA con una base creada solo con `ERP_Asus.txt`:

- `usuario.fullname`, `usuario.username`
- tablas `sesion` (CU01), `bitacora_logs` (CU02/CU04/CU06/CU09), `almacenes` (CU09)
- `empresa_id` en `categorias` y `unidades_medida`
- `ventas.tipo_comprobante` y `ventas.motivo_anulacion`

`V3` y `V20261021_2` son idempotentes: si a la base de RDS le falta algo, se pueden ejecutar a
mano.

**Ojo:** una base que ya tenga aplicadas las migraciones viejas no se transforma sola (Flyway da
`V1`–`V3` por aplicadas por número). Lo limpio es empezar con una base vacía.

---

## Corrección de permisos del Almacenero (CU03)

En `ServicioAutorizacion.mapearFuncionAPermisos`, la función del Almacenero **"Registrar Compra y
Devolucion de Proveedores"** caía en la rama de ventas por la palabra "devolucion" y le daba
`FACTURACION_CONSULTAR`, `FACTURACION_CREAR` y `FACTURACION_ANULAR` en todo el sistema.

Se movió la rama de compras/proveedores antes que la de ventas. Comprobado con el usuario real
`almacen@asustech.bo`: ahora recibe 403 en los reportes de ventas.

---

## CU37 - Gestionar Reportes Predefinidos

| Categoría | Permiso | Reportes |
|---|---|---|
| Ventas | `FACTURACION_CONSULTAR` (Administrador, Vendedor, Cajero) | Ventas por período · Ventas por cajero · Productos más vendidos (top N) |
| Inventario | `INVENTARIO_CONSULTAR` (todos los roles) | Existencias por sucursal · Productos bajo stock mínimo · Valorización del inventario |

Ventas usa `FACTURACION_CONSULTAR` y no `COMERCIAL_CONSULTAR` porque este último
`ServicioAutorizacion` se lo da a todos los roles como acceso operativo.

- Las ventas `ANULADA` no cuentan. Sin fechas, se toma el mes en curso; rango máximo 366 días.
- Cada reporte trae fila de totales y se exporta a **CSV** (formato del Kardex del CU12: `;`,
  coma decimal, BOM, protección contra fórmulas de Excel) y a **PDF** con la impresión del
  navegador, en un documento propio con el logo y colores del CU06.

### Backend — `modulo/reportes_y_bi/cu37_reportes_predefinidos`

| Capa | Clase |
|---|---|
| catalogo | `ReportePredefinido` (los 6 reportes: filtros, columnas, categoría), `CategoriaReporte`, `FiltroReporte`, `Columna` |
| repository | `ReportesRepositorio` (SQL agregado con `NamedParameterJdbcTemplate`) |
| dto | `ReporteDefinicionDto`, `ReporteResultadoDto`, `ColumnaDto`, `FiltrosReporte`, `OpcionesReporteDto` |
| service | `ServicioReportes` (permisos con `EvaluadorDePermisos`, validación, totales, CSV) |
| controller | `ControladorReportes` |

| Endpoint | Qué devuelve |
|---|---|
| `GET /api/reportes/predefinidos` | Reportes que el usuario puede ver |
| `GET /api/reportes/predefinidos/opciones` | Sucursales y categorías para los filtros |
| `GET /api/reportes/predefinidos/{codigo}?desde&hasta&sucursalId&categoriaId&limite` | Resultado (JSON) |
| `GET /api/reportes/predefinidos/{codigo}/csv?...` | Descarga CSV |

Errores: 403 sin permiso de la categoría, 404 código inexistente, 400 fechas invertidas, rango
mayor a un año o top fuera de 1–100.

### Frontend — `modulos/reportes-y-bi/cu37-reportes-predefinidos`

Se renombró la carpeta vieja `cu37-estados-financieros-basicos` (y los README) porque tenía el
nombre de la numeración anterior.

- `services/reportes.service.ts`: catálogo, generación, descarga del CSV con token e impresión
  en un iframe oculto.
- `utils/formato.ts`: formato de valores y documento imprimible (escapa el HTML de los datos).
- `components/reportes-predefinidos.component`: catálogo agrupado, filtros del reporte elegido,
  tabla con totales y botones de exportación.

En desarrollo (front en `:4200`, back en `:8080`) CORS no expone `Content-Disposition`; el
nombre del CSV se arma en el cliente con el mismo formato que el servidor.

---

## Cómo levantarlo en local

1. PostgreSQL local en el puerto 5433 con una base **vacía** `erp_db` (la de `backend/.env.local`).
2. Backend, desde `backend/`:

   ```powershell
   .\gradlew clean bootRun --args="--spring.profiles.active=local --spring.flyway.enabled=true"
   ```

   Flyway aplica `V1`, `V2`, `V3`, `V20261021_1` y `V20261021_2`, y el seeder crea
   `admin@erp.com`. Si en `.env.local` está activo `FLYWAY_ENABLED=true`, basta con
   `--spring.profiles.active=local`.
3. Frontend, desde `frontend/`: `npm start`.

La URL por defecto de `application.properties` (RDS
`erp-database.coxoq4mwa6sg.us-east-1.rds.amazonaws.com`) ya no resuelve en DNS: sin el perfil
`local` el backend no arranca.

---

## Pruebas

| Qué | Resultado |
|---|---|
| `ServicioAparienciaTest` (CU06) | 13/13 |
| `ServicioReportesTest` (CU37) | 12/12 |
| `CatalogoPermisosTest`, `EvaluadorDePermisosTest` | 9/9 y 9/9 |
| Frontend: tests nuevos de CU06 y CU37 | 16/16 y 15/15 |
| `ng build` (SSR + prerender) | OK |
| Endpoints de CU06 y CU37 con el backend corriendo sobre `erp_db` | OK, incluidos 400/401/403/404/413 |
| CU01, CU02, CU03 y CU09 tras el retiro del modelo viejo | responden 200 |

Siguen fallando, **igual que antes de estos cambios**:

- Frontend: 26 tests de `auth.service`, `usuarios`, `pedidos-venta` y `main-layout`.
- Backend: `ValidacionComercialIntegrationTest` (13), porque arranca con la URL de RDS caída.

`build.gradle` excluye `src/test/java/modulo/seguridad_y_auditoria/**`, así que con
`.\gradlew test` normal **no corren** los tests de CU06 ni de CU03. Hay que acotar esa exclusión
a las carpetas viejas (`roles_y_permisos/**`, `acceso_al_sistema/**`, `gestion_de_usuarios/**`).

## Pendientes para el grupo

- Ruteo de una base por empresa y creación de la base en el onboarding.
- Acotar la exclusión de tests en `build.gradle`.
- `admin@erp.com` y `admin@asustech.bo` quedan con el mismo `username` (`admin`), y al entrar con
  el primero `mis-permisos` devuelve el usuario 1. Viene del `InitializerSeeder` del CU01.
- Usar el encabezado, pie y logo del CU06 al imprimir facturas (CU19) y proformas (CU15).
- Exponer `Content-Disposition` en `CorsConfig` si se quiere el nombre del servidor en desarrollo.
