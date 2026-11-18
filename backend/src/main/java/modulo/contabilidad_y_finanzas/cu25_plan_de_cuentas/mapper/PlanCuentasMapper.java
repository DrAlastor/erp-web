package modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.mapper;

import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaNodoResponse;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.dto.CuentaResponse;
import modulo.contabilidad_y_finanzas.cu25_plan_de_cuentas.service.ReglasPlanCuentas;
import modulo.ventas_y_facturacion.compartido.entity.CuentaContable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PlanCuentasMapper {

    public CuentaResponse aRespuesta(CuentaContable cuenta, String codigoPadre, boolean tieneHijos,
                                     boolean tieneMovimientos) {
        return new CuentaResponse(
                cuenta.getId(),
                cuenta.getCodigo(),
                cuenta.getNombre(),
                cuenta.getNivel(),
                cuenta.getTipo(),
                cuenta.getNaturaleza(),
                Boolean.TRUE.equals(cuenta.getPermiteMovimiento()),
                cuenta.getPadreId(),
                codigoPadre,
                Boolean.TRUE.equals(cuenta.getActivo()),
                tieneHijos,
                tieneMovimientos,
                ReglasPlanCuentas.esCuentaIntegracion(cuenta.getCodigo()));
    }

    /**
     * Arma el árbol a partir de la lista plana. Una cuenta cuyo padre no está en la lista se
     * muestra como raíz, así nunca desaparece del árbol.
     */
    public List<CuentaNodoResponse> aArbol(Collection<CuentaContable> cuentas, Set<Long> conMovimientos) {
        Map<Long, CuentaContable> porId = new LinkedHashMap<>();
        cuentas.stream().sorted(Comparator.comparing(CuentaContable::getCodigo))
                .forEach(c -> porId.put(c.getId(), c));

        Map<Long, List<CuentaContable>> hijosDe = new HashMap<>();
        List<CuentaContable> raices = new ArrayList<>();
        for (CuentaContable c : porId.values()) {
            if (c.getPadreId() != null && porId.containsKey(c.getPadreId())) {
                hijosDe.computeIfAbsent(c.getPadreId(), k -> new ArrayList<>()).add(c);
            } else {
                raices.add(c);
            }
        }
        return raices.stream().map(r -> nodo(r, porId, hijosDe, conMovimientos)).toList();
    }

    private CuentaNodoResponse nodo(CuentaContable cuenta, Map<Long, CuentaContable> porId,
                                    Map<Long, List<CuentaContable>> hijosDe, Set<Long> conMovimientos) {
        List<CuentaContable> hijos = hijosDe.getOrDefault(cuenta.getId(), List.of());
        CuentaContable padre = cuenta.getPadreId() == null ? null : porId.get(cuenta.getPadreId());
        CuentaResponse datos = aRespuesta(cuenta, padre == null ? null : padre.getCodigo(),
                !hijos.isEmpty(), conMovimientos.contains(cuenta.getId()));
        return new CuentaNodoResponse(datos,
                hijos.stream().map(h -> nodo(h, porId, hijosDe, conMovimientos)).toList());
    }
}
