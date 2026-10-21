package modulo.compras_y_proveedores.cu21_gestionar_proveedores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import modulo.compras_y_proveedores.compartido.service.AvisosCompras;
import modulo.compras_y_proveedores.compartido.service.Solicitante;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.HistorialProveedorResponse;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.ProveedorRequest;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.dto.ProveedorResponse;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.CondicionPago;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.Proveedor;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.mapper.ProveedorMapper;
import modulo.compras_y_proveedores.cu21_gestionar_proveedores.repository.ProveedorRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

class ProveedorServiceTest {

    private final ProveedorRepository proveedores = mock(ProveedorRepository.class);
    private final AvisosCompras avisos = mock(AvisosCompras.class);
    private final Solicitante admin = new Solicitante(1L, "admin", "127.0.0.1");

    private ProveedorService service;
    private Proveedor existente;

    @BeforeEach
    void preparar() {
        service = new ProveedorService(proveedores, new ProveedorMapper(), avisos);

        when(proveedores.saveAndFlush(any(Proveedor.class))).thenAnswer(inv -> {
            Proveedor p = inv.getArgument(0);
            if (p.getId() == null) p.setId(40);
            return p;
        });

        existente = new Proveedor();
        existente.setId(5);
        existente.setRazonSocial("Importadora Sur");
        existente.setNitCi("998877");
        existente.setCondicionPago(CondicionPago.CREDITO);
        existente.setDiasCredito(30);
        existente.setActivo(true);
        when(proveedores.findById(5)).thenReturn(Optional.of(existente));
        when(proveedores.bloquear(5)).thenReturn(Optional.of(existente));
        when(proveedores.saldoPendiente(5)).thenReturn(BigDecimal.ZERO);
    }

    @Test
    void registraUnProveedorActivoYLoDejaEnLaBitacora() {
        ProveedorResponse r = service.registrar(peticion("1234567", CondicionPago.CREDITO, 30), admin);

        assertThat(r.id()).isEqualTo(40);
        assertThat(r.activo()).isTrue();
        assertThat(r.condicionPago()).isEqualTo("CREDITO");
        assertThat(r.diasCredito()).isEqualTo(30);
        assertThat(r.email()).isEqualTo("ventas@norte.bo");
        verify(avisos).registrar("PROVEEDOR_CREADO", admin, "proveedores", 40);
    }

    @Test
    void rechazaUnNitRepetidoSinGuardarNada() {
        when(proveedores.existsByNitCi("1234567")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(peticion("1234567", CondicionPago.CONTADO, 0), admin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("NIT/CI 1234567");
        verify(proveedores, never()).saveAndFlush(any());
    }

    @Test
    void creditoSinDiasEsInvalido() {
        assertThatThrownBy(() -> service.registrar(peticion("1234567", CondicionPago.CREDITO, 0), admin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
        verify(proveedores, never()).saveAndFlush(any());
    }

    @Test
    void contadoQuedaSiempreConCeroDias() {
        assertThat(service.registrar(peticion("1234567", CondicionPago.CONTADO, 15), admin).diasCredito()).isZero();
    }

    @Test
    void modificarNoPermiteTomarElNitDeOtro() {
        when(proveedores.existsByNitCiAndIdNot("1234567", 5)).thenReturn(true);

        assertThatThrownBy(() -> service.modificar(5, peticion("1234567", CondicionPago.CONTADO, 0), admin))
                .hasMessageContaining("otro proveedor");
    }

    @Test
    void noSeDesactivaConOrdenesAbiertas() {
        when(proveedores.contarOrdenesAbiertas(5)).thenReturn(2L);

        assertThatThrownBy(() -> service.desactivar(5, "Ya no trabaja con nosotros", admin))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("2 orden(es)");
        assertThat(existente.getActivo()).isTrue();
    }

    @Test
    void noSeDesactivaConSaldoPorPagar() {
        when(proveedores.saldoPendiente(5)).thenReturn(new BigDecimal("150.00"));

        assertThatThrownBy(() -> service.desactivar(5, "Cierre", admin))
                .hasMessageContaining("Bs 150.00");
        assertThat(existente.getActivo()).isTrue();
    }

    @Test
    void desactivaAvisandoElMotivoYLuegoReactiva() {
        ProveedorResponse r = service.desactivar(5, "Cerró operaciones", admin);

        assertThat(r.activo()).isFalse();
        verify(avisos).registrar("PROVEEDOR_DESACTIVADO", admin, "proveedores", 5);
        verify(avisos).avisarAdministradores(eq("Proveedor desactivado"), anyString(), isNull());

        assertThat(service.reactivar(5, admin).activo()).isTrue();
        verify(avisos).registrar("PROVEEDOR_REACTIVADO", admin, "proveedores", 5);
    }

    @Test
    void historialJuntaOrdenesComprasYSaldo() {
        when(proveedores.ultimasOrdenes(5)).thenReturn(List.<Object[]>of(
                new Object[]{1, "OC-2026-0001", Date.valueOf("2026-10-01"), new BigDecimal("500.00"), "APROBADA"}));
        when(proveedores.ultimasCompras(5)).thenReturn(List.of());
        when(proveedores.saldoPendiente(5)).thenReturn(new BigDecimal("80.00"));
        when(proveedores.contarOrdenesAbiertas(5)).thenReturn(1L);

        HistorialProveedorResponse h = service.historial(5);

        assertThat(h.ordenes()).hasSize(1);
        assertThat(h.ordenes().get(0).fecha()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(h.saldoPendiente()).isEqualByComparingTo("80.00");
        assertThat(h.ordenesAbiertas()).isEqualTo(1L);
    }

    @Test
    void leeCondicionesDePagoEscritasAManoEnLaBase() {
        modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.CondicionPagoConverter c =
                new modulo.compras_y_proveedores.cu21_gestionar_proveedores.entity.CondicionPagoConverter();
        assertThat(c.convertToEntityAttribute(" credito ")).isEqualTo(CondicionPago.CREDITO);
        assertThat(c.convertToEntityAttribute("Crédito")).isEqualTo(CondicionPago.CREDITO);
        assertThat(c.convertToEntityAttribute("contado")).isEqualTo(CondicionPago.CONTADO);
        assertThat(c.convertToEntityAttribute(null)).isNull();
        assertThat(c.convertToDatabaseColumn(CondicionPago.CREDITO)).isEqualTo("CREDITO");
    }

    private static ProveedorRequest peticion(String nit, CondicionPago condicion, Integer dias) {
        return new ProveedorRequest("Distribuidora Norte", nit, "Ana Pérez", "70012345", "Ventas@Norte.bo",
                "Av. Banzer 123", "Santa Cruz", condicion, dias);
    }
}
