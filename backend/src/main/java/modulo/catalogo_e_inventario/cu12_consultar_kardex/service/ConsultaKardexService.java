package modulo.catalogo_e_inventario.cu12_consultar_kardex.service;

import lombok.RequiredArgsConstructor;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Almacen;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.KardexMovimiento;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.entity.Producto;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.AlmacenRepository;
import modulo.catalogo_e_inventario.cu11_movimientos_de_inventario.repository.ProductoRepository;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.dto.KardexConsultaResponse;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.dto.KardexLineaResponse;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.dto.KardexValoracionResponse;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.mapper.KardexMapper;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.repository.KardexConsultaRepository;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.service.KardexCalculador.Calculo;
import modulo.catalogo_e_inventario.cu12_consultar_kardex.service.KardexCalculador.Paso;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CU12 - Consultar Kardex. Es de solo lectura: reconstruye el Kardex valorado desde los
 * movimientos que registra el CU11 y nunca modifica existencias ni el historial.
 */
@Service
@RequiredArgsConstructor
public class ConsultaKardexService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final KardexConsultaRepository kardexRepository;
    private final ProductoRepository productoRepository;
    private final AlmacenRepository almacenRepository;
    private final KardexMapper mapper;

    /**
     * Kardex de un producto en un almacén. El saldo inicial es el que dejaba el último
     * movimiento anterior a {@code fechaInicio}. Si se filtra por tipo de movimiento solo se
     * ocultan filas: los saldos siguen siendo los reales.
     */
    @Transactional(readOnly = true)
    public KardexConsultaResponse consultar(
            Long productoId,
            Integer almacenId,
            MetodoCosteo metodo,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            String tipoMovimiento
    ) {
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de inicio no puede ser posterior a la fecha de fin.");
        }
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Producto con ID " + productoId + " no encontrado."));
        Almacen almacen = almacenRepository.findById(almacenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Almacén con ID " + almacenId + " no encontrado."));

        Calculo calculo = KardexCalculador.calcular(
                kardexRepository.historialDeProducto(productoId, almacenId), metodo);

        BigDecimal saldoInicialCantidad = calculo.aperturaCantidad();
        BigDecimal saldoInicialValor = calculo.aperturaValor();
        List<Paso> enRango = new ArrayList<>();
        for (Paso paso : calculo.pasos()) {
            LocalDateTime fecha = paso.movimiento().getFecha();
            if (fechaInicio != null && fecha.isBefore(fechaInicio)) {
                saldoInicialCantidad = paso.saldoCantidad();
                saldoInicialValor = paso.saldoValor();
            } else if (fechaFin == null || !fecha.isAfter(fechaFin)) {
                enRango.add(paso);
            }
        }

        String tipo = tipoMovimiento != null && !tipoMovimiento.isBlank()
                ? tipoMovimiento.trim().toUpperCase() : null;
        List<Paso> visibles = enRango.stream()
                .filter(p -> tipo == null || tipo.equals(p.movimiento().getTipoMovimiento()))
                .toList();

        BigDecimal entradasCantidad = BigDecimal.ZERO;
        BigDecimal entradasValor = BigDecimal.ZERO;
        BigDecimal salidasCantidad = BigDecimal.ZERO;
        BigDecimal salidasValor = BigDecimal.ZERO;
        for (Paso p : visibles) {
            if (p.entrada()) {
                entradasCantidad = entradasCantidad.add(p.cantidad());
                entradasValor = entradasValor.add(p.valor());
            } else {
                salidasCantidad = salidasCantidad.add(p.cantidad());
                salidasValor = salidasValor.add(p.valor());
            }
        }

        BigDecimal saldoFinalCantidad = saldoInicialCantidad;
        BigDecimal saldoFinalValor = saldoInicialValor;
        BigDecimal saldoFinalCosto = costoUnitario(saldoInicialCantidad, saldoInicialValor);
        if (!enRango.isEmpty()) {
            Paso ultimo = enRango.get(enRango.size() - 1);
            saldoFinalCantidad = ultimo.saldoCantidad();
            saldoFinalValor = ultimo.saldoValor();
            saldoFinalCosto = ultimo.saldoCostoUnitario();
        }

        return KardexConsultaResponse.builder()
                .productoId(producto.getId())
                .productoNombre(producto.getNombre())
                .productoSku(producto.getCodigoSku())
                .unidadMedida(producto.getUnidadMedida())
                .almacenId(almacen.getId())
                .almacenNombre(almacen.getNombre())
                .metodo(metodo.name())
                .metodoEtiqueta(metodo.etiqueta())
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .saldoInicialCantidad(saldoInicialCantidad)
                .saldoInicialValor(saldoInicialValor)
                .totalEntradasCantidad(entradasCantidad)
                .totalEntradasValor(entradasValor)
                .totalSalidasCantidad(salidasCantidad)
                .totalSalidasValor(salidasValor)
                .saldoFinalCantidad(saldoFinalCantidad)
                .saldoFinalCostoUnitario(saldoFinalCosto)
                .saldoFinalValor(saldoFinalValor)
                .movimientos(visibles.stream().map(mapper::toLinea).toList())
                .build();
    }

    /** Existencias valoradas de todos los productos con movimientos en un almacén. */
    @Transactional(readOnly = true)
    public KardexValoracionResponse valorar(Integer almacenId, MetodoCosteo metodo) {
        Almacen almacen = almacenRepository.findById(almacenId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Almacén con ID " + almacenId + " no encontrado."));

        Map<Long, List<KardexMovimiento>> porProducto = new LinkedHashMap<>();
        for (KardexMovimiento m : kardexRepository.historialDeAlmacen(almacenId)) {
            porProducto.computeIfAbsent(m.getProducto().getId(), k -> new ArrayList<>()).add(m);
        }

        List<KardexValoracionResponse.Item> items = new ArrayList<>();
        BigDecimal totalCantidad = BigDecimal.ZERO;
        BigDecimal totalValor = BigDecimal.ZERO;
        for (List<KardexMovimiento> movimientos : porProducto.values()) {
            Calculo calculo = KardexCalculador.calcular(movimientos, metodo);
            Producto producto = movimientos.get(0).getProducto();
            BigDecimal cantidad = calculo.saldoFinalCantidad();
            BigDecimal valor = calculo.saldoFinalValor();
            items.add(KardexValoracionResponse.Item.builder()
                    .productoId(producto.getId())
                    .productoNombre(producto.getNombre())
                    .productoSku(producto.getCodigoSku())
                    .unidadMedida(producto.getUnidadMedida())
                    .cantidad(cantidad)
                    .costoUnitario(costoUnitario(cantidad, valor))
                    .valor(valor)
                    .build());
            totalCantidad = totalCantidad.add(cantidad);
            totalValor = totalValor.add(valor);
        }
        items.sort(Comparator.comparing(KardexValoracionResponse.Item::getProductoNombre,
                String.CASE_INSENSITIVE_ORDER));

        return KardexValoracionResponse.builder()
                .almacenId(almacen.getId())
                .almacenNombre(almacen.getNombre())
                .metodo(metodo.name())
                .metodoEtiqueta(metodo.etiqueta())
                .totalCantidad(totalCantidad)
                .totalValor(totalValor)
                .items(items)
                .build();
    }

    /**
     * Kardex en CSV para abrir en Excel: separador punto y coma, coma decimal y marca BOM
     * para que se conserven las tildes.
     */
    @Transactional(readOnly = true)
    public byte[] exportarCsv(
            Long productoId,
            Integer almacenId,
            MetodoCosteo metodo,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            String tipoMovimiento
    ) {
        KardexConsultaResponse k = consultar(productoId, almacenId, metodo, fechaInicio, fechaFin, tipoMovimiento);

        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Kardex valorado;").append(celda(k.getMetodoEtiqueta())).append('\n');
        csv.append("Producto;").append(celda(k.getProductoSku() + " - " + k.getProductoNombre())).append('\n');
        csv.append("Almacen;").append(celda(k.getAlmacenNombre())).append('\n');
        csv.append("Saldo inicial;").append(numero(k.getSaldoInicialCantidad())).append(';')
                .append(numero(k.getSaldoInicialValor())).append("\n\n");
        csv.append("Fecha;Tipo;Referencia;Motivo;")
                .append("Entrada cant.;Entrada costo unit.;Entrada valor;")
                .append("Salida cant.;Salida costo unit.;Salida valor;")
                .append("Saldo cant.;Saldo costo unit.;Saldo valor\n");
        for (KardexLineaResponse f : k.getMovimientos()) {
            csv.append(f.getFecha().format(FORMATO_FECHA)).append(';')
                    .append(celda(f.getTipoMovimiento())).append(';')
                    .append(celda(f.getReferenciaDoc())).append(';')
                    .append(celda(f.getMotivo())).append(';')
                    .append(numero(f.getEntradaCantidad())).append(';')
                    .append(numero(f.getEntradaCostoUnitario())).append(';')
                    .append(numero(f.getEntradaValor())).append(';')
                    .append(numero(f.getSalidaCantidad())).append(';')
                    .append(numero(f.getSalidaCostoUnitario())).append(';')
                    .append(numero(f.getSalidaValor())).append(';')
                    .append(numero(f.getSaldoCantidad())).append(';')
                    .append(numero(f.getSaldoCostoUnitario())).append(';')
                    .append(numero(f.getSaldoValor())).append('\n');
        }
        csv.append("Totales;;;;")
                .append(numero(k.getTotalEntradasCantidad())).append(";;")
                .append(numero(k.getTotalEntradasValor())).append(';')
                .append(numero(k.getTotalSalidasCantidad())).append(";;")
                .append(numero(k.getTotalSalidasValor())).append(';')
                .append(numero(k.getSaldoFinalCantidad())).append(';')
                .append(numero(k.getSaldoFinalCostoUnitario())).append(';')
                .append(numero(k.getSaldoFinalValor())).append('\n');
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static BigDecimal costoUnitario(BigDecimal cantidad, BigDecimal valor) {
        return cantidad.signum() > 0
                ? valor.divide(cantidad, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4);
    }

    private static String numero(BigDecimal valor) {
        return valor == null ? "" : valor.toPlainString().replace('.', ',');
    }

    /** Entrecomilla el texto y neutraliza las fórmulas para que Excel no las ejecute. */
    private static String celda(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.replace("\"", "\"\"").replace('\n', ' ').replace('\r', ' ');
        if (!limpio.isEmpty() && "=+-@".indexOf(limpio.charAt(0)) >= 0) {
            limpio = "'" + limpio;
        }
        return "\"" + limpio + "\"";
    }
}
