package modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity;

/** Condición de pago pactada con el proveedor (columna CONDICION_PAGO de PROVEEDORES). */
public enum CondicionPago {
    /** Se paga al recibir la compra: no tiene días de crédito. */
    CONTADO,
    /** Se paga a plazo: exige días de crédito mayores a cero. */
    CREDITO
}
