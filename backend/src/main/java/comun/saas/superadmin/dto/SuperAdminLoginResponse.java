package comun.saas.superadmin.dto;

public record SuperAdminLoginResponse(
        String token,
        Integer id,
        String email,
        String nombre,
        String apellido,
        String rolGlobal
) {}
