package modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.controller;

import comun.multitenancy.MasterTenantResolver;
import comun.multitenancy.TenantContext;
import comun.multitenancy.TenantDescriptor;
import comun.saas.SaasOnboardingService;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.LoginRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.ClienteRegisterRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.OnboardingEmpresaRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.OnboardingEmpresaResponse;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.RefreshTokenRequest;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.dto.auth.TokenResponse;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MasterTenantResolver tenantResolver;
    private final SaasOnboardingService onboardingService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = extraerIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        TenantDescriptor tenant = tenantResolver.resolveUserTenant(
                request.getUsernameOrEmail(), request.getCodigoEmpresa());
        return ResponseEntity.ok(TenantContext.execute(
                tenant,
                () -> authService.login(request, ip, userAgent)));
    }

    @PostMapping("/register/cliente")
    public ResponseEntity<TokenResponse> registerCliente(
            @Valid @RequestBody ClienteRegisterRequest request, HttpServletRequest httpRequest) {
        String ip = extraerIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        TenantDescriptor tenant = tenantResolver.resolve(request.codigoEmpresa());
        return ResponseEntity.status(201).body(TenantContext.execute(
                tenant,
                () -> authService.registerCliente(request, ip, userAgent)));
    }

    @PostMapping("/onboarding-empresa")
    public ResponseEntity<OnboardingEmpresaResponse> onboardingEmpresa(
            @Valid @RequestBody OnboardingEmpresaRequest request, HttpServletRequest httpRequest) {
        String ip = extraerIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.status(201).body(onboardingService.register(request));
    }

    @PostMapping("/onboarding-confirmar")
    public ResponseEntity<java.util.Map<String, String>> confirmarOnboarding(
            @RequestParam String sessionId) {
        String codigoEmpresa = onboardingService.confirmStripeSession(sessionId);
        return ResponseEntity.ok(java.util.Map.of(
                "estado", "ACTIVA",
                "codigoEmpresa", codigoEmpresa));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TenantDescriptor tenant = tenantResolver.resolve(request.getCodigoEmpresa());
        return ResponseEntity.ok(TenantContext.execute(tenant, () -> authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        TenantDescriptor tenant = tenantResolver.resolve(request.getCodigoEmpresa());
        TenantContext.execute(tenant, () -> {
            authService.logout(request);
            return null;
        });
        return ResponseEntity.noContent().build();
    }

    private String extraerIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
