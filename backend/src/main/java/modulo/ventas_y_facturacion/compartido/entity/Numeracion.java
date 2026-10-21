package modulo.ventas_y_facturacion.compartido.entity;

import java.util.UUID;

/**
 * Números correlativos de los documentos comerciales (COT-000123, PED-000012, DES-000045).
 *
 * <p>El correlativo sale del id que asigna la base, así que es único sin contadores
 * aparte. Como la columna del número es NOT NULL, se inserta primero con un número
 * provisional y se reemplaza dentro de la misma transacción.
 */
public final class Numeracion {

    private Numeracion() {
    }

    public static String provisional() {
        return "TMP-" + UUID.randomUUID().toString().replace("-", "");
    }

    public static String formatear(String prefijo, Integer id) {
        return String.format("%s-%06d", prefijo, id);
    }
}
