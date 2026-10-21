package modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Caja;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Cobrar;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Comercial;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Inventario;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Pagar;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.DashboardEjecutivoResponse.Ventas;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.dto.SucursalOpcionResponse;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.repository.DashboardConsultaRepository;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.repository.DashboardConsultaRepository.ResumenStock;
import modulo.reportes_y_bi.cu39_dashboard_analitico_kpis.repository.DashboardConsultaRepository.Suma;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * HU-36 / CU39: dashboard ejecutivo, una foto rapida de la empresa en una pantalla.
 *
 * <p>Solo lectura. Cada bloque se calcula de forma independiente y aislada: si su consulta
 * falla se registra en el log y el bloque sale en ceros, sin tumbar el endpoint. Por eso el
 * servicio NO es transaccional: en PostgreSQL un error dentro de una transaccion abortaria
 * todas las consultas siguientes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardEjecutivoService {

    private static final int DIAS_SERIE = 30;
    private static final int TOP = 5;
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(2);

    private final DashboardConsultaRepository consultas;

    public DashboardEjecutivoResponse ejecutivo(Long sucursalId) {
        LocalDate hoy = LocalDate.now();
        Ventas ventas = bloque("ventas", () -> ventas(hoy, sucursalId),
                new Ventas(CERO, CERO, CERO, null, 0, CERO,
                        SerieDiaria.rellenar(Map.of(), hoy.minusDays(DIAS_SERIE - 1), hoy), List.of()));
        Comercial comercial = bloque("comercial", () -> new Comercial(
                        consultas.pedidosPorEstado(sucursalId),
                        consultas.cotizacionesVigentes(hoy),
                        consultas.clientesActivos()),
                new Comercial(List.of(), 0, 0));
        Cobrar cobrar = bloque("cobrar", () -> {
            Cobrar c = consultas.cobrar(hoy, sucursalId);
            return new Cobrar(escala(c.saldoTotal()), escala(c.vencido()), c.cantidadVencidas());
        }, new Cobrar(CERO, CERO, 0));
        Pagar pagar = bloque("pagar", () -> {
            Pagar p = consultas.pagar(hoy, hoy.plusDays(7), sucursalId);
            return new Pagar(escala(p.saldoTotal()), escala(p.vencido()), escala(p.porVencer7Dias()),
                    p.cantidadVencidas());
        }, new Pagar(CERO, CERO, CERO, 0));
        Inventario inventario = bloque("inventario", () -> {
            ResumenStock r = consultas.resumenStock(sucursalId);
            return new Inventario(r.productosActivos(), r.bajoStockMinimo(), r.sinStock(), escala(r.valor()),
                    consultas.stockCritico(sucursalId, TOP));
        }, new Inventario(0, 0, 0, CERO, List.of()));
        Caja caja = bloque("caja", () -> {
            Caja c = consultas.caja(hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay(), sucursalId);
            return new Caja(c.turnosAbiertos(), escala(c.ingresosHoy()), escala(c.egresosHoy()));
        }, new Caja(0, CERO, CERO));

        return new DashboardEjecutivoResponse(
                LocalDateTime.now(), sucursalId, ventas, comercial, cobrar, pagar, inventario, caja,
                GeneradorAlertas.generar(ventas, comercial, cobrar, pagar, inventario));
    }

    public List<SucursalOpcionResponse> sucursales() {
        return consultas.sucursalesActivas();
    }

    private Ventas ventas(LocalDate hoy, Long sucursalId) {
        LocalDateTime manana = hoy.plusDays(1).atStartOfDay();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate inicioAnterior = inicioMes.minusMonths(1);
        // Mismo tramo del mes anterior: del dia 1 al mismo dia (acotado al largo de ese mes).
        LocalDate finAnterior = inicioAnterior.withDayOfMonth(
                Math.min(hoy.getDayOfMonth(), inicioAnterior.lengthOfMonth()));

        BigDecimal ventasHoy = consultas.ventas(hoy.atStartOfDay(), manana, sucursalId).total();
        Suma mes = consultas.ventas(inicioMes.atStartOfDay(), manana, sucursalId);
        BigDecimal anterior = consultas.ventas(
                inicioAnterior.atStartOfDay(), finAnterior.plusDays(1).atStartOfDay(), sucursalId).total();
        LocalDate desdeSerie = hoy.minusDays(DIAS_SERIE - 1);

        return new Ventas(
                escala(ventasHoy),
                escala(mes.total()),
                escala(anterior),
                variacion(mes.total(), anterior),
                mes.cantidad(),
                mes.cantidad() == 0 ? CERO : escala(mes.total().divide(BigDecimal.valueOf(mes.cantidad()), 2, RoundingMode.HALF_UP)),
                SerieDiaria.rellenar(consultas.ventasPorDia(desdeSerie.atStartOfDay(), manana, sucursalId), desdeSerie, hoy),
                consultas.topProductos(inicioMes.atStartOfDay(), manana, sucursalId, TOP));
    }

    /** Variacion porcentual (2 decimales) de actual contra anterior; null si anterior es 0. */
    public static BigDecimal variacion(BigDecimal actual, BigDecimal anterior) {
        if (anterior == null || anterior.signum() == 0) return null;
        return escala(actual).subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior, 2, RoundingMode.HALF_UP);
    }

    private <T> T bloque(String nombre, Supplier<T> calculo, T vacio) {
        try {
            return calculo.get();
        } catch (RuntimeException e) {
            log.warn("Dashboard ejecutivo: no se pudo calcular el bloque '{}': {}", nombre, e.getMessage());
            return vacio;
        }
    }

    private static BigDecimal escala(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
