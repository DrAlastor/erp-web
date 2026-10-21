package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Plan de cuotas para una compra a credito. Sin primer vencimiento se usa la fecha de la
 * compra + dias de credito del proveedor (o +30 si no tiene); sin dias entre cuotas, 30.
 */
public record GenerarCuotasRequest(
        @NotNull(message = "La compra es obligatoria")
        Long compraId,
        @NotNull(message = "El número de cuotas es obligatorio")
        @Min(value = 1, message = "Debe haber al menos 1 cuota")
        @Max(value = 24, message = "No se admiten más de 24 cuotas")
        Integer numeroCuotas,
        LocalDate primerVencimiento,
        @Min(value = 1, message = "Los días entre cuotas deben ser al menos 1")
        @Max(value = 365, message = "Los días entre cuotas no pueden superar 365")
        Integer diasEntreCuotas) {}
