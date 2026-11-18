package modulo.contabilidad_y_finanzas.cu30_consultar_libros_contables.dto;

import java.time.LocalDate;
import java.util.List;

public record LibroMayorResponse(
        String periodoSeleccionado,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        int totalCuentas,
        List<CuentaMayorDTO> cuentas
) {}
