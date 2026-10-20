package modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.repository;

import modulo.ventas_y_facturacion.cu19_registrar_venta_facturacion_y_anulacion.entity.Descuento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DescuentoRepository extends JpaRepository<Descuento, Long> {

    List<Descuento> findByActivoTrueOrderByNombreAsc();
}
