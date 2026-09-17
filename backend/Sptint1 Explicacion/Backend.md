# Arquitectura del Backend ERP (Fase 1)
## Ubicación actual de la infraestructura común

El código actual utiliza `src/main/java/comun` para `BackendApplication`, `config` (CORS y seguridad), `controller/HealthController` y `exception` (respuesta de error, recurso no encontrado y manejador global). Spring detecta componentes en `comun` y `modulo`, y entidades y repositorios bajo `modulo`.

El CU01 está en `src/main/java/modulo/seguridad_y_auditoria/acceso_al_sistema`. Sus entidades, repositorios, servicio, DTO, seguridad JWT y excepciones específicas del login permanecen allí. La distribución de entidades compartidas queda pendiente de revisión con el compañero responsable. Los diagramas originales que aparecen más abajo describen la propuesta anterior de paquetes.

El backend está construido bajo el patrón de **monolito modular** en Spring Boot. Cada módulo funcional empaqueta sus propias capas del patrón MVC/Domain (`controller`, `dto`, `entity`, `mapper`, `repository`, `service`), compartiendo únicamente infraestructura transversal (seguridad, excepciones, auditoría y configuración base).

```text
com.erp.backend
├── config
├── exception
├── security
├── seed
├── modulo_acceso        <-- (HU-01, HU-02, HU-03) Nicolas, Alessandro, Santi
├── modulo_comercial     <-- (HU-04) Javier
└── modulo_inventario    <-- (HU-05, HU-06) Javier, Leonardo

```

---

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

## 2. Estructura Completa de Paquetes por Módulo

```text
com.uagrm.erp.backend/
├── config/
│   ├── SecurityConfig.java
│   └── CorsConfig.java
├── exception/
│   ├── ErrorResponse.java
│   ├── ResourceNotFoundException.java
│   ├── InvalidCredentialsException.java
│   ├── AccountLockedException.java
│   └── GlobalExceptionHandler.java
├── security/
│   ├── JwtAuthenticationFilter.java
│   ├── JwtService.java
│   └── ApplicationUserPrincipal.java
├── seed/
│   └── InitializerSeeder.java
│
├── modulo_acceso/                      <-- (HU-01, HU-02, HU-03) Nicolas, Alessandro, Santi
│   ├── controller/
│   │   ├── AuthController.java         <-- Nicolas (HU-01)
│   │   ├── UsuarioController.java      <-- Alessandro (HU-02)
│   │   └── RolPermisoController.java   <-- Santi (HU-03)
│   ├── dto/
│   │   ├── auth/ (LoginRequest, RefreshTokenRequest, TokenResponse, UsuarioPerfilResponse, PasswordResetRequest)
│   │   ├── usuario/ (UsuarioRequest, UsuarioResponse)
│   │   └── rol/ (RolRequest, RolResponse, PermisoResponse)
│   ├── entity/
│   │   ├── Usuario.java                <-- Mapea 'usuarios'
│   │   ├── Rol.java                    <-- Mapea 'roles'
│   │   ├── Permiso.java                <-- Mapea 'permisos'
│   │   ├── Sesion.java                 <-- Mapea 'sesion'
│   │   ├── PasswordResetToken.java     <-- Mapea 'password_reset_tokens'
│   │   └── BitacoraLog.java            <-- Mapea 'bitacora_logs'
│   ├── mapper/
│   │   ├── UsuarioMapper.java
│   │   └── RolMapper.java
│   ├── repository/
│   │   ├── UsuarioRepository.java
│   │   ├── RolRepository.java
│   │   ├── PermisoRepository.java
│   │   ├── SesionRepository.java
│   │   ├── PasswordResetTokenRepository.java
│   │   └── BitacoraLogRepository.java
│   └── service/
│       ├── AuthService.java            <-- Nicolas (HU-01)
│       ├── UsuarioService.java         <-- Alessandro (HU-02)
│       └── RolPermisoService.java      <-- Santi (HU-03)
│
├── modulo_comercial/                   <-- Javier (HU-04)
│   ├── controller/
│   │   └── ClienteController.java
│   ├── dto/
│   │   └── cliente/ (ClienteRequest, ClienteResponse)
│   ├── entity/
│   │   └── Cliente.java
│   ├── mapper/
│   │   └── ClienteMapper.java
│   ├── repository/
│   │   └── ClienteRepository.java
│   └── service/
│       └── ClienteService.java
│
└── modulo_inventario/                  <-- Javier (HU-05), Leonardo (HU-06)
    ├── controller/
    │   ├── ProductoController.java     <-- Javier (HU-05)
    │   └── StockController.java        <-- Leonardo (HU-06)
    ├── dto/
    │   ├── producto/ (ProductoRequest, ProductoResponse, CategoriaResponse)
    │   └── stock/ (AjusteStockRequest, StockAlmacenResponse)
    ├── entity/
    │   ├── Categoria.java
    │   ├── Producto.java
    │   ├── Almacen.java
    │   └── StockAlmacen.java
    ├── mapper/
    │   ├── ProductoMapper.java
    │   └── StockMapper.java
    ├── repository/
    │   ├── CategoriaRepository.java
    │   ├── ProductoRepository.java
    │   ├── AlmacenRepository.java
    │   └── StockAlmacenRepository.java
    └── service/
        ├── ProductoService.java        <-- Javier (HU-05)
        └── StockService.java           <-- Leonardo (HU-06)

```

---

## 3. Módulos de Negocio y Responsabilidades

### 3.1. `modulo_acceso` (Seguridad, Usuarios, Auditoría y Restablecimiento)

* **Responsables:** Nicolas (HU-01), Alessandro (HU-02), Santi (HU-03).
* **Alcance:**
* **HU-01 (Nicolas):** Endpoint `/api/auth/login` y `/api/auth/refresh`. Emisión de Access Token JWT. Control de intentos fallidos de inicio de sesión (`intentos_fallidos`), bloqueo temporal (`bloqueado_hasta`) y generación de tokens de recuperación en la tabla `password_reset_tokens`.


* **HU-02 (Alessandro):** CRUD completo de usuarios (`/api/usuarios`). Mapeo directo con la tabla `usuarios` incluyendo campos de auditoría (`created_by`, `created_at`, `updated_by`, `updated_at`) y cambio de estado (`enable`).


* **HU-03 (Santi):** Gestión de Roles y Permisos RBAC (`/api/roles`). Mapeo de la estructura granular de la tabla `permisos` (`modulo`, `pantalla`, `accion`) mediante las relaciones `usuario_roles` y `rol_permisos`, transformadas dinámicamente en `Authorities` de Spring Security. Registros de auditoría mediante `bitacora_logs`.





### 3.2. `modulo_comercial` (Gestión Comercial Base)

* **Responsable:** Javier (HU-04).
* **Alcance:**
* **HU-04 (Javier):** Registro y consulta de clientes (`/api/clientes`). Validación de formato único de NIT/CI y filtrado de clientes activos.



### 3.3. `modulo_inventario` (Catálogo y Existencias)

* **Responsables:** Javier (HU-05), Leonardo (HU-06).
* **Alcance:**
* **HU-05 (Javier):** Catálogo maestro de productos y categorías (`/api/productos`). Control de SKU, código de barras y stock mínimo.
* **HU-06 (Leonardo):** Control de existencias físicas por almacén (`/api/stock`). Consulta de disponibilidad actual y registro de ajustes de entrada/salida de stock en `stock_almacen`.



---

## 4. Implementación Representativa: Flujo Completo (Ejemplo Entidad e Implementación de `Usuario.java`)

### 4.1. Entity (`Usuario.java` - Ajustada a Esquema UML)

```java
package com.erp.backend.modulo_acceso.entity;

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

```java
package com.erp.backend.modulo_acceso.dto.usuario;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.Set;

@Data
public class UsuarioRequest {
    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "Formato de correo inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;

    @NotBlank(message = "El nombre completo es obligatorio")
    private String fullname;

    private Set<Long> rolesIds;
}

```

### 4.3. Repository (`UsuarioRepository.java`)

```java
package com.erp.backend.modulo_acceso.repository;

import com.erp.backend.modulo_acceso.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}

```

### 4.4. Service (`UsuarioService.java`)

```java
package com.erp.backend.modulo_acceso.service;

import com.erp.backend.exception.ResourceNotFoundException;
import com.erp.backend.modulo_acceso.dto.usuario.*;
import com.erp.backend.modulo_acceso.entity.Usuario;
import com.erp.backend.modulo_acceso.mapper.UsuarioMapper;
import com.erp.backend.modulo_acceso.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("El nombre de usuario ya está registrado");
        }
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }
        
        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtenerPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
        return usuarioMapper.toResponse(usuario);
    }
}

```

### 4.5. Controller (`UsuarioController.java`)

```java
package com.erp.backend.modulo_acceso.controller;

import com.erp.backend.modulo_acceso.dto.usuario.*;
import com.erp.backend.modulo_acceso.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @PreAuthorize("hasAuthority('ACCESO:USUARIOS:ESCRITURA')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACCESO:USUARIOS:LECTURA')")
    public ResponseEntity<UsuarioResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.obtenerPorId(id));
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

1. **Aislamiento por Módulo:** Nicolas, Alessandro y Santi trabajan exclusivamente dentro de `modulo_acceso`. Javier en `modulo_comercial` e `modulo_inventario` (productos), y Leonardo en `modulo_inventario` (stock).
2. **Prohibido retornar Entidades JPA:** Todo endpoint debe retornar un `DTO` (`Response`). Las entidades JPA no salen de la capa de Servicio.
3. **Manejo Transaccional Strict:** Métodos de lectura usan `@Transactional(readOnly = true)`. Métodos de escritura usan `@Transactional`.
4. **Seguridad Granular RBAC:** Proteger cada endpoint en los controllers con `@PreAuthorize("hasAuthority('MODULO:PANTALLA:ACCION')")` concatenando las columnas de la tabla `permisos` (`modulo`, `pantalla`, `accion`) cargadas por la HU-03.