package modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.repository;

import modulo.contabilidad_y_finanzas.cu29_cuentas_por_cobrar_y_cobros.entity.Cobro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CobroRepository extends JpaRepository<Cobro, Long> {

    @Query("SELECT MAX(c.numeroRecibo) FROM CobroCliente c WHERE c.numeroRecibo LIKE :prefijo")
    String ultimoRecibo(@Param("prefijo") String prefijo);
}
