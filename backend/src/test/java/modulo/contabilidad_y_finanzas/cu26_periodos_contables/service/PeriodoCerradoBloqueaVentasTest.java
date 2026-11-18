package modulo.contabilidad_y_finanzas.cu26_periodos_contables.service;

import modulo.ventas_y_facturacion.compartido.entity.PeriodoContable;
import modulo.ventas_y_facturacion.compartido.repository.AsientoContableRepository;
import modulo.ventas_y_facturacion.compartido.repository.CuentaContableRepository;
import modulo.ventas_y_facturacion.compartido.repository.PeriodoContableRepository;
import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService;
import modulo.ventas_y_facturacion.compartido.service.ContabilidadVentaService.Partida;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Un periodo cerrado en el CU26 impide que la integración de ventas registre asientos en él. */
class PeriodoCerradoBloqueaVentasTest {

    @Test
    void laVentaEnUnPeriodoCerradoRespondeConflicto() {
        AsientoContableRepository asientos = mock(AsientoContableRepository.class);
        PeriodoContableRepository periodos = mock(PeriodoContableRepository.class);
        ContabilidadVentaService contabilidad =
                new ContabilidadVentaService(asientos, mock(CuentaContableRepository.class), periodos);

        PeriodoContable noviembre = ReglasPeriodos.nuevo(2026, 11);
        noviembre.setEstado(PeriodoContable.ESTADO_CERRADO);
        when(periodos.findByGestionAndMes(2026, 11)).thenReturn(Optional.of(noviembre));

        BigDecimal monto = new BigDecimal("100.00");
        assertThatThrownBy(() -> contabilidad.registrarAsiento(LocalDate.of(2026, 11, 17), "VENTAS", "Venta",
                1L, List.of(Partida.debe(ContabilidadVentaService.CAJA, "Cobro", monto),
                        Partida.haber(ContabilidadVentaService.VENTAS, "Venta", monto))))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(asientos, never()).save(any());
    }
}
