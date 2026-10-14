package modulo.ventas_y_facturacion.cu17_validacion_comercial_aprobacion;

import comun.BackendApplication;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import modulo.seguridad_y_auditoria.compartido.entity.Usuario;
import modulo.seguridad_y_auditoria.compartido.repository.UsuarioRepository;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.security.ApplicationUserPrincipal;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de punta a punta de CU12 y CU17 contra la base configurada. Todo corre dentro de una
 * transacción que se revierte al terminar, así que ni los pedidos de prueba ni sus decisiones
 * quedan guardados. Los datos se buscan en la propia base (un cliente de contado al día, un
 * producto con stock sin reservas) para no depender de ids fijos.
 */
@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc
@Transactional
class ValidacionComercialIntegrationTest {

    private static final int SUCURSAL = 1;

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UsuarioRepository usuarios;
    @PersistenceContext private EntityManager entityManager;

    private int vendedorId;
    private int clienteContadoId;
    private int productoId;
    private BigDecimal stockLibre;

    /** El administrador de la base: tiene todos los permisos del módulo comercial e inventario. */
    private RequestPostProcessor admin() {
        Usuario cuenta = usuarios.findByUsernameOrEmail("admin@erp.com").orElseThrow();
        UsuarioPrincipal identidad = new UsuarioPrincipal(cuenta.getId(), cuenta.getEmpresaId(),
                cuenta.getFullname(), cuenta.getEmail());
        // El token real del administrador trae la autoridad de su rol; el evaluador de permisos la lee.
        return user(new ApplicationUserPrincipal(cuenta.getUsername(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR")), identidad));
    }

    @BeforeEach
    void datosDePrueba() {
        vendedorId = jdbc.queryForObject("SELECT id FROM usuario ORDER BY id LIMIT 1", Integer.class);

        // Cliente de contado, activo y sin cuentas por cobrar abiertas: no dispara mora ni crédito.
        clienteContadoId = jdbc.queryForObject("""
                SELECT c.id FROM clientes c
                WHERE c.activo AND UPPER(c.condicion_pago) = 'CONTADO'
                  AND NOT EXISTS (SELECT 1 FROM cuentas_por_cobrar x WHERE x.cliente_id = c.id AND x.saldo > 0)
                ORDER BY c.id LIMIT 1
                """, Integer.class);

        // Producto con stock en la sucursal y ninguna unidad reservada por pedidos aprobados.
        productoId = jdbc.queryForObject("""
                SELECT i.producto_id FROM inventario_sucursal i
                WHERE i.sucursal_id = ? AND i.stock_disponible >= 2
                  AND NOT EXISTS (
                      SELECT 1 FROM pedido_detalles d JOIN pedidos p ON p.id = d.pedido_id
                      WHERE d.producto_id = i.producto_id AND p.sucursal_id = i.sucursal_id AND p.estado = 'APROBADO')
                ORDER BY i.producto_id LIMIT 1
                """, Integer.class, SUCURSAL);
        stockLibre = jdbc.queryForObject(
                "SELECT stock_disponible FROM inventario_sucursal WHERE sucursal_id = ? AND producto_id = ?",
                BigDecimal.class, SUCURSAL, productoId);
    }

    private int crearPedido(int cantidad, BigDecimal descuento) {
        BigDecimal precio = new BigDecimal("100.00");
        BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(cantidad));
        int id = jdbc.queryForObject("""
                INSERT INTO pedidos (cliente_id, vendedor_id, sucursal_id, numero_pedido, fecha, subtotal,
                                     descuento, total, estado, observaciones, fecha_creacion)
                VALUES (?, ?, ?, ?, CURRENT_DATE, ?, ?, ?, 'PENDIENTE', 'Prueba automatica CU17', NOW())
                RETURNING id
                """, Integer.class, clienteContadoId, vendedorId, SUCURSAL,
                "TEST-CU17-" + UUID.randomUUID().toString().substring(0, 8),
                subtotal, descuento, subtotal.subtract(descuento));
        jdbc.update("""
                INSERT INTO pedido_detalles (pedido_id, producto_id, cantidad, precio_unitario, descuento, subtotal)
                VALUES (?, ?, ?, ?, 0, ?)
                """, id, productoId, cantidad, precio, subtotal);
        return id;
    }

    private String estadoEnBase(int pedidoId) {
        // Lo que Hibernate tiene pendiente se envía a la base antes de leer con JDBC.
        entityManager.flush();
        return jdbc.queryForObject("SELECT estado FROM pedidos WHERE id = ?", String.class, pedidoId);
    }

    @Test
    @DisplayName("Aprobar un pedido sano lo deja APROBADO con la decisión anotada")
    void apruebaPedidoSano() throws Exception {
        int pedido = crearPedido(1, BigDecimal.ZERO);

        mvc.perform(post("/api/validacion-comercial/pedidos/" + pedido + "/aprobar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacion\":\"Todo en orden\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedido.estado").value("APROBADO"))
                .andExpect(jsonPath("$.observaciones").value(org.hamcrest.Matchers.containsString("APROBADO")))
                .andExpect(jsonPath("$.observaciones").value(org.hamcrest.Matchers.containsString("Todo en orden")));

        assertEquals("APROBADO", estadoEnBase(pedido));
    }

    @Test
    @DisplayName("La existencia reservada por un pedido aprobado no se puede prometer de nuevo")
    void reservaBloqueaAlSegundoPedido() throws Exception {
        int primero = crearPedido(stockLibre.intValue(), BigDecimal.ZERO);
        int segundo = crearPedido(1, BigDecimal.ZERO);

        mvc.perform(post("/api/validacion-comercial/pedidos/" + primero + "/aprobar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/validacion-comercial/pedidos/" + segundo + "/aprobar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sin stock suficiente")));

        assertEquals("PENDIENTE", estadoEnBase(segundo));
    }

    @Test
    @DisplayName("En un despacho parcial solo sigue reservado lo que falta despachar")
    void despachoParcialReservaSoloLoPendiente() throws Exception {
        // El pedido pide todo el stock y ya despachó todo menos 1 unidad: queda 1 reservada.
        int parcial = crearPedido(stockLibre.intValue(), BigDecimal.ZERO);
        jdbc.update("UPDATE pedidos SET estado = 'DESPACHO_PARCIAL' WHERE id = ?", parcial);
        int nota = jdbc.queryForObject("""
                INSERT INTO notas_despacho (pedido_id, sucursal_id, despachado_por, numero_despacho, fecha, estado)
                VALUES (?, ?, ?, ?, NOW(), 'DESPACHADO') RETURNING id
                """, Integer.class, parcial, SUCURSAL, vendedorId,
                "TEST-DESP-" + UUID.randomUUID().toString().substring(0, 8));
        jdbc.update("INSERT INTO despacho_detalles (despacho_id, producto_id, cantidad) VALUES (?, ?, ?)",
                nota, productoId, stockLibre.intValue() - 1);

        int pideTodo = crearPedido(stockLibre.intValue(), BigDecimal.ZERO);
        int pideLoLibre = crearPedido(stockLibre.intValue() - 1, BigDecimal.ZERO);

        mvc.perform(get("/api/validacion-comercial/pedidos/" + pideTodo).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas[0].reservado").value(1.0))
                .andExpect(jsonPath("$.lineas[0].cubierta").value(false));
        mvc.perform(get("/api/validacion-comercial/pedidos/" + pideLoLibre).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas[0].cubierta").value(true));
    }

    @Test
    @DisplayName("Un pedido que pide más de lo que hay no se puede aprobar")
    void stockInsuficiente() throws Exception {
        int pedido = crearPedido(stockLibre.intValue() + 1, BigDecimal.ZERO);

        mvc.perform(get("/api/validacion-comercial/pedidos/" + pedido).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeAprobar").value(false))
                .andExpect(jsonPath("$.lineas[0].cubierta").value(false));

        mvc.perform(post("/api/validacion-comercial/pedidos/" + pedido + "/aprobar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
        assertEquals("PENDIENTE", estadoEnBase(pedido));
    }

    @Test
    @DisplayName("Un descuento sobre el tope exige justificación para aprobar")
    void descuentoExigeJustificacion() throws Exception {
        // 2 unidades a 100 = 200 de subtotal; 50 de descuento es 25%, sobre el tope de 10%.
        int pedido = crearPedido(2, new BigDecimal("50.00"));

        mvc.perform(post("/api/validacion-comercial/pedidos/" + pedido + "/aprobar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        assertEquals("PENDIENTE", estadoEnBase(pedido));

        mvc.perform(post("/api/validacion-comercial/pedidos/" + pedido + "/aprobar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacion\":\"Cliente frecuente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedido.estado").value("APROBADO"));
    }

    @Test
    @DisplayName("Rechazar exige un motivo y deja el pedido RECHAZADO")
    void rechazaConMotivo() throws Exception {
        int pedido = crearPedido(1, BigDecimal.ZERO);

        mvc.perform(post("/api/validacion-comercial/pedidos/" + pedido + "/rechazar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacion\":\"  \"}"))
                .andExpect(status().isBadRequest());
        assertEquals("PENDIENTE", estadoEnBase(pedido));

        mvc.perform(post("/api/validacion-comercial/pedidos/" + pedido + "/rechazar").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacion\":\"Cliente sin respaldo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pedido.estado").value("RECHAZADO"));
        assertEquals("RECHAZADO", estadoEnBase(pedido));
    }

    @Test
    @DisplayName("Un pedido ya decidido no admite otra decisión")
    void noSeDecideDosVeces() throws Exception {
        int pedido = crearPedido(1, BigDecimal.ZERO);
        String decision = "/api/validacion-comercial/pedidos/" + pedido + "/aprobar";

        mvc.perform(post(decision).with(admin()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
        mvc.perform(post(decision).with(admin()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("La bandeja filtra por estado y rechaza estados desconocidos")
    void bandejaPorEstado() throws Exception {
        int pedido = crearPedido(1, BigDecimal.ZERO);

        mvc.perform(get("/api/validacion-comercial/pedidos").param("estado", "PENDIENTE").param("size", "100")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + pedido + ")]").isNotEmpty());

        mvc.perform(get("/api/validacion-comercial/pedidos").param("estado", "ANULADO").with(admin()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/validacion-comercial/pedidos").param("estado", "INVENTADO").with(admin()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sin sesión, la validación comercial responde 401")
    void sinSesion() throws Exception {
        mvc.perform(get("/api/validacion-comercial/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/validacion-comercial/pedidos/1/aprobar")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ===================== CU12 - Kardex =====================

    private int productoConKardex() {
        return jdbc.queryForObject(
                "SELECT producto_id FROM kardex_movimientos WHERE sucursal_id = ? ORDER BY producto_id LIMIT 1",
                Integer.class, SUCURSAL);
    }

    @Test
    @DisplayName("El Kardex da la misma existencia con Promedio y con PEPS")
    void kardexMismaExistenciaConAmbosMetodos() throws Exception {
        int producto = productoConKardex();
        double[] cantidades = new double[2];
        String[] metodos = {"PROMEDIO", "PEPS"};
        for (int i = 0; i < metodos.length; i++) {
            String json = mvc.perform(get("/api/kardex").with(admin())
                            .param("productoId", String.valueOf(producto)).param("almacenId", String.valueOf(SUCURSAL))
                            .param("metodo", metodos[i]))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.metodo").value(metodos[i]))
                    .andReturn().getResponse().getContentAsString();
            cantidades[i] = Double.parseDouble(com.jayway.jsonpath.JsonPath.read(json, "$.saldoFinalCantidad").toString());
        }
        assertEquals(cantidades[0], cantidades[1], 0.001);
    }

    @Test
    @DisplayName("El Kardex rechaza un rango de fechas invertido y un método desconocido")
    void kardexValidaParametros() throws Exception {
        int producto = productoConKardex();
        mvc.perform(get("/api/kardex").with(admin())
                        .param("productoId", String.valueOf(producto)).param("almacenId", String.valueOf(SUCURSAL))
                        .param("fechaInicio", "2026-12-31T00:00:00").param("fechaFin", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/kardex").with(admin())
                        .param("productoId", String.valueOf(producto)).param("almacenId", String.valueOf(SUCURSAL))
                        .param("metodo", "LIFO"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/kardex").with(admin())
                        .param("productoId", "99999999").param("almacenId", String.valueOf(SUCURSAL)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El Kardex se exporta como CSV con encabezado y totales")
    void kardexExportaCsv() throws Exception {
        int producto = productoConKardex();
        String csv = mvc.perform(get("/api/kardex/exportar").with(admin())
                        .param("productoId", String.valueOf(producto)).param("almacenId", String.valueOf(SUCURSAL)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("﻿Kardex valorado;"), "debe empezar con BOM y título");
        assertTrue(csv.contains("Saldo valor"), "debe tener la fila de encabezados");
        assertTrue(csv.contains("Totales;"), "debe cerrar con la fila de totales");
    }

    @Test
    @DisplayName("La valoración del almacén suma el valor de sus productos")
    void valoracionDelAlmacen() throws Exception {
        String json = mvc.perform(get("/api/kardex/valoracion").with(admin())
                        .param("almacenId", String.valueOf(SUCURSAL)).param("metodo", "PEPS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andReturn().getResponse().getContentAsString();

        List<Number> valores = com.jayway.jsonpath.JsonPath.read(json, "$.items[*].valor");
        double suma = valores.stream().mapToDouble(Number::doubleValue).sum();
        double total = Double.parseDouble(com.jayway.jsonpath.JsonPath.read(json, "$.totalValor").toString());
        assertEquals(suma, total, 0.01);
    }
}
