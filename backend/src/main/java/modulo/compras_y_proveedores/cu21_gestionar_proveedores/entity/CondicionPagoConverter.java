package modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Lee la columna CONDICION_PAGO sin romperse con datos cargados a mano en la base
 * ("credito", "Crédito", " CONTADO "...). Con {@code @Enumerated(STRING)} un solo valor
 * distinto tiraba el listado completo con un 500. Siempre escribe CONTADO o CREDITO.
 */
@Converter
public class CondicionPagoConverter implements AttributeConverter<CondicionPago, String> {

    @Override
    public String convertToDatabaseColumn(CondicionPago condicion) {
        return condicion == null ? null : condicion.name();
    }

    @Override
    public CondicionPago convertToEntityAttribute(String valor) {
        if (valor == null || valor.isBlank()) return null;
        String limpio = valor.trim().toUpperCase().replace('É', 'E');
        return limpio.startsWith("CRED") ? CondicionPago.CREDITO : CondicionPago.CONTADO;
    }
}
