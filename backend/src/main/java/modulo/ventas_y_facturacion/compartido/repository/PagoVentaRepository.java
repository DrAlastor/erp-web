package modulo.ventas_y_facturacion.compartido.repository;

import modulo.ventas_y_facturacion.compartido.entity.PagoVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagoVentaRepository extends JpaRepository<PagoVenta, Long> {

    /** Una sesión de pago en línea solo puede respaldar una factura, aunque luego se anule. */
    boolean existsByTransaccionExterna(String transaccionExterna);
}
