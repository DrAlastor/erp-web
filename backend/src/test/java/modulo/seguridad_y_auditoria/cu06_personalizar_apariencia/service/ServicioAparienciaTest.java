package modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.service;

import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto.AparienciaDto;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto.Peticiones;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.entity.ConfiguracionApariencia;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.mapper.AparienciaMapper;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.repository.ConfiguracionAparienciaRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServicioAparienciaTest {

    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final String ACTOR = "admin@empresa.bo";
    private static final String IP = "127.0.0.1";

    private ConfiguracionAparienciaRepositorio repositorio;
    private BitacoraLogRepository bitacora;
    private ServicioApariencia servicio;

    @BeforeEach
    void preparar() {
        repositorio = mock(ConfiguracionAparienciaRepositorio.class);
        bitacora = mock(BitacoraLogRepository.class);
        servicio = new ServicioApariencia(repositorio, bitacora, new AparienciaMapper());
        when(repositorio.save(any(ConfiguracionApariencia.class))).thenAnswer(inv -> {
            ConfiguracionApariencia c = inv.getArgument(0);
            if (c.getId() == null) ReflectionTestUtils.setField(c, "id", 1);
            return c;
        });
    }

    private static Peticiones.Actualizar peticion() {
        return new Peticiones.Actualizar("OSCURO", "#e11d48", "#111827", "#f59e0b",
                "GRANDE", "POPPINS", "REDONDEADOS", "  Mi Empresa S.R.L. ", "Gracias por su compra");
    }

    private static ConfiguracionApariencia existente() {
        ConfiguracionApariencia c = new ConfiguracionApariencia();
        ReflectionTestUtils.setField(c, "id", 7);
        return c;
    }

    @Test
    @DisplayName("sin fila en la base devuelve la marca NexoERP")
    void sinFilaDevuelvePorDefecto() {
        when(repositorio.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        AparienciaDto dto = servicio.obtener();

        assertThat(dto.tema()).isEqualTo("CLARO");
        assertThat(dto.colorPrimario()).isEqualTo("#007AFF");
        assertThat(dto.tipografia()).isEqualTo("INTER");
        assertThat(dto.tieneLogo()).isFalse();
    }

    @Test
    @DisplayName("las columnas en null o con valores inválidos se completan con los valores por defecto")
    void columnasNulasUsanPorDefecto() {
        ConfiguracionApariencia sembrada = existente();
        sembrada.setColorPrimario("rojo");
        sembrada.setEncabezadoFactura("ASUS BOLIVIA");
        when(repositorio.findFirstByOrderByIdAsc()).thenReturn(Optional.of(sembrada));

        AparienciaDto dto = servicio.obtener();

        assertThat(dto.tema()).isEqualTo("CLARO");
        assertThat(dto.colorPrimario()).isEqualTo("#007AFF");
        assertThat(dto.encabezadoFactura()).isEqualTo("ASUS BOLIVIA");
    }

    @Test
    @DisplayName("si no existe la fila, guardar la crea con los datos enviados")
    void actualizarCreaLaFila() {
        when(repositorio.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

        AparienciaDto dto = servicio.actualizar(peticion(), ACTOR, IP);

        assertThat(dto.tema()).isEqualTo("OSCURO");
        assertThat(dto.colorPrimario()).isEqualTo("#E11D48");
        assertThat(dto.tamanoFuente()).isEqualTo("GRANDE");
        assertThat(dto.encabezadoFactura()).isEqualTo("Mi Empresa S.R.L.");
        assertThat(dto.modificadoPor()).isEqualTo(ACTOR);
        assertThat(dto.fechaModificacion()).isNotNull();
    }

    @Test
    @DisplayName("si la fila existe, guardar la actualiza en lugar de crear otra")
    void actualizarReutilizaLaFila() {
        ConfiguracionApariencia fila = existente();
        when(repositorio.findFirstByOrderByIdAsc()).thenReturn(Optional.of(fila));

        servicio.actualizar(peticion(), ACTOR, IP);

        ArgumentCaptor<ConfiguracionApariencia> guardada = ArgumentCaptor.forClass(ConfiguracionApariencia.class);
        verify(repositorio).save(guardada.capture());
        assertThat(guardada.getValue()).isSameAs(fila);
        assertThat(fila.getColorAcento()).isEqualTo("#F59E0B");
    }

    @Test
    @DisplayName("cada cambio queda en la bitácora")
    void registraEnBitacora() {
        when(repositorio.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existente()));

        servicio.actualizar(peticion(), ACTOR, IP);

        ArgumentCaptor<BitacoraLog> evento = ArgumentCaptor.forClass(BitacoraLog.class);
        verify(bitacora).save(evento.capture());
        assertThat(evento.getValue().getAccion()).isEqualTo(ServicioApariencia.APARIENCIA_ACTUALIZADA);
        assertThat(evento.getValue().getEntidad()).isEqualTo("configuracion_empresa");
        assertThat(evento.getValue().getEntidadId()).isEqualTo(7L);
    }

    @Test
    @DisplayName("si la bitácora falla, el cambio de apariencia se guarda igual")
    void bitacoraNoBloquea() {
        when(repositorio.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existente()));
        when(bitacora.save(any())).thenThrow(new RuntimeException("tabla no disponible"));

        AparienciaDto dto = servicio.actualizar(peticion(), ACTOR, IP);

        assertThat(dto.tema()).isEqualTo("OSCURO");
    }

    @Test
    @DisplayName("subir un PNG lo guarda e incrementa la versión del logo")
    void subirLogoPng() {
        ConfiguracionApariencia fila = existente();
        when(repositorio.findFirstByOrderByIdAsc()).thenReturn(Optional.of(fila));

        AparienciaDto dto = servicio.subirLogo(new MockMultipartFile("archivo", "logo.png", "image/png", PNG), ACTOR, IP);

        assertThat(dto.tieneLogo()).isTrue();
        assertThat(dto.logoVersion()).isEqualTo(1);
        assertThat(fila.getLogoTipo()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("un archivo que no es imagen se rechaza aunque diga ser PNG")
    void rechazaArchivoDisfrazado() {
        when(repositorio.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existente()));
        MockMultipartFile exe = new MockMultipartFile("archivo", "logo.png", "image/png", new byte[]{'M', 'Z', 0, 0});

        assertThatThrownBy(() -> servicio.subirLogo(exe, ACTOR, IP))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("un logo de más de 512 KB se rechaza")
    void rechazaLogoGrande() {
        byte[] grande = new byte[(int) ServicioApariencia.TAMANO_MAXIMO_LOGO + 1];
        System.arraycopy(PNG, 0, grande, 0, PNG.length);

        assertThatThrownBy(() -> servicio.subirLogo(new MockMultipartFile("archivo", "logo.png", "image/png", grande), ACTOR, IP))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE));
    }

    @Test
    @DisplayName("un SVG con scripts se rechaza")
    void rechazaSvgConScript() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"></svg>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> servicio.detectarTipo(svg))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("scripts");
    }

    @Test
    @DisplayName("un SVG limpio se acepta")
    void aceptaSvgLimpio() {
        byte[] svg = "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"><rect width=\"10\" height=\"10\"/></svg>"
                .getBytes(StandardCharsets.UTF_8);

        assertThat(servicio.detectarTipo(svg)).isEqualTo("image/svg+xml");
    }

    @Test
    @DisplayName("restablecer vuelve a la marca NexoERP, quita el logo y conserva la factura")
    void restablecerConservaFactura() {
        ConfiguracionApariencia fila = existente();
        fila.setColorPrimario("#E11D48");
        fila.setTema("OSCURO");
        fila.setPieFactura("Gracias por su compra");
        fila.cambiarLogo(PNG, "image/png");
        when(repositorio.findFirstByOrderByIdAsc()).thenReturn(Optional.of(fila));

        AparienciaDto dto = servicio.restablecer(ACTOR, IP);

        assertThat(dto.colorPrimario()).isEqualTo("#007AFF");
        assertThat(dto.tema()).isEqualTo("CLARO");
        assertThat(dto.tieneLogo()).isFalse();
        assertThat(dto.logoVersion()).isEqualTo(2);
        assertThat(dto.pieFactura()).isEqualTo("Gracias por su compra");
    }

    @Test
    @DisplayName("pedir el logo cuando no hay uno responde 404")
    void logoInexistente() {
        when(repositorio.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existente()));

        assertThatThrownBy(() -> servicio.logo())
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
