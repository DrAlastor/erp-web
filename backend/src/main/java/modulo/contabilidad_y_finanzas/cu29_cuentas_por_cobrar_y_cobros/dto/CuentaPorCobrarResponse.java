package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Cuota con los datos de la venta y su historial de cobros (el más reciente primero). */
public record CuentaPorCobrarResponse(
        CuentaPorCobrarResumenResponse cuota,
        LocalDate fechaVenta,
        BigDecimal totalVenta,
        String clienteTelefono,
        String clienteEmail,
        boolean cobrable,
        List<CobroResponse> cobros) {}
