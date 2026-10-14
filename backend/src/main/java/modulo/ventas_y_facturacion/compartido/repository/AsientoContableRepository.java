package modulo.ventas_y_facturacion.compartido.repository;

import modulo.ventas_y_facturacion.compartido.entity.AsientoContable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AsientoContableRepository extends JpaRepository<AsientoContable, Long> {

    @Query("SELECT MAX(a.numeroAsiento) FROM AsientoContable a WHERE a.numeroAsiento LIKE :prefijo")
    String ultimoNumero(@Param("prefijo") String prefijo);
}
