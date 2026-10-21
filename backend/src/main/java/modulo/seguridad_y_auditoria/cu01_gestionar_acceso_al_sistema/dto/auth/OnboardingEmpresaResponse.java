package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth;

public record OnboardingEmpresaResponse(
        String mensaje,
        String empresaNombre,
        String nit,
        String codigoEmpresa,
        String dbName,
        String planCodigo,
        String adminEmail,
        String rolAsignado,
        String estado,
        String checkoutUrl,
        TokenResponse tokens) {
}
