package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.repository;

import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.entity.ReporteQbe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteQbeRepository extends JpaRepository<ReporteQbe, Long> {

    List<ReporteQbe> findAllByOrderByFechaCreacionDescIdDesc();
}
