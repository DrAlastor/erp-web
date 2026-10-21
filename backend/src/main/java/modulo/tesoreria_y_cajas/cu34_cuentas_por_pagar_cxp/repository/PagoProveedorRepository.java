package modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.repository;

import modulo.tesoreria_y_cajas.cu34_cuentas_por_pagar_cxp.entity.PagoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PagoProveedorRepository extends JpaRepository<PagoProveedor, Long> {
}
