package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.dto;

/** Resultado de un cobro: el recibo emitido, el asiento generado y la cuota al día. */
public record RegistrarCobroResponse(
        CobroResponse cobro,
        String numeroAsiento,
        CuentaPorCobrarResponse cuenta) {}
