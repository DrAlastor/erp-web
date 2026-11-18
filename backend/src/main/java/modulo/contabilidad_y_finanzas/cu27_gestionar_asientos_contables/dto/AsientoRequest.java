package modulo.contabilidad_y_finanzas.cu27_gestionar_asientos_contables.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Asiento manual a registrar o a corregir mientras está en BORRADOR. Que cuadre (debe =
 * haber), que cada línea tenga importe en un solo lado y que las cuentas admitan
 * movimiento se valida en el servicio (422), igual que el periodo abierto (409).
 */
public record AsientoRequest(
        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha,
        @NotBlank(message = "El tipo de asiento es obligatorio")
        String tipo,
        @NotBlank(message = "La glosa es obligatoria")
        @Size(max = 500, message = "La glosa no puede superar 500 caracteres")
        String glosa,
        @NotNull(message = "El asiento necesita líneas")
        @Size(min = 2, max = 60, message = "El asiento debe tener entre 2 y 60 líneas")
        List<@Valid Linea> lineas) {

    public record Linea(
            @NotNull(message = "Cada línea necesita una cuenta")
            Long cuentaId,
            @Size(max = 255, message = "La glosa de la línea no puede superar 255 caracteres")
            String glosa,
            @DecimalMin(value = "0.00", message = "El debe no puede ser negativo")
            @Digits(integer = 12, fraction = 2, message = "El debe admite hasta 2 decimales")
            BigDecimal debe,
            @DecimalMin(value = "0.00", message = "El haber no puede ser negativo")
            @Digits(integer = 12, fraction = 2, message = "El haber admite hasta 2 decimales")
            BigDecimal haber) {}
}
