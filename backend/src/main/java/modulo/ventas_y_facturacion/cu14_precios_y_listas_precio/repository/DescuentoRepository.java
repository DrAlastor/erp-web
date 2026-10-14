package modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.repository;

import modulo.ventas_y_facturacion.cu14_precios_y_listas_precio.entity.Descuento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("descuentoTarifasRepository")
public interface DescuentoRepository extends JpaRepository<Descuento, Integer> {

    List<Descuento> findAllByOrderByActivoDescNombreAsc();

    List<Descuento> findByActivoTrue();
}
