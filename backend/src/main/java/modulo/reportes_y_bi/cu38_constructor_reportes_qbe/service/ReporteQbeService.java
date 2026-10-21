package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.Agregacion;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.CatalogoEntidadesQbe;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.CatalogoEntidadesQbe.Campo;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.CatalogoEntidadesQbe.Entidad;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.Granularidad;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.OperadorFiltro;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.TipoCampo;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.CatalogoQbeResponse;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.Columna;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.Filtro;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.OperadorLogico;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.DefinicionQbe.Orden;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.PlantillaQbeRequest;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.PlantillaQbeResponse;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.ResultadoQbeResponse;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.entity.ReporteQbe;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.repository.ConsultaQbeRepository;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.repository.ReporteQbeRepository;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.service.ConstructorConsultaQbe.ConsultaSql;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.service.ExportadorReporteQbe.Encabezado;
import modulo.seguridad_y_auditoria.cu01_gestionar_acceso_al_sistema.security.ApplicationUserPrincipal;
import modulo.seguridad_y_auditoria.cu03_gestionar_roles_y_permisos.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CU38 - Constructor de reportes QBE: vista previa paginada, exportación a PDF/XLSX y
 * gestión de plantillas en {@code REPORTES_QBE} (HU38.1 a HU38.3).
 */
@Service
@RequiredArgsConstructor
public class ReporteQbeService {

    public enum Formato { PDF, XLSX }

    public record Archivo(byte[] contenido, String nombre, MediaType tipo) {}

    /** Lo que se guarda en FILTROS_APLICADOS: todo lo que no cabe en las columnas de la tabla. */
    record DefinicionGuardada(
            int version,
            String descripcion,
            List<Columna> columnas,
            List<Filtro> filtros,
            OperadorLogico operadorLogico,
            List<Orden> orden) {}

    static final int TAMANIO_MAXIMO = 200;
    static final int FILAS_EXPORTACION = 5000;
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    /** Filtro en el formato antiguo de los datos semilla: {@code ESTADO = EMITIDA}. */
    private static final Pattern FILTRO_ANTIGUO =
            Pattern.compile("^\\s*([A-Za-z_]+)\\s*(>=|<=|<>|!=|=|>|<)\\s*'?(.*?)'?\\s*$");

    private final ConstructorConsultaQbe constructor;
    private final ConsultaQbeRepository consultas;
    private final ReporteQbeRepository plantillas;
    private final ExportadorReporteQbe exportador;
    private final ObjectMapper json;

    public CatalogoQbeResponse catalogo() {
        return new CatalogoQbeResponse(
                CatalogoEntidadesQbe.todas().stream()
                        .map(e -> new CatalogoQbeResponse.EntidadDto(e.codigo(), e.etiqueta(), e.modulo(),
                                e.campos().stream()
                                        .map(c -> new CatalogoQbeResponse.CampoDto(c.clave(), c.etiqueta(), c.tipo()))
                                        .toList()))
                        .toList(),
                Arrays.stream(OperadorFiltro.values())
                        .map(o -> new CatalogoQbeResponse.OperadorDto(o.name(), o.etiqueta(), o.valores(),
                                Arrays.stream(TipoCampo.values()).filter(o::admite).toList()))
                        .toList(),
                Arrays.stream(Agregacion.values())
                        .map(a -> new CatalogoQbeResponse.AgregacionDto(a.name(), a.etiqueta(),
                                Arrays.stream(TipoCampo.values()).filter(a::admite).toList()))
                        .toList(),
                Arrays.stream(Granularidad.values())
                        .map(g -> new CatalogoQbeResponse.OpcionDto(g.name(), g.etiqueta()))
                        .toList());
    }

    /** HU38.3: ejecuta la consulta construida y devuelve una página del resultado. */
    @Transactional(readOnly = true)
    public ResultadoQbeResponse vistaPrevia(DefinicionQbe definicion, int pagina, int tamanio) {
        int tam = Math.min(Math.max(tamanio, 1), TAMANIO_MAXIMO);
        int pag = Math.max(pagina, 0);
        ConsultaSql consulta = constructor.construir(definicion, LocalDate.now());
        long total = consultas.contar(consulta);
        List<Map<String, Object>> filas = consultas.ejecutar(consulta, tam, (long) pag * tam);
        return new ResultadoQbeResponse(consulta.columnas(), filas, total, pag, tam,
                (int) Math.ceil(total / (double) tam), consulta.descripcionFiltros());
    }

    @Transactional(readOnly = true)
    public Archivo exportar(DefinicionQbe definicion, String nombre, Formato formato) {
        return generarArchivo(definicion, nombre, null, formato);
    }

    @Transactional(readOnly = true)
    public Archivo exportarPlantilla(Long id, Formato formato) {
        ReporteQbe r = buscar(id);
        DefinicionGuardada guardada = leer(r, new ArrayList<>());
        return generarArchivo(definicion(r, guardada), r.getNombre(), guardada.descripcion(), formato);
    }

    @Transactional(readOnly = true)
    public List<PlantillaQbeResponse> listar() {
        List<ReporteQbe> todas = plantillas.findAllByOrderByFechaCreacionDescIdDesc();
        Map<Long, String> autores = consultas.correos(todas.stream().map(ReporteQbe::getUsuarioId).distinct().toList());
        return todas.stream().map(r -> respuesta(r, autores.get(r.getUsuarioId()))).toList();
    }

    @Transactional(readOnly = true)
    public PlantillaQbeResponse obtener(Long id) {
        ReporteQbe r = buscar(id);
        return respuesta(r, consultas.correos(List.of(r.getUsuarioId())).get(r.getUsuarioId()));
    }

    /** HU38.3: guarda la definición como plantilla reutilizable. */
    @Transactional
    public PlantillaQbeResponse crear(PlantillaQbeRequest request, Authentication autenticacion) {
        ReporteQbe r = new ReporteQbe();
        r.setUsuarioId(usuarioActual(autenticacion));
        r.setFechaCreacion(LocalDateTime.now());
        aplicar(r, request);
        return obtener(plantillas.save(r).getId());
    }

    @Transactional
    public PlantillaQbeResponse actualizar(Long id, PlantillaQbeRequest request) {
        ReporteQbe r = buscar(id);
        aplicar(r, request);
        return obtener(plantillas.save(r).getId());
    }

    @Transactional
    public void eliminar(Long id) {
        plantillas.delete(buscar(id));
    }

    private void aplicar(ReporteQbe r, PlantillaQbeRequest request) {
        DefinicionQbe d = request.definicion();
        // Se construye la consulta para rechazar una definición inválida antes de guardarla.
        Entidad entidad = constructor.construir(d, LocalDate.now()).entidad();
        r.setNombre(request.nombre().trim());
        r.setModulo(entidad.modulo());
        r.setTablaBase(entidad.codigo());
        r.setCamposSeleccionados(String.join(",", d.columnas().stream()
                .map(c -> c.campo().trim().toLowerCase(Locale.ROOT)).toList()));
        String descripcion = request.descripcion() == null || request.descripcion().isBlank()
                ? null : request.descripcion().trim();
        try {
            r.setFiltrosAplicados(json.writeValueAsString(new DefinicionGuardada(
                    1, descripcion, d.columnas(), d.filtros() == null ? List.of() : d.filtros(),
                    d.operadorLogico() == null ? OperadorLogico.AND : d.operadorLogico(),
                    d.orden() == null ? List.of() : d.orden())));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la definición del reporte", e);
        }
    }

    private Archivo generarArchivo(DefinicionQbe definicion, String nombre, String descripcion, Formato formato) {
        ConsultaSql consulta = constructor.construir(definicion, LocalDate.now());
        long total = consultas.contar(consulta);
        List<Map<String, Object>> filas = consultas.ejecutar(consulta, FILAS_EXPORTACION, 0);

        String titulo = nombre == null || nombre.isBlank() ? "Reporte de " + consulta.entidad().etiqueta() : nombre.trim();
        List<String> lineas = new ArrayList<>();
        if (descripcion != null && !descripcion.isBlank()) lineas.add(descripcion);
        lineas.add("Entidad: " + consulta.entidad().etiqueta() + " · " + consulta.descripcionFiltros());
        lineas.add("Generado el " + LocalDateTime.now().format(FECHA_HORA) + " · " + total + " filas"
                + (total > FILAS_EXPORTACION ? " (se exportan las primeras " + FILAS_EXPORTACION + ")" : ""));
        Encabezado encabezado = new Encabezado(titulo, lineas);

        String archivo = slug(titulo) + "-" + LocalDate.now();
        return formato == Formato.PDF
                ? new Archivo(exportador.pdf(encabezado, consulta.columnas(), filas), archivo + ".pdf", MediaType.APPLICATION_PDF)
                : new Archivo(exportador.xlsx(encabezado, consulta.columnas(), filas), archivo + ".xlsx", XLSX);
    }

    private PlantillaQbeResponse respuesta(ReporteQbe r, String autor) {
        List<String> advertencias = new ArrayList<>();
        DefinicionGuardada guardada = leer(r, advertencias);
        Optional<Entidad> entidad = CatalogoEntidadesQbe.buscar(r.getTablaBase());
        return new PlantillaQbeResponse(r.getId(), r.getNombre(), guardada.descripcion(), r.getModulo(),
                entidad.map(Entidad::codigo).orElse(r.getTablaBase()),
                entidad.map(Entidad::etiqueta).orElse(r.getTablaBase()),
                autor, r.getFechaCreacion(), definicion(r, guardada), advertencias);
    }

    private static DefinicionQbe definicion(ReporteQbe r, DefinicionGuardada g) {
        String entidad = CatalogoEntidadesQbe.buscar(r.getTablaBase()).map(Entidad::codigo).orElse(r.getTablaBase());
        return new DefinicionQbe(entidad, g.columnas(), g.filtros(), g.operadorLogico(), g.orden());
    }

    /**
     * Lee la definición guardada. Las plantillas creadas por esta pantalla tienen JSON; las de
     * los datos semilla tienen campos en mayúsculas y filtros como {@code ESTADO = EMITIDA}, y
     * se convierten descartando (con advertencia) lo que ya no existe en el catálogo.
     */
    private DefinicionGuardada leer(ReporteQbe r, List<String> advertencias) {
        String filtros = r.getFiltrosAplicados() == null ? "" : r.getFiltrosAplicados().trim();
        if (filtros.startsWith("{")) {
            try {
                DefinicionGuardada g = json.readValue(filtros, DefinicionGuardada.class);
                return new DefinicionGuardada(g.version(), g.descripcion(),
                        g.columnas() == null ? columnasAntiguas(r, advertencias) : g.columnas(),
                        g.filtros() == null ? List.of() : g.filtros(),
                        g.operadorLogico() == null ? OperadorLogico.AND : g.operadorLogico(),
                        g.orden() == null ? List.of() : g.orden());
            } catch (JsonProcessingException e) {
                advertencias.add("La definición guardada está dañada; se cargaron solo los campos.");
                return new DefinicionGuardada(1, null, columnasAntiguas(r, advertencias), List.of(), OperadorLogico.AND, List.of());
            }
        }
        return new DefinicionGuardada(1, null, columnasAntiguas(r, advertencias),
                filtrosAntiguos(r, filtros, advertencias), OperadorLogico.AND, List.of());
    }

    private static List<Columna> columnasAntiguas(ReporteQbe r, List<String> advertencias) {
        Optional<Entidad> entidad = CatalogoEntidadesQbe.buscar(r.getTablaBase());
        List<Columna> columnas = new ArrayList<>();
        if (r.getCamposSeleccionados() == null) return columnas;
        for (String clave : r.getCamposSeleccionados().split(",")) {
            if (clave.isBlank()) continue;
            Optional<Campo> campo = entidad.flatMap(e -> campoAntiguo(e, clave));
            if (campo.isPresent()) {
                columnas.add(new Columna(campo.get().clave(), null, null));
            } else {
                advertencias.add("El campo '" + clave.trim() + "' ya no está disponible y se omitió.");
            }
        }
        return columnas;
    }

    private static List<Filtro> filtrosAntiguos(ReporteQbe r, String texto, List<String> advertencias) {
        Optional<Entidad> entidad = CatalogoEntidadesQbe.buscar(r.getTablaBase());
        List<Filtro> filtros = new ArrayList<>();
        if (texto.isBlank()) return filtros;
        for (String parte : texto.split("(?i)\\s+AND\\s+")) {
            Matcher m = FILTRO_ANTIGUO.matcher(parte);
            Optional<Campo> campo = m.matches() ? entidad.flatMap(e -> campoAntiguo(e, m.group(1))) : Optional.empty();
            if (campo.isEmpty()) {
                advertencias.add("No se pudo interpretar el filtro '" + parte.trim() + "' y se omitió.");
                continue;
            }
            OperadorFiltro op = switch (m.group(2)) {
                case "=" -> OperadorFiltro.IGUAL;
                case "<>", "!=" -> OperadorFiltro.DISTINTO;
                case ">" -> OperadorFiltro.MAYOR;
                case ">=" -> OperadorFiltro.MAYOR_IGUAL;
                case "<" -> OperadorFiltro.MENOR;
                default -> OperadorFiltro.MENOR_IGUAL;
            };
            if (!op.admite(campo.get().tipo())) {
                advertencias.add("El filtro '" + parte.trim() + "' no aplica al tipo del campo y se omitió.");
                continue;
            }
            filtros.add(new Filtro(campo.get().clave(), op, m.group(3), null));
        }
        return filtros;
    }

    /** Busca por clave y, si no existe, sin el sufijo "_id" (CAJERO_ID → cajero). */
    private static Optional<Campo> campoAntiguo(Entidad entidad, String clave) {
        String k = clave.trim().toLowerCase(Locale.ROOT);
        Optional<Campo> campo = entidad.campo(k);
        return campo.isPresent() || !k.endsWith("_id") ? campo : entidad.campo(k.substring(0, k.length() - 3));
    }

    private ReporteQbe buscar(Long id) {
        return plantillas.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "La plantilla de reporte " + id + " no existe."));
    }

    /** Id de la tabla {@code usuario} de quien guarda la plantilla (FK de REPORTES_QBE). */
    private Long usuarioActual(Authentication autenticacion) {
        Long usuarioId = null;
        Object principal = autenticacion == null ? null : autenticacion.getPrincipal();
        if (principal instanceof ApplicationUserPrincipal app && app.identidad() != null) {
            usuarioId = app.identidad().usuarioId();
        } else if (principal instanceof UsuarioPrincipal identidad) {
            usuarioId = identidad.usuarioId();
        }
        if (usuarioId != null && consultas.existeUsuarioActivo(usuarioId)) return usuarioId;
        return consultas.usuarioActivoPorEmail(autenticacion == null ? null : autenticacion.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "No se pudo identificar al usuario que guarda la plantilla."));
    }

    private static String slug(String texto) {
        String s = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return s.isEmpty() ? "reporte" : s.length() > 60 ? s.substring(0, 60) : s;
    }
}
