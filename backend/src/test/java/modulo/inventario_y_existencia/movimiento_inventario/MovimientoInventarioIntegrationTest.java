package modulo.inventario_y_existencia.movimiento_inventario;

import com.fasterxml.jackson.databind.ObjectMapper;
import comun.BackendApplication;
import modulo.inventario_y_existencia.movimiento_inventario.dto.MovimientoInventarioRequest;
import modulo.inventario_y_existencia.movimiento_inventario.entity.Almacen;
import modulo.inventario_y_existencia.movimiento_inventario.entity.Producto;
import modulo.inventario_y_existencia.movimiento_inventario.entity.StockAlmacen;
import modulo.inventario_y_existencia.movimiento_inventario.repository.AlmacenRepository;
import modulo.inventario_y_existencia.movimiento_inventario.repository.KardexMovimientoRepository;
import modulo.inventario_y_existencia.movimiento_inventario.repository.ProductoRepository;
import modulo.inventario_y_existencia.movimiento_inventario.repository.StockAlmacenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@Transactional
class MovimientoInventarioIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private AlmacenRepository almacenRepository;
    @Autowired private StockAlmacenRepository stockRepository;
    @Autowired private KardexMovimientoRepository kardexRepository;

    private Producto producto;
    private Almacen almacen;

    private RequestPostProcessor almaceneroAuth() {
        return user("leonardo")
                .authorities(
                        new SimpleGrantedAuthority("INVENTARIO:MOVIMIENTOS:ESCRITURA"),
                        new SimpleGrantedAuthority("INVENTARIO:MOVIMIENTOS:LECTURA"),
                        new SimpleGrantedAuthority("ROLE_ALMACENERO")
                );
    }

    @BeforeEach
    void setUp() {
        producto = productoRepository.findById(1L).orElseGet(() ->
                productoRepository.save(Producto.builder()
                        .nombre("Producto Test")
                        .codigoSku("SKU-TEST-01")
                        .precioVenta(new BigDecimal("100.00"))
                        .costoPromedio(new BigDecimal("60.00"))
                        .stockMinimo(5)
                        .activo(true)
                        .build())
        );

        almacen = almacenRepository.findById(1).orElseGet(() ->
                almacenRepository.save(Almacen.builder()
                        .nombre("Almacen Principal")
                        .esPrincipal(true)
                        .activo(true)
                        .build())
        );
    }

    @Test
    @DisplayName("Debe rechazar peticiones no autenticadas con 401 Unauthorized")
    void debeRechazarPeticionSinAutenticacion() throws Exception {
        mvc.perform(get("/api/inventario/movimientos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("HU-06: Debe registrar ENTRADA e incrementar el stock en el almacén")
    void debeRegistrarEntradaDeInventario() throws Exception {
        BigDecimal stockInicial = stockRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
                .map(StockAlmacen::getCantidadActual)
                .orElse(BigDecimal.ZERO);

        MovimientoInventarioRequest request = MovimientoInventarioRequest.builder()
                .productoId(producto.getId())
                .almacenId(almacen.getId())
                .tipoMovimiento("ENTRADA")
                .cantidad(new BigDecimal("10.00"))
                .costoUnitario(new BigDecimal("950.00"))
                .referenciaDoc("FACT-COMPRA-001")
                .motivo("Ingreso de mercadería por compra de lote")
                .fecha(LocalDateTime.now())
                .build();

        mvc.perform(post("/api/inventario/movimientos")
                        .with(almaceneroAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tipoMovimiento").value("ENTRADA"))
                .andExpect(jsonPath("$.cantidad").value(10.00))
                .andExpect(jsonPath("$.saldoCantidad").value(stockInicial.add(new BigDecimal("10.00")).doubleValue()));

        StockAlmacen stockActualizado = stockRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId()).orElseThrow();
        assertEquals(0, stockInicial.add(new BigDecimal("10.00")).compareTo(stockActualizado.getCantidadActual()));
    }

    @Test
    @DisplayName("HU-06: Debe registrar SALIDA y decrementar el stock cuando hay existencias suficientes")
    void debeRegistrarSalidaDeInventarioConStockSuficiente() throws Exception {
        StockAlmacen stock = stockRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
                .orElseGet(() -> StockAlmacen.builder().producto(producto).almacen(almacen).build());
        stock.setCantidadActual(new BigDecimal("20.00"));
        stockRepository.saveAndFlush(stock);

        MovimientoInventarioRequest request = MovimientoInventarioRequest.builder()
                .productoId(producto.getId())
                .almacenId(almacen.getId())
                .tipoMovimiento("SALIDA")
                .cantidad(new BigDecimal("5.00"))
                .referenciaDoc("NOTA-DESPACHO-01")
                .motivo("Despacho a cliente")
                .build();

        mvc.perform(post("/api/inventario/movimientos")
                        .with(almaceneroAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoMovimiento").value("SALIDA"))
                .andExpect(jsonPath("$.saldoCantidad").value(15.00));

        StockAlmacen stockActualizado = stockRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("15.00").compareTo(stockActualizado.getCantidadActual()));
    }

    @Test
    @DisplayName("HU-06 Criterio de Aceptación: Debe evitar salidas que superen el stock disponible con 400 Bad Request")
    void debeRechazarSalidaQueSuperaStockDisponible() throws Exception {
        StockAlmacen stock = stockRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
                .orElseGet(() -> StockAlmacen.builder().producto(producto).almacen(almacen).build());
        stock.setCantidadActual(new BigDecimal("3.00"));
        stockRepository.saveAndFlush(stock);

        MovimientoInventarioRequest request = MovimientoInventarioRequest.builder()
                .productoId(producto.getId())
                .almacenId(almacen.getId())
                .tipoMovimiento("SALIDA")
                .cantidad(new BigDecimal("10.00"))
                .motivo("Intento de salida sin stock")
                .build();

        mvc.perform(post("/api/inventario/movimientos")
                        .with(almaceneroAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU-06 Criterio de Aceptación: Los ajustes de inventario exigen motivo o justificación obligatoria")
    void debeExigirMotivoEnAjusteDeInventario() throws Exception {
        MovimientoInventarioRequest requestSinMotivo = MovimientoInventarioRequest.builder()
                .productoId(producto.getId())
                .almacenId(almacen.getId())
                .tipoMovimiento("AJUSTE_POSITIVO")
                .cantidad(new BigDecimal("2.00"))
                .motivo("   ") // en blanco
                .build();

        mvc.perform(post("/api/inventario/movimientos")
                        .with(almaceneroAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(requestSinMotivo)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("HU-06: Consulta del historial de movimientos (Kardex) con paginación")
    void debeConsultarHistorialKardex() throws Exception {
        MovimientoInventarioRequest request = MovimientoInventarioRequest.builder()
                .productoId(producto.getId())
                .almacenId(almacen.getId())
                .tipoMovimiento("ENTRADA")
                .cantidad(new BigDecimal("2.00"))
                .motivo("Stock inicial para prueba de Kardex")
                .build();

        mvc.perform(post("/api/inventario/movimientos")
                        .with(almaceneroAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/inventario/movimientos")
                        .with(almaceneroAuth())
                        .param("productoId", producto.getId().toString())
                        .param("almacenId", almacen.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("Administrar Stock por Almacén: Consulta paginada por almacén específico")
    void debeConsultarStockPorAlmacen() throws Exception {
        mvc.perform(get("/api/inventario/stock/almacen/" + almacen.getId())
                        .with(almaceneroAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Alertas de Stock Mínimo: Identifica existencias con cantidad actual <= stock mínimo")
    void debeConsultarAlertasDeStockMinimo() throws Exception {
        // Fijar existencia baja para el producto (stockActual: 2, stockMinimo: 5 => alerta con déficit 3)
        StockAlmacen stock = stockRepository.findByProductoIdAndAlmacenId(producto.getId(), almacen.getId())
                .orElseGet(() -> StockAlmacen.builder().producto(producto).almacen(almacen).build());
        stock.setCantidadActual(new BigDecimal("2.00"));
        stockRepository.saveAndFlush(stock);

        mvc.perform(get("/api/inventario/stock/alertas-minimo")
                        .with(almaceneroAuth())
                        .param("almacenId", almacen.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].deficit").exists())
                .andExpect(jsonPath("$[0].nivelCriticidad").exists());
    }

    @Test
    @DisplayName("Consulta de Stock Disponible para integración con otros módulos")
    void debeConsultarStockDisponible() throws Exception {
        mvc.perform(get("/api/inventario/stock/disponible")
                        .with(almaceneroAuth())
                        .param("productoId", producto.getId().toString())
                        .param("almacenId", almacen.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productoId").value(producto.getId()))
                .andExpect(jsonPath("$.stockDisponible").isNumber());
    }
}
