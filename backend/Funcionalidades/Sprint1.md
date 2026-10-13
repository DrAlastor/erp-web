## HU-01 — Iniciar Sesión en el Sistema (Seguridad)

**Responsable:** Nicolas

**Módulo Backend:** `com.erp.backend.modulo_acceso`

**Módulo Frontend:** `modulo_acceso`

**Prioridad:** Alta

### Descripción Funcional

Permite a cualquier usuario registrado y activo autenticarse en el ERP mediante la entrega de credenciales válidas. Tras un inicio de sesión exitoso, el sistema emite un token JWT que encapsula la identidad y los permisos asignados. Si se detectan múltiples intentos fallidos consecutivos, el sistema aplica un bloqueo temporal sobre la cuenta.

### Flujo Principal

1. **Autenticación Base:**
* El usuario envía `username` (o `email`) y `password` a `/api/auth/login`.


* El backend consulta la tabla `usuarios`. Si no existe o `enable = FALSE`, retorna error `401 Unauthorized`.


* Se evalúa la columna `bloqueado_hasta`. Si la fecha actual es menor, retorna un error de bloqueo de cuenta (`ACCOUNT_LOCKED`).




2. **Validación de Contraseña:**
* Se valida la contraseña con `PasswordEncoder` (`BCrypt`).


* **Contraseña Incorrecta:** Incrementa `intentos_fallidos` en +1. Si `intentos_fallidos >= 3`, establece `bloqueado_hasta = NOW() + 15 MINUTOS`. Retorna `401 Unauthorized`.


* **Contraseña Correcta:** Reinicia `intentos_fallidos = 0` y `bloqueado_hasta = NULL`.


3. **Generación de Tokens y Sesión:**
* Genera el Access Token JWT (vida útil parametrizada en `JWT_EXPIRATION`) conteniendo el `username` y las *authorities* asociadas.


* Genera un `refresh_token` aleatorio de alta entropía.
* Inserta una fila en la tabla `sesion` guardando `usuario_id`, el hash del token (`refresh_token_hash`), `ip_origen`, `user_agent` y la `fecha_expiracion` de la sesión.
* Retorna al cliente el par de tokens junto con los datos de perfil básico (`fullname`, `email`).


4. **Refresco de Token:**
* El cliente envía el `refresh_token` a `/api/auth/refresh`.


* El sistema busca en `sesion` por el hash del token, valida que `fecha_cierre` sea NULO y `fecha_expiracion > NOW()`.
* Emite un nuevo Access Token JWT.




5. **Cierre de Sesión (Logout):**
* El cliente envía la petición a `/api/auth/logout`.
* El sistema busca la sesión activa en `sesion` y asigna `fecha_cierre = NOW()`.



### Tablas Implicadas

| Tabla | Operación | Motivo |
| --- | --- | --- |
| `usuarios` | Lectura y Escritura | Consulta credenciales, verifica `enable`, gestiona `intentos_fallidos` y `bloqueado_hasta`. |
| `sesion` | CRUD | Guarda, valida y liquida sesiones activas con tokens. |
| `usuario_roles` | Lectura | Consulta de roles para la generación del token JWT. |
| `rol_permisos` | Lectura | Obtención de permisos granulares convertidos a *authorities* de Spring Security.

 |

### Dependencias y Conexiones

* **Dependencias Backend:** Clase `JwtService`, `JwtAuthenticationFilter` y repositorios de `modulo_acceso`.


* **Dependencias Frontend:** `AuthService` y `auth.interceptor.ts` para adjuntar el encabezado `Authorization: Bearer <token>` a cada solicitud HTTP.

---

## HU-02 — Gestionar Usuarios (Seguridad)

**Responsable:** Alessandro

**Módulo Backend:** `com.erp.backend.modulo_acceso`

**Módulo Frontend:** `modulo_acceso`

**Prioridad:** Alta

### Descripción Funcional

Proporciona el control completo sobre las cuentas de usuario que operan el ERP, permitiendo su creación, consulta paginada, actualización de datos personales, asignación de roles y habilitación o deshabilitación lógica.

### Flujo Principal

1. **Listar Usuarios:**
* Endpoint `GET /api/usuarios` (Requiere `ACCESO:USUARIOS:LECTURA`).


* Soporta paginación, ordenamiento y búsqueda por los campos `username`, `fullname` o `email`.


* Retorna una colección de DTOs `UsuarioResponse` proyectando los roles asociados sin exponer el hash de la contraseña.




2. **Crear Usuario:**
* Endpoint `POST /api/usuarios` (Requiere `ACCESO:USUARIOS:ESCRITURA`).


* Valida unicidad de `username` y `email` en la tabla `usuarios`. Si ya existen, lanza una excepción de negocio.


* Transforma el DTO `UsuarioRequest` a la entidad `Usuario`, codifica la contraseña mediante `PasswordEncoder` y asigna `enable = TRUE` por defecto.


* Asigna los roles especificados por ID en la tabla `usuario_roles`.


* Guarda el registro completando de manera automática la auditoría (`created_at = NOW()`).




3. **Editar Usuario:**
* Endpoint `PUT /api/usuarios/{id}` (Requiere `ACCESO:USUARIOS:ESCRITURA`).
* Permite actualizar `fullname`, `email` y la lista de roles asignados.
* Si la solicitud incluye un cambio de contraseña, esta es encriptada antes de guardarse. Actualiza `updated_at`.




4. **Activar / Desactivar Usuario (Baja Lógica):**
* Endpoint `PATCH /api/usuarios/{id}/status` (Requiere `ACCESO:USUARIOS:ESCRITURA`).
* Invierte o cambia el valor del campo booleano `enable`. Si pasa a `FALSE`, el usuario pierde la capacidad de autenticarse o refrescar sesión inmediatamente.





### Tablas Implicadas

| Tabla | Operación | Motivo |
| --- | --- | --- |
| `usuarios` | CRUD | Gestión principal del catálogo de cuentas de usuario.

 |
| `roles` | Lectura | Consulta de existencia de los roles asignados.

 |
| `usuario_roles` | Inserción y Borrado | Mapeo de la relación muchos a muchos entre usuarios y sus roles.

 |

### Dependencias y Conexiones

* **Dependencias Backend:** Invocado por `UsuarioController` y gestionado por `UsuarioService`.


* **Dependencias Frontend:** Pantalla `usuarios.component.ts` dentro del camino de ruta `/acceso/usuarios` protegida por `permissionGuard`.



---

## HU-03 — Gestionar Roles y Permisos (Seguridad)

**Responsable:** Santi

**Módulo Backend:** `com.erp.backend.modulo_acceso`

**Módulo Frontend:** `modulo_acceso`

**Prioridad:** Alta

### Descripción Funcional

Permite parametrizar el esquema de Control de Acceso Basado en Roles (RBAC). Define los perfiles operacionales del ERP (por ejemplo, "ADMINISTRADOR", "CAJERO", "ALMACENERO") y les asigna permisos de forma granular cruzando las variables de `modulo`, `pantalla` y `accion`.

### Flujo Principal

1. **Listar y Consultar Roles:**
* Endpoint `GET /api/roles` (Requiere `ACCESO:ROLES:LECTURA`).
* Obtiene la lista de roles definidos en la tabla `roles` junto con la cantidad de permisos vinculados.


2. **Crear o Modificar Rol:**
* Endpoint `POST /api/roles` y `PUT /api/roles/{id}` (Requiere `ACCESO:ROLES:ESCRITURA`).
* Registra o actualiza los campos `nombre` y `descripcion` en la tabla `roles`.
* El nombre del rol se valida para evitar duplicados en la base de datos.


3. **Asignación Granular de Permisos:**
* Endpoint `PUT /api/roles/{id}/permisos` (Requiere `ACCESO:ROLES:ESCRITURA`).
* Recibe un arreglo de IDs de permisos desde el frontend.
* Elimina la lista previa de autorizaciones en `rol_permisos` para el `rol_id` indicado e inserta los nuevos pares `(rol_id, permiso_id)`.
* Los permisos asignados concatenan la estructura `'MODULO:PANTALLA:ACCION'` (ejemplo: `'INVENTARIO:STOCK:ESCRITURA'`), la cual Spring Security lee para otorgar las autorizaciones runtime (`hasAuthority`).


4. **Listar Catálogo Master de Permisos:**
* Endpoint `GET /api/permisos` (Requiere `ACCESO:ROLES:LECTURA`).
* Retorna la matriz completa de la tabla `permisos` agrupada por `modulo` y `pantalla` para facilitar su renderización en checkboxes dentro de la interfaz gráfica.



### Tablas Implicadas

| Tabla | Operación | Motivo |
| --- | --- | --- |
| `roles` | CRUD | Entidad base de los perfiles de acceso del sistema.

 |
| `permisos` | Lectura | Catálogo con la combinación única de `modulo`, `pantalla` y `accion`.

 |
| `rol_permisos` | Inserción y Borrado | Mapeo asociativo de permisos concedidos a cada rol.

 |

### Dependencias y Conexiones

* **Dependencias Backend:** `RolPermisoController` y `RolPermisoService`. Al alterar un rol, las autorizaciones de las sesiones activas asociadas se refrescan en su siguiente petición REST.


* **Dependencias Frontend:** Vista `roles.component.ts` configurada en la ruta `/acceso/roles`.



---

## HU-04 — Gestionar Clientes (Comercial)

**Responsable:** Javier

**Módulo Backend:** `com.erp.backend.modulo_comercial`

**Módulo Frontend:** `modulo_comercial`

**Prioridad:** Media

### Descripción Funcional

Administra la información comercial de los clientes o empresas compradoras. Mantiene el padrón de contribuyentes validando registros como la Razón Social y el NIT/CI para su empleo posterior en la emisión de comprobantes o pedidos de venta.

### Flujo Principal

1. **Listar / Buscar Clientes:**
* Endpoint `GET /api/clientes` (Requiere `COMERCIALL:CLIENTES:LECTURA`).
* Permite filtrar por `razon_social`, `nit_ci` o estado de habilitación (`activo`).
* Retorna una lista estructurada mediante `ClienteResponse`.




2. **Registrar Cliente:**
* Endpoint `POST /api/clientes` (Requiere `COMERCIALL:CLIENTES:ESCRITURA`).
* Recibe `ClienteRequest` conteniendo `razon_social`, `nit_ci`, `telefono` y `direccion`.


* Verifica mediante `ClienteRepository` que el número de `nit_ci` no se encuentre registrado previamente.
* Asigna `activo = TRUE`, establece `fecha_creacion = NOW()` e inserta en la tabla `clientes`.


3. **Actualizar Datos Comerciales:**
* Endpoint `PUT /api/clientes/{id}` (Requiere `COMERCIALL:CLIENTES:ESCRITURA`).
* Modifica valores de contacto, dirección fiscal o Razón Social del cliente seleccionado.


4. **Inactivar Cliente:**
* Endpoint `PATCH /api/clientes/{id}/status` (Requiere `COMERCIALL:CLIENTES:ESCRITURA`).
* Cambia la columna `activo` a `FALSE` impidiendo que el cliente sea seleccionado en nuevos flujos comerciales, preservando el historial de datos para auditoría.



### Tablas Implicadas

| Tabla | Operación | Motivo |
| --- | --- | --- |
| `clientes` | CRUD | Almacenamiento de datos fiscales y de contacto de los clientes de la empresa. |

### Dependencias y Conexiones

* **Dependencias Backend:** `ClienteController`, `ClienteService` y `ClienteRepository` dentro del paquete `modulo_comercial`.


* **Dependencias Frontend:** Interfaz gráfica `clientes.component.ts` localizada en el menú de navegación `/comercial/clientes`.



---

## HU-05 — Gestionar Catálogo de Artículos (Inventario)

**Responsable:** Javier

**Módulo Backend:** `com.erp.backend.modulo_inventario`

**Módulo Frontend:** `modulo_inventario`

**Prioridad:** Alta

### Descripción Funcional

Mantiene el registro unificado del catálogo de mercancías y productos. Codifica parámetros fundamentales como código SKU, código de barras, unidades de medida, precios de venta base y umbrales de stock mínimo permitidos.

### Flujo Principal

1. **Consulta del Catálogo:**
* Endpoint `GET /api/productos` (Requiere `INVENTARIO:PRODUCTOS:LECTURA`).


* Permite realizar búsquedas exactas por `codigo_sku`, `codigo_barra` o búsquedas parciales por `nombre` de producto o filtro por `categoria_id`.




2. **Crear Producto:**
* Endpoint `POST /api/productos` (Requiere `INVENTARIO:PRODUCTOS:ESCRITURA`).
* Valida que no existan duplicados para los valores de `codigo_sku` ni `codigo_barra` en la tabla `productos`.
* Verifica la existencia de la categoría elegida mediante la tabla `categorias`.
* Mapea los valores ingresados (`precio_venta`, `costo_promedio`, `stock_minimo`, `unidad_medida`), establece `activo = TRUE` y guarda la entidad registrando `fecha_creacion = NOW()`.


3. **Editar Producto:**
* Endpoint `PUT /api/productos/{id}` (Requiere `INVENTARIO:PRODUCTOS:ESCRITURA`).
* Permite actualizar la categorización, descripciones, precios o límites mínimos de inventario.


4. **Gestión de Categorías:**
* Endpoints CRUD en `/api/categorias` para crear y listar las familias o categorías que agrupan los artículos comerciables.



### Tablas Implicadas

| Tabla | Operación | Motivo |
| --- | --- | --- |
| `productos` | CRUD | Maestro donde se definen los artículos, SKUs, precios y stock mínimo.

 |
| `categorias` | Lectura y Escritura | Clasificación jerárquica de los productos.

 |

### Dependencias y Conexiones

* **Dependencias Backend:** Componentes `ProductoController`, `ProductoService` y `ProductoRepository` en `modulo_inventario`.


* **Dependencias Frontend:** Pantallas standalone `productos.component.ts` y `categorias.component.ts` en las rutas `/inventario/productos` y `/inventario/categorias`.



---

## HU-06 — Gestionar Existencias de Artículos (Inventario)

**Responsable:** Leonardo

**Módulo Backend:** `com.erp.backend.modulo_inventario`

**Módulo Frontend:** `modulo_inventario`

**Prioridad:** Alta

### Descripción Funcional

Controla la disponibilidad física y la distribución del stock de los productos a través de las diferentes ubicaciones o almacenes definidos en la empresa. Permite consultar saldos en tiempo real y efectuar ajustes de entrada o salida sobre el inventario.

### Flujo Principal

1. **Consulta de Stock por Almacén:**
* Endpoint `GET /api/stock` (Requiere `INVENTARIO:STOCK:LECTURA`).


* Retorna la disponibilidad cruzada filtrada por `almacen_id` o `producto_id` desde la tabla `stock_almacen`.


* Compara `cantidad_actual` contra el `stock_minimo` del producto para marcar alertas de reabastecimiento en la respuesta DTO.


2. **Ajuste de Stock (Entradas y Salidas):**
* Endpoint `POST /api/stock/ajuste` (Requiere `INVENTARIO:STOCK:ESCRITURA`).
* Recibe la transacción con `producto_id`, `almacen_id`, `cantidad` y `tipo_operacion` (`INCREMENTO` / `DECREMENTO`).
* Si la relación `(producto_id, almacen_id)` no existe en `stock_almacen`, la crea con un valor inicial de `0.00`.
* En caso de un decremento, verifica que `cantidad_actual - cantidad_solicitada >= 0`. Si el saldo resultara negativo, invalida la operación con un error de negocio.
* Ejecuta el cálculo sobre `cantidad_actual` de forma transaccional (`@Transactional`).


3. **Gestión de Almacenes:**
* Endpoint `GET /api/almacenes` (Requiere `INVENTARIO:STOCK:LECTURA`) para listar las infraestructuras de depósito configuradas (`es_principal`, `direccion`).



### Tablas Implicadas

| Tabla | Operación | Motivo |
| --- | --- | --- |
| `stock_almacen` | CRUD | Guarda la relación física de inventario (`cantidad_actual`) por almacén y producto. |
| `productos` | Lectura | Consulta de referencias, datos del artículo y umbral de `stock_minimo`.

 |
| `almacenes` | Lectura | Validación y consulta de los centros de almacenamiento registrados. |

### Dependencias y Conexiones

* **Dependencias Backend:** Ejecutado por `StockController` y procesado por `StockService` dentro de `modulo_inventario`.


* **Dependencias Frontend:** Vista `stock.component.ts` accesible mediante la ruta `/inventario/stock`. Se conecta directamente con el catálogo maestro expuesto en la HU-05.