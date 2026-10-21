package modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.service;

import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.entity.BitacoraLog;
import modulo.seguridad_y_auditoria.cu02_gestionar_usuarios.repository.BitacoraLogRepository;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto.AparienciaDto;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto.LogoArchivo;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto.Peticiones;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.entity.ConfiguracionApariencia;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.mapper.AparienciaMapper;
import modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.repository.ConfiguracionAparienciaRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * CU-06: la apariencia que el administrador define para todos los usuarios de su empresa.
 *
 * <p>Cada empresa tiene su propia base, así que no hace falta filtrar por empresa: la única
 * fila de {@code configuracion_empresa} de la base de la petición es la de esa empresa.
 */
@Service
public class ServicioApariencia {

    public static final String APARIENCIA_ACTUALIZADA = "APARIENCIA_ACTUALIZADA";
    public static final String APARIENCIA_LOGO_SUBIDO = "APARIENCIA_LOGO_SUBIDO";
    public static final String APARIENCIA_LOGO_ELIMINADO = "APARIENCIA_LOGO_ELIMINADO";
    public static final String APARIENCIA_RESTABLECIDA = "APARIENCIA_RESTABLECIDA";

    /** Tamaño máximo del logo: 512 KB. */
    public static final long TAMANO_MAXIMO_LOGO = 512L * 1024;

    private static final Logger log = LoggerFactory.getLogger(ServicioApariencia.class);
    private static final String ENTIDAD = "configuracion_empresa";

    /** Lo que vuelve peligroso a un SVG: código que se ejecuta o recursos externos. */
    private static final Pattern SVG_PELIGROSO = Pattern.compile(
            "<script|<foreignobject|<!entity|<iframe|<embed|<object|javascript:|\\son[a-z]+\\s*=",
            Pattern.CASE_INSENSITIVE);

    private final ConfiguracionAparienciaRepositorio repositorio;
    private final BitacoraLogRepository bitacoraLogRepository;
    private final AparienciaMapper mapper;

    public ServicioApariencia(ConfiguracionAparienciaRepositorio repositorio,
                              BitacoraLogRepository bitacoraLogRepository,
                              AparienciaMapper mapper) {
        this.repositorio = repositorio;
        this.bitacoraLogRepository = bitacoraLogRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public AparienciaDto obtener() {
        return repositorio.findFirstByOrderByIdAsc()
                .map(mapper::toDto)
                .orElseGet(mapper::porDefecto);
    }

    @Transactional
    public AparienciaDto actualizar(Peticiones.Actualizar peticion, String actor, String ip) {
        ConfiguracionApariencia c = configuracionParaEscribir();
        c.setTema(peticion.tema());
        c.setColorPrimario(peticion.colorPrimario().toUpperCase(Locale.ROOT));
        c.setColorSecundario(peticion.colorSecundario().toUpperCase(Locale.ROOT));
        c.setColorAcento(peticion.colorAcento().toUpperCase(Locale.ROOT));
        c.setTamanoFuente(peticion.tamanoFuente());
        c.setTipografia(peticion.tipografia());
        c.setBordes(peticion.bordes());
        c.setEncabezadoFactura(textoOpcional(peticion.encabezadoFactura()));
        c.setPieFactura(textoOpcional(peticion.pieFactura()));
        c.registrarCambio(actor);

        ConfiguracionApariencia guardada = repositorio.save(c);
        registrarBitacora(APARIENCIA_ACTUALIZADA, actor, guardada, ip);
        return mapper.toDto(guardada);
    }

    @Transactional
    public AparienciaDto subirLogo(MultipartFile archivo, String actor, String ip) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleccione un archivo de imagen para el logo");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_LOGO) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "El logo no puede superar los 512 KB");
        }

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo del logo", e);
        }
        String tipo = detectarTipo(contenido);

        ConfiguracionApariencia c = configuracionParaEscribir();
        c.cambiarLogo(contenido, tipo);
        c.registrarCambio(actor);

        ConfiguracionApariencia guardada = repositorio.save(c);
        registrarBitacora(APARIENCIA_LOGO_SUBIDO, actor, guardada, ip);
        return mapper.toDto(guardada);
    }

    @Transactional(readOnly = true)
    public LogoArchivo logo() {
        return repositorio.findFirstByOrderByIdAsc()
                .filter(ConfiguracionApariencia::tieneLogo)
                .map(c -> new LogoArchivo(c.getLogo(), c.getLogoTipo(), c.getLogoVersion()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La empresa no tiene un logo personalizado"));
    }

    @Transactional
    public AparienciaDto eliminarLogo(String actor, String ip) {
        ConfiguracionApariencia c = repositorio.findFirstByOrderByIdAsc().orElse(null);
        if (c == null || !c.tieneLogo()) {
            return mapper.toDto(c);
        }
        c.quitarLogo();
        c.registrarCambio(actor);

        ConfiguracionApariencia guardada = repositorio.save(c);
        registrarBitacora(APARIENCIA_LOGO_ELIMINADO, actor, guardada, ip);
        return mapper.toDto(guardada);
    }

    /** Vuelve a la marca NexoERP: colores, tema, preferencias y logo. Conserva la factura. */
    @Transactional
    public AparienciaDto restablecer(String actor, String ip) {
        ConfiguracionApariencia c = configuracionParaEscribir();
        mapper.aplicarPorDefecto(c);
        if (c.tieneLogo()) {
            c.quitarLogo();
        }
        c.registrarCambio(actor);

        ConfiguracionApariencia guardada = repositorio.save(c);
        registrarBitacora(APARIENCIA_RESTABLECIDA, actor, guardada, ip);
        return mapper.toDto(guardada);
    }

    /**
     * Identifica la imagen por su contenido y no por el nombre ni el Content-Type, que los
     * elige quien sube el archivo.
     */
    String detectarTipo(byte[] b) {
        if (empiezaCon(b, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return "image/png";
        if (empiezaCon(b, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
        if (b.length >= 12 && empiezaCon(b, 'R', 'I', 'F', 'F')
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return "image/webp";

        String texto = new String(b, StandardCharsets.UTF_8).trim();
        if (texto.startsWith("﻿")) texto = texto.substring(1);
        String inicio = texto.toLowerCase(Locale.ROOT);
        if ((inicio.startsWith("<?xml") || inicio.startsWith("<svg")) && inicio.contains("<svg")) {
            if (SVG_PELIGROSO.matcher(texto).find()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El SVG contiene scripts o recursos externos y no se puede usar como logo");
            }
            return "image/svg+xml";
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El logo debe ser una imagen PNG, JPG, WEBP o SVG");
    }

    private static boolean empiezaCon(byte[] b, int... firma) {
        if (b.length < firma.length) return false;
        for (int i = 0; i < firma.length; i++) {
            if ((b[i] & 0xFF) != firma[i]) return false;
        }
        return true;
    }

    private ConfiguracionApariencia configuracionParaEscribir() {
        return repositorio.findFirstByOrderByIdAsc().orElseGet(() -> {
            ConfiguracionApariencia nueva = new ConfiguracionApariencia();
            mapper.aplicarPorDefecto(nueva);
            return nueva;
        });
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private void registrarBitacora(String accion, String actor, ConfiguracionApariencia c, String ip) {
        try {
            bitacoraLogRepository.save(new BitacoraLog(accion, actor, ENTIDAD, c.getId().longValue(), ip));
        } catch (Exception e) {
            log.warn("No se pudo registrar en la bitácora la acción {}: {}", accion, e.getMessage());
        }
    }
}
