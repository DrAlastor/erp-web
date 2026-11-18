package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas;

import modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service.ReglasCaja;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasCajaTest {

    @Test
    void reconoceLosRolesDeAdministrador() {
        assertThat(ReglasCaja.esRolAdmin("administrador")).isTrue();
        assertThat(ReglasCaja.esRolAdmin(" ADMIN ")).isTrue();
        assertThat(ReglasCaja.esRolAdmin("SUPERADMIN")).isTrue();
        assertThat(ReglasCaja.esRolAdmin("CAJERO")).isFalse();
        assertThat(ReglasCaja.esRolAdmin(null)).isFalse();
    }

    @Test
    void soloElDuenoOUnAdminAccedenAlTurno() {
        assertThatCode(() -> ReglasCaja.validarAcceso(5L, 5L, false)).doesNotThrowAnyException();
        assertThatCode(() -> ReglasCaja.validarAcceso(5L, 9L, true)).doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglasCaja.validarAcceso(5L, 9L, false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void unTurnoCerradoNoAdmiteOperaciones() {
        assertThatThrownBy(() -> ReglasCaja.validarAbierto(7L, false))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void elEgresoEnEfectivoNoPuedeSuperarElDisponible() {
        assertThatCode(() -> ReglasCaja.validarEgresoEfectivo(new BigDecimal("100.00"), new BigDecimal("100")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> ReglasCaja.validarEgresoEfectivo(new BigDecimal("100.01"), new BigDecimal("100")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bs 100.00")
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void generaElNumeroDeComprobantePorDefecto() {
        assertThat(ReglasCaja.numeroComprobante(12L, 3)).isEqualTo("MOV-12-3");
    }
}
