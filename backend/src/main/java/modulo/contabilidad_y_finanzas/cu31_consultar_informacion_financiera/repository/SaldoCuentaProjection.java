package modulo.contabilidad_y_finanzas.cu31_consultar_informacion_financiera.repository;

import java.math.BigDecimal;

public interface SaldoCuentaProjection {
    Long getCuentaId();
    BigDecimal getTotalDebe();
    BigDecimal getTotalHaber();
}
