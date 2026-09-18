# Arquitectura del Backend ERP (Fase 1)

## Ubicación de la infraestructura común y de los casos de uso

La infraestructura transversal vive en `src/main/java/comun`: `BackendApplication`, `config` (CORS, seguridad HTTP y seguridad de métodos), `controller/HealthController` y `exception` (respuesta de error, recurso no encontrado y manejador global). Spring detecta componentes en `comun` y `modulo`.

Cada caso de uso vive en su propia carpeta dentro de `modulo/<modulo_de_negocio>/<caso_de_uso>` y empaqueta las mismas capas. Estado verificado de los casos de uso implementados:

```text
modulo/
├── seguridad_y_auditoria/
│   ├── compartido/                  <-- entidades y repositorios compartidos entre casos de uso
│   ├── acceso_al_sistema/           <-- CU01: inicio de sesión, JWT y sesiones
│   ├── gestion_de_usuarios/         <-- CU02: administración de cuentas
│   └── roles_y_permisos/            <-- CU03: roles, permisos y autorización
└── inventario_y_almacenes/
    └── movimientos_de_inventario/   <-- CU06: Kardex y existencias por almacén
```

El resto de las carpetas de `modulo` (`caja_y_arqueo`, `comercial_y_preventa`, `contabilidad_e_impuestos`, `inventario_y_existencia` y los demás casos de uso de `inventario_y_almacenes`) están creadas y vacías: son el lugar previsto para los casos de uso que faltan implementar. Al implementarlos deben seguir exactamente las capas y las reglas de la sección 6.

Las entidades y repositorios que comparten varios casos de uso —`Usuario`, `Rol`, `Permiso`— viven en `seguridad_y_auditoria/compartido`, porque los usan el login (CU01), la administración de cuentas (CU02) y el RBAC (CU03) sobre la misma tabla. El resto de las entidades pertenece al caso de uso que las administra.

El backend está construido bajo el patrón de **monolito modular** en Spring Boot. Cada caso de uso empaqueta sus propias capas del patrón MVC/Domain (`controller`, `dto`, `entity`, `mapper`, `repository`, `service`) y comparte únicamente la infraestructura transversal.

## 1. Configuración de Stack e Infraestructura

**Tecnologías:**

* Java 21 / Spring Boot 3.x
* PostgreSQL 16 (Base de datos relacional)
* Spring Data JPA / Hibernate
* Spring Security + JWT (`jjwt`)
* Spring Boot Starter Validation
* Lombok

**Variables de Entorno (`.env` / `application.properties`):**

```properties
DB_URL=jdbc:postgresql://localhost:5432/erp_db
DB_USERNAME=postgres
DB_PASSWORD=root
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_EXPIRATION=86400000
SERVER_PORT=8080
TIME_ZONE=America/La_Paz

```

---

## 2. Estructura Completa de Paquetes por Caso de Uso

Todas las capas son obligatorias en cada caso de uso: `controller`, `dto`, `entity`, `mapper`, `repository` y `service`. Un caso de uso puede agregar un paquete propio cuando aporta algo que no es una capa —`catalogo` en el RBAC, `security` cuando expone su propio puente de autorización—, pero nunca puede faltar una de las seis.

```text
src/main/java/
├── comun/                                     <-- infraestructura transversal
│   ├── BackendApplication.java
│   ├── config/
│   │   ├── SecurityConfig.java                (filtro JWT, sesiones stateless, CORS)
│   │   ├── CorsConfig.java
│   │   ├── ConfiguracionMetodosSeguros.java   (habilita @PreAuthorize)
│   │   └── EvaluadorDePermisos.java           (hasPermission contra el RBAC de CU03)
│   ├── controller/
│   │   └── HealthController.java
│   └── exception/
│       ├── ErrorResponse.java
│       ├── ResourceNotFoundException.java
│       └── GlobalExceptionHandler.java
│
└── modulo/
    ├── seguridad_y_auditoria/
    │   ├── compartido/
    │   │   ├── entity/        (Usuario, Rol, Permiso)
    │   │   └── repository/    (UsuarioRepository, RolRepository)
    │   │
    │   ├── acceso_al_sistema/                 <-- CU01 (Nicolas)
    │   │   ├── controller/    (AuthController)
    │   │   ├── dto/auth/      (LoginRequest, RefreshTokenRequest, TokenResponse, UsuarioPerfilResponse)
    │   │   ├── entity/        (Sesion)
    │   │   ├── exception/     (AccountLockedException, InvalidCredentialsException)
    │   │   ├── mapper/        (AuthMapper)
    │   │   ├── repository/    (SesionRepository)
    │   │   ├── security/      (JwtService, JwtAuthenticationFilter, ApplicationUserPrincipal)
    │   │   ├── seed/          (InitializerSeeder)
    │   │   └── service/       (AuthService, SesionService)
    │   │
    │   ├── gestion_de_usuarios/               <-- CU02 (Alessandro)
    │   │   ├── controller/    (UsuarioController)
    │   │   ├── dto/           (UsuarioRequest, UsuarioUpdateRequest, UsuarioStatusRequest, UsuarioResponse)
    │   │   ├── entity/        (BitacoraLog)
    │   │   ├── mapper/        (UsuarioMapper)
    │   │   ├── repository/    (UsuarioGestionRepository, BitacoraLogRepository, UsuarioSpecifications)
    │   │   └── service/       (UsuarioService, UsuarioAuditoriaService)
    │   │
    │   └── roles_y_permisos/                  <-- CU03 (Santiago)
    │       ├── catalogo/      (CatalogoPermisos, MatrizRolesDeSistema, ModuloErp, AccionErp, RolDeSistema, DefinicionPermiso)
    │       ├── controller/    (ControladorRoles, ControladorAsignaciones, ControladorMisPermisos, manejadores de error de seguridad)
    │       ├── dto/           (RolDto, PermisoDto, AsignacionDto, MisPermisosDto, UsuarioResumenDto, RolAsignadoDto, Peticiones)
    │       ├── entity/        (Empresa, Usuario, Rol, Permiso, UsuarioRol, UsuarioRolId, BitacoraEvento)
    │       ├── exception/     (RecursoNoEncontrado, ReglaDeNegocio)
    │       ├── mapper/        (RolMapper, PermisoMapper, UsuarioResumenMapper, AsignacionMapper)
    │       ├── repository/    (RolRepositorio, PermisoRepositorio, UsuarioRolRepositorio, UsuarioRepositorio, EmpresaRepositorio, BitacoraRepositorio)
    │       ├── security/      (UsuarioPrincipal, UsuarioActual)
    │       └── service/       (ServicioRoles, ServicioAsignaciones, ServicioAutorizacion, ServicioAprovisionamiento, ArranqueSeguridad)
    │
    └── inventario_y_almacenes/
        └── movimientos_de_inventario/         <-- CU06 (Leonardo)
            ├── controller/    (MovimientoInventarioController)
            ├── dto/           (MovimientoInventarioRequest, MovimientoInventarioResponse, StockAlmacenResponse, AlertaStockMinimoResponse, ProductoSimpleResponse, AlmacenSimpleResponse)
            ├── entity/        (Producto, Categoria, Almacen, StockAlmacen, KardexMovimiento)
            ├── mapper/        (MovimientoInventarioMapper)
            ├── repository/    (ProductoRepository, CategoriaRepository, AlmacenRepository, StockAlmacenRepository, KardexMovimientoRepository)
            └── service/       (MovimientoInventarioService)
```

Las pruebas se organizan en `src/test/java` espejando esa estructura: las de integración en la carpeta del caso de uso y las unitarias en la misma capa del código que prueban.

---

## 3. Casos de Uso y Responsabilidades

### 3.1. `seguridad_y_auditoria` (Seguridad, Usuarios, Auditoría y Autorización)

* **Responsables:** Nicolas (HU-01), Alessandro (HU-02), Santiago (HU-03).
* **`acceso_al_sistema` — HU-01 (Nicolas):** Endpoint `/api/auth/login`, `/api/auth/refresh` y `/api/auth/logout`. Emisión del access token JWT, control de intentos fallidos (`intentos_fallidos`), bloqueo temporal (`bloqueado_hasta`), cierre de sesiones y tabla `sesion`.
* **`gestion_de_usuarios` — HU-02 (Alessandro):** CRUD de cuentas en `/api/usuarios`. Mapeo de la tabla `usuarios` con sus campos de auditoría (`created_by`, `created_at`, `updated_by`, `updated_at`), cambio de estado (`enable`) y registro de cada cambio en `bitacora_logs`. No crea ni modifica roles: usa el servicio de asignaciones de HU-03.
* **`roles_y_permisos` — HU-03 (Santiago):** Roles y matriz de permisos en `/api/seguridad`. Estructura granular del catálogo (`modulo`, `accion`) con las relaciones `usuario_rol` y `rol_permiso`, resueltas en tiempo de ejecución como permisos efectivos. Aprovisiona los 24 permisos y los 7 roles de sistema en cada empresa y audita en `bitacora_auditoria`. Es la pieza de la que depende la protección de todos los demás casos de uso.
* **`compartido`:** entidades y repositorios que usan los tres casos de uso anteriores (`Usuario`, `Rol`, `Permiso`).

### 3.2. `inventario_y_almacenes`

* **`movimientos_de_inventario` — HU-06 (Leonardo):** Control de existencias por almacén y Kardex en `/api/inventario`. Registra entradas, salidas y ajustes de forma transaccional sobre `stock_almacen`, deja el movimiento inmutable en el Kardex, calcula saldos valorados y expone las alertas de stock mínimo.

### 3.3. Casos de uso pendientes

`comercial_y_preventa` (clientes, cotizaciones, pedidos, precios, validación comercial, ventas y facturación), `contabilidad_e_impuestos` (plan de cuentas, asientos, integración contable, cuentas por cobrar, información financiera), `caja_y_arqueo` (caja y cierre diario), `inventario_y_existencia` e `inventario_y_almacenes/catalogo_de_articulos` y `despachos_de_venta` todavía no tienen código: solo la carpeta. Al implementarlos se copia la estructura de las seis capas.

---

## 4. Implementación Representativa: Flujo Completo (Ejemplo Entidad e Implementación de `Usuario.java`)

### 4.1. Entity (`Usuario.java` - Ajustada a Esquema UML)

En el repositorio esta entidad vive en `modulo/seguridad_y_auditoria/compartido/entity`, porque la usan el login, la administración de cuentas y el RBAC. Sobre el esquema de la fase 1 agrega `empresa_id` (aislamiento entre inquilinos) y `rbac_inicializado` (si sus roles ya se migraron al RBAC).

```java
package modulo.seguridad_y_auditoria.compartido.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "fullname", nullable = false)
    private String fullname;

    @Column(nullable = false)
    private Boolean enable = true;

    @Column(name = "intentos_fallidos")
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

```

### 4.2. DTO Request (`UsuarioRequest.java`)

Los DTO de entrada son `record` con las validaciones de Jakarta en sus componentes. Lo que no está declarado no se ignora: se rechaza con 400, para que un intento de enviar un campo protegido quede visible.

```java
package modulo.seguridad_y_auditoria.gestion_de_usuarios.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public record UsuarioRequest(
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}") String username,
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 150) String fullname,
        @NotNull @Size(max = 7) Set<@NotNull UUID> rolesIds) {

    @JsonAnySetter
    public void reject(String key, Object value) {
        throw new IllegalArgumentException("Dato protegido o desconocido: " + key);
    }
}
```

### 4.3. Repository (`UsuarioGestionRepository.java`)

```java
package modulo.seguridad_y_auditoria.gestion_de_usuarios.repository;

import jakarta.persistence.LockModeType;

import modulo.seguridad_y_auditoria.compartido.entity.Usuario;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioGestionRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Optional<Usuario> findByIdAndEmpresaId(Long id, UUID empresaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id and u.empresaId = :empresaId")
    Optional<Usuario> findForAdministration(@Param("id") Long id, @Param("empresaId") UUID empresaId);
}
```

### 4.4. Service (`UsuarioService.java`)

```java
package modulo.seguridad_y_auditoria.gestion_de_usuarios.service;

import lombok.RequiredArgsConstructor;

import comun.exception.ResourceNotFoundException;

import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.UsuarioRequest;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.UsuarioResponse;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.mapper.UsuarioMapper;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.repository.UsuarioGestionRepository;
import modulo.seguridad_y_auditoria.roles_y_permisos.repository.UsuarioRolRepositorio;
import modulo.seguridad_y_auditoria.roles_y_permisos.security.UsuarioActual;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsuarioService {

    private static final int MAX_BYTES_CONTRASENA = 72;

    private final UsuarioGestionRepository usuarioGestionRepository;
    private final UsuarioRolRepositorio usuarioRolRepositorio;   // repositorio de la CU03
    private final UsuarioAuditoriaService auditoria;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse create(UsuarioRequest request, String actor, String ip) {
        UUID empresaId = UsuarioActual.obtener().empresaId();

        if (usuarioGestionRepository.existsByUsernameIgnoreCase(request.username().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya pertenece a otra cuenta");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_CONTRASENA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contrasena excede el limite de 72 bytes");
        }

        Usuario usuario = usuarioMapper.toEntity(request, empresaId, actor);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuarioGestionRepository.saveAndFlush(usuario);
        auditoria.registrar("USUARIO_CREADO", actor, usuario.getId(), ip);

        return response(usuario);
    }

    public UsuarioResponse detail(Long id) {
        return response(usuarioGestionRepository.findByIdAndEmpresaId(id, UsuarioActual.obtener().empresaId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario inexistente")));
    }

    private UsuarioResponse response(Usuario usuario) {
        return usuarioMapper.toResponse(usuario, usuarioRolRepositorio.findRolesDe(
                List.of(usuario.getId()), usuario.getEmpresaId()));
    }
}
```

### 4.5. Controller (`UsuarioController.java`)

```java
package modulo.seguridad_y_auditoria.gestion_de_usuarios.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.UsuarioRequest;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.dto.UsuarioResponse;
import modulo.seguridad_y_auditoria.gestion_de_usuarios.service.UsuarioService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.Principal;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Validated
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request,
                                                 Principal actor,
                                                 HttpServletRequest peticion) {
        UsuarioResponse creado = usuarioService.create(request, actor.getName(), peticion.getRemoteAddr());
        return ResponseEntity.created(URI.create("/api/usuarios/" + creado.id())).body(creado);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasPermission('SEGURIDAD','CONSULTAR')")
    public UsuarioResponse detalle(@PathVariable Long id) {
        return usuarioService.detail(id);
    }
}
```

---

## 5. Diagrama del Flujo de Ejecución REST

```text
[Cliente Web (Angular) / Móvil (Flutter)]
                 │
                 │ 1. HTTP Request + Bearer JWT
                 ▼
    ┌──────────────────────────┐
    │ JwtAuthenticationFilter  │  (Valida Firma, Expiración y Carga authorities)
    └────────────┬─────────────┘
                 │
                 │ 2. Pasa Seguridad
                 ▼
    ┌──────────────────────────┐
    │    UsuarioController     │  (Valida DTO con @Valid, chequea @PreAuthorize)
    └────────────┬─────────────┘
                 │
                 │ 3. Invoca Servicio
                 ▼
    ┌──────────────────────────┐
    │     UsuarioService       │  (@Transactional, ejecuta regla de negocio)
    └────────────┬─────────────┘
                 │
                 │ 4. Transforma y Consulta
                 ▼
    ┌──────────────────────────┐
    │ UsuarioRepository / JPA  │  (Mapea a PostgreSQL 16)
    └────────────┬─────────────┘
                 │
                 │ 5. Convierte Entity a Response DTO mediante Mapper
                 ▼
[Respuesta JSON 200 OK / 201 Created al Frontend]

```

---

## 6. Reglas de Desarrollo para el Equipo

1. **Aislamiento por caso de uso:** cada integrante trabaja dentro de su carpeta `modulo/<modulo_de_negocio>/<caso_de_uso>`. Para reutilizar algo de otro caso de uso se usa su servicio o su repositorio; no se consultan sus tablas con SQL propio.
2. **Seis capas obligatorias:** `controller`, `dto`, `entity`, `mapper`, `repository` y `service`. La conversión entre entidad y DTO va en `mapper`, no en el DTO ni en el servicio.
3. **Prohibido retornar entidades JPA:** todo endpoint devuelve un DTO (`*Request` de entrada y `*Response` de salida). Las entidades no salen de la capa de servicio.
4. **Manejo transaccional strict:** los métodos de lectura usan `@Transactional(readOnly = true)` y los de escritura `@Transactional`. La auditoría se registra dentro de la misma transacción que el cambio.
5. **Seguridad granular RBAC:** cada endpoint se protege con el permiso del catálogo, nunca con el nombre de un rol ni con authorities ad hoc:

   ```java
   @PreAuthorize("hasPermission('SEGURIDAD','MODIFICAR')")
   ```

   Los módulos del catálogo son `SEGURIDAD`, `COMERCIAL`, `INVENTARIO`, `CONTABILIDAD`, `FACTURACION` y `REPORTES`, y las acciones `CREAR`, `CONSULTAR`, `MODIFICAR` y `ANULAR`. Un código que no exista en el catálogo deniega el acceso y deja un aviso en el log.
6. **Aislamiento por empresa:** la empresa nunca se recibe del cliente. Sale de la identidad autenticada (`UsuarioActual`), y toda consulta la filtra.
7. **Sin secretos en las respuestas:** ningún DTO devuelve la contraseña, su hash, tokens ni datos de bloqueo.
