package modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila del Libro Diario: cabecera del asiento sin sus líneas. */
public record AsientoResumenResponse(
        Long id,
        String numero,
        LocalDate fecha,
        String tipo,
        String glosa,
        BigDecimal totalDebe,
        BigDecimal totalHaber,
        String estado,
        boolean manual,
        String periodo,
        String creadoPor) {}
