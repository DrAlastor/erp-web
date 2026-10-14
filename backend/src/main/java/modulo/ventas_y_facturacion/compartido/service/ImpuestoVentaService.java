package modulo.ventas_y_facturacion.compartido.service;

import lombok.RequiredArgsConstructor;
import modulo.ventas_y_facturacion.compartido.entity.Impuesto;
import modulo.ventas_y_facturacion.compartido.repository.ImpuestoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

/**
 * Tasas de IVA e IT vigentes, leídas de la tabla impuestos (CU19). Un impuesto inactivo
 * se factura con tasa cero; uno que no existe es un error de configuración.
 */
@Service
@RequiredArgsConstructor
public class ImpuestoVentaService {

    private final ImpuestoRepository repository;

    /** Tasas en porcentaje (13.00 = 13 %). */
    public record Tasas(BigDecimal iva, BigDecimal it) {

        public static final Tasas SIN_IMPUESTOS = new Tasas(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public Tasas vigentes() {
        return new Tasas(tasa(Impuesto.IVA), tasa(Impuesto.IT));
    }

    private BigDecimal tasa(String codigo) {
        Impuesto impuesto = repository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Falta configurar el impuesto " + codigo + " en la tabla de impuestos."));
        return Boolean.TRUE.equals(impuesto.getActivo()) ? impuesto.getPorcentaje() : BigDecimal.ZERO;
    }
}
