# Paquete `auth` — autenticación mínima (PROVISIONAL)

Este paquete existe porque la **HU-03 (Roles y Permisos)** necesita un usuario
autenticado y con empresa para poder verificarle permisos, y la **CU-01
(Gestionar Acceso al Sistema)** todavía no estaba implementada cuando se
construyó el RBAC.

## Qué hace

- `POST /api/auth/login` verifica email y contraseña (BCrypt) y emite un JWT HS256.
- El token lleva **usuario, empresa, nombre y email**. **No lleva permisos**, a
  propósito: si los llevara, quitarle un rol a alguien no tendría efecto hasta
  que el token venciera.
- `FiltroJwt` deja un `UsuarioPrincipal` en el contexto de seguridad.

## Cómo reemplazarlo sin romper la autorización

El RBAC **no depende de este paquete**, solo del tipo `UsuarioPrincipal`
(`usuarioId`, `empresaId`, `nombre`, `email`). Para poner la CU-01 en su lugar
alcanza con que la autenticación definitiva siga dejando un `UsuarioPrincipal`
en el `SecurityContext`. Concretamente:

1. Reemplazar `ServicioAutenticacion`, `ServicioJwt`, `ControladorAuth` y
   `FiltroJwt` por la implementación de la CU-01.
2. Mantener `UsuarioPrincipal` y `UsuarioActual`, o dejar equivalentes con la
   misma forma.
3. No hace falta tocar nada de `com.uagrm.erp.backend.seguridad`.

## Lo que falta para producción (es de la CU-01, no de la HU-03)

- Refresh tokens y cierre de sesión del lado del servidor.
- Bloqueo por intentos fallidos.
- Recuperación de contraseña.
- Política de complejidad de contraseñas.
- Rotación del secreto (hoy sale de la variable de entorno `ERP_JWT_SECRETO`).
