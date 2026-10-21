package comun;

import modulo.seguridad_y_auditoria.cu07_gestionar_backups.service.BitacoraAuditoriaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BitacoraMultimoduloTest {

    @Autowired
    private BitacoraAuditoriaService bitacoraAuditoriaService;

    @Test
    void testRegistrarEventosMultimoduloYVerificarCadena() {
        // 1. Registrar movimiento de inventario / stock
        bitacoraAuditoriaService.registrarEvento(
                1,
                "ASUS-7K4P2M",
                "alessandro@asustech.bo",
                "MOVIMIENTO_STOCK",
                "INVENTARIO",
                "Ingreso por compra: 25 u. de 'Laptop ROG Zephyrus G16' al Almacén Central.",
                "192.168.1.45",
                "EXITO",
                "BAJA",
                "{\"productoId\":101,\"sku\":\"ASUS-ROG-G16\",\"almacenId\":1,\"cantidad\":25,\"saldo\":50}"
        );

        // 2. Registrar venta y facturación
        bitacoraAuditoriaService.registrarEvento(
                1,
                "ASUS-7K4P2M",
                "cajero.central@asustech.bo",
                "EMISION_VENTA",
                "VENTAS",
                "Emisión de Factura Computarizada #10042 por Bs. 18.500,00 para 'Tecnología Santa Cruz S.R.L.'.",
                "192.168.1.12",
                "EXITO",
                "MEDIA",
                "{\"ventaId\":85,\"factura\":10042,\"total\":18500.00,\"clienteId\":12,\"tipoComprobante\":\"FACTURA\"}"
        );

        // 3. Registrar cambio en catálogo de artículos
        bitacoraAuditoriaService.registrarEvento(
                1,
                "ASUS-7K4P2M",
                "alessandro@asustech.bo",
                "MODIFICAR_ARTICULO",
                "CATALOGO",
                "Actualización de precio de venta y stock mínimo para 'Monitor ASUS TUF Gaming 27\"'.",
                "192.168.1.45",
                "EXITO",
                "MEDIA",
                "{\"articuloId\":204,\"sku\":\"ASUS-TUF-VG279\",\"precioVenta\":2200.00,\"stockMinimo\":5}"
        );

        // 4. Registrar evento de seguridad de usuarios
        bitacoraAuditoriaService.registrarEvento(
                1,
                "ASUS-7K4P2M",
                "alessandro@asustech.bo",
                "CREACION_USUARIO",
                "SEGURIDAD",
                "Creación de nuevo operador de caja 'valeria.mercado@asustech.bo' con rol CAJERO.",
                "192.168.1.45",
                "EXITO",
                "MEDIA",
                "{\"usuarioId\":32,\"rol\":\"CAJERO\",\"fullname\":\"Valeria Mercado\"}"
        );

        // 5. Verificar que la cadena criptográfica completa del tenant sea íntegra y válida
        Map<String, Object> integridad = bitacoraAuditoriaService.verificarIntegridadCadenaDetallada(1);
        assertNotNull(integridad);
        assertEquals(Boolean.TRUE, integridad.get("valida"), "La cadena criptográfica SHA-256 debe ser 100% válida");
        assertTrue((int) integridad.get("totalBloques") >= 4, "Debe tener al menos los bloques validados");
        System.out.println(">>> RESULTADO VERIFICACIÓN CADENA CRIPTOGRÁFICA TENANT: " + integridad);
    }
}
