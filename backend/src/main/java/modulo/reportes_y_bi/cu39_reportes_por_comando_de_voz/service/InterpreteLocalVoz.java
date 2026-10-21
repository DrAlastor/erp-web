package modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.service;

import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.AgrupacionVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.catalogo.MetricaVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.dto.ConsultaReporteVoz;
import modulo.reportes_y_bi.cu39_reportes_por_comando_de_voz.repository.ReporteVozRepository.Entidad;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intérprete de respaldo del CU39: traduce una frase en español a una
 * {@link ConsultaReporteVoz} con reglas de palabras clave. Se usa cuando el flujo de N8N no
 * está configurado o no responde, y aplica las mismas equivalencias que se esperan de N8N
 * ("lo que vendimos" → ventas, "el mes pasado" → mes calendario anterior, etc.).
 *
 * <p>No toca la base de datos: recibe las sucursales y productos ya leídos, así se puede
 * probar sin Spring.
 */
@Component
public class InterpreteLocalVoz {

    private static final String[] MESES = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

    private static final Map<String, String> NUMEROS = Map.ofEntries(
            Map.entry("dos", "2"), Map.entry("tres", "3"), Map.entry("cuatro", "4"),
            Map.entry("cinco", "5"), Map.entry("seis", "6"), Map.entry("siete", "7"),
            Map.entry("ocho", "8"), Map.entry("nueve", "9"), Map.entry("diez", "10"),
            Map.entry("once", "11"), Map.entry("doce", "12"), Map.entry("quince", "15"),
            Map.entry("veinte", "20"), Map.entry("treinta", "30"));

    /** Palabras que no sirven para reconocer una sucursal o un producto. */
    private static final Set<String> VACIAS = Set.of(
            "sucursal", "de", "del", "la", "las", "el", "los", "y", "en", "con", "para", "por",
            "calle", "av", "esq", "the", "asus");

    private static final Pattern ULTIMOS_N = Pattern.compile(
            " (?:ultimos|ultimas) (\\d{1,3}) (dias|semanas|meses|anos) |"
                    + " (\\d{1,3}) (?:ultimos|ultimas) (dias|semanas|meses|anos) ");
    private static final Pattern MES_NOMBRE = Pattern.compile(
            " (" + String.join("|", MESES) + ")(?: (?:de|del) (\\d{4}))? ");
    private static final Pattern ANIO = Pattern.compile(" (?:en|del|de|el) (?:ano )?(20\\d{2}) ");
    private static final Pattern LIMITE = Pattern.compile(
            " (?:top|los|las|primeros|primeras|mejores) (\\d{1,3}) (?!ultim|dia|semana|mes|ano)|"
                    + " (\\d{1,3}) (?:productos|articulos|clientes|proveedores|vendedores|mejores|primeros) ");

    /**
     * Interpreta la frase. Vacío si no se reconoce qué reporte se pide, que es lo que pasa
     * cuando la transcripción salió mal.
     */
    public Optional<ConsultaReporteVoz> interpretar(
            String frase, List<Entidad> sucursales, List<Entidad> productos, LocalDate hoy) {
        String t = normalizar(frase);
        MetricaVoz metrica = detectarMetrica(t);
        if (metrica == null) {
            return Optional.empty();
        }

        LocalDate[] rango = detectarRango(t, hoy);
        AgrupacionVoz agrupacion = detectarAgrupacion(t, rango);

        // "Ventas por producto" o "ventas de la laptop TUF" se responden desde el detalle.
        Long productoId = buscar(t, productos);
        if (metrica == MetricaVoz.VENTAS
                && (productoId != null || agrupacion == AgrupacionVoz.PRODUCTO
                    || agrupacion == AgrupacionVoz.CATEGORIA)) {
            metrica = MetricaVoz.PRODUCTOS_VENDIDOS;
        }
        if (!metrica.admiteProducto()) {
            productoId = null;
        }
        if (agrupacion == null && productoId != null) {
            if (metrica == MetricaVoz.PRODUCTOS_VENDIDOS) agrupacion = AgrupacionVoz.NINGUNA;
            if (metrica == MetricaVoz.STOCK) agrupacion = AgrupacionVoz.SUCURSAL;
        }
        if (agrupacion != null && !metrica.admite(agrupacion)) {
            agrupacion = null;
        }

        boolean criticos = metrica.admiteCriticos() && (metrica == MetricaVoz.STOCK
                ? tiene(t, "bajo minimo", "bajo el minimo", "debajo del minimo", "critico", "agotad", "reponer", "reposicion")
                : tiene(t, "vencid", "mora", "morosos", "atrasad"));

        return Optional.of(ConsultaReporteVoz.builder()
                .metrica(metrica)
                .agrupacion(agrupacion)
                .fechaInicio(metrica.usaFechas() && rango != null ? rango[0] : null)
                .fechaFin(metrica.usaFechas() && rango != null ? rango[1] : null)
                .sucursalId(buscar(t, sucursales))
                .productoId(productoId)
                .soloCriticos(criticos ? Boolean.TRUE : null)
                .limite(detectarLimite(t))
                .build());
    }

    /** Minúsculas, sin tildes ni signos, números en cifras y con un espacio a cada lado. */
    static String normalizar(String frase) {
        String sinTildes = Normalizer.normalize(frase == null ? "" : frase, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        StringBuilder sb = new StringBuilder(" ");
        for (String palabra : sinTildes.split(" ")) {
            if (!palabra.isEmpty()) {
                sb.append(NUMEROS.getOrDefault(palabra, palabra)).append(' ');
            }
        }
        return sb.toString();
    }

    private static MetricaVoz detectarMetrica(String t) {
        if (tiene(t, "por cobrar", "cxc", "cobranza", "cartera", "deuda", "nos deben", "morosos")) {
            return MetricaVoz.CUENTAS_POR_COBRAR;
        }
        if (tiene(t, "compra", "proveedor", "adquisicion")) {
            return MetricaVoz.COMPRAS;
        }
        if (tiene(t, "kardex", "movimiento", "entradas", "salidas")) {
            return MetricaVoz.MOVIMIENTOS_KARDEX;
        }
        if (tiene(t, "stock", "inventario", "existencia", "agotad", "reponer", "reposicion")) {
            return MetricaVoz.STOCK;
        }
        if (tiene(t, "mas vendid", "productos vendidos", "articulos vendidos", "que productos", "que articulos")) {
            return MetricaVoz.PRODUCTOS_VENDIDOS;
        }
        // "Top 5 clientes" o "mejores productos": un ranking de ventas; el sustantivo elige la agrupación.
        if (tiene(t, "venta", "vend", "factur", "ingreso", "recaud", "ganamos", "top", "ranking", "mejores")) {
            return MetricaVoz.VENTAS;
        }
        return null;
    }

    private static AgrupacionVoz detectarAgrupacion(String t, LocalDate[] rango) {
        if (tiene(t, "por dia", "diari", "cada dia")) return AgrupacionVoz.DIA;
        if (tiene(t, "por mes", "mensual", "cada mes")) return AgrupacionVoz.MES;
        if (tiene(t, "por sucursal", "cada sucursal")) return AgrupacionVoz.SUCURSAL;
        if (tiene(t, "por cliente", "cada cliente")) return AgrupacionVoz.CLIENTE;
        if (tiene(t, "por vendedor", "por cajero", "cada vendedor")) return AgrupacionVoz.VENDEDOR;
        if (tiene(t, "por producto", "por articulo", "cada producto")) return AgrupacionVoz.PRODUCTO;
        if (tiene(t, "por categoria", "cada categoria")) return AgrupacionVoz.CATEGORIA;
        if (tiene(t, "por proveedor", "cada proveedor")) return AgrupacionVoz.PROVEEDOR;
        if (tiene(t, "por tipo")) return AgrupacionVoz.TIPO;
        if (tiene(t, "por estado")) return AgrupacionVoz.ESTADO;

        if (tiene(t, "evolucion", "tendencia", "historico", "comportamiento")) {
            boolean corto = rango != null && ChronoUnit.DAYS.between(rango[0], rango[1]) <= 62;
            return corto ? AgrupacionVoz.DIA : AgrupacionVoz.MES;
        }
        // Un plural sin "por" también suele pedir una comparación: "mejores clientes".
        if (tiene(t, "sucursales")) return AgrupacionVoz.SUCURSAL;
        if (tiene(t, "clientes")) return AgrupacionVoz.CLIENTE;
        if (tiene(t, "vendedores", "cajeros")) return AgrupacionVoz.VENDEDOR;
        if (tiene(t, "categorias")) return AgrupacionVoz.CATEGORIA;
        if (tiene(t, "productos", "articulos")) return AgrupacionVoz.PRODUCTO;
        if (tiene(t, "proveedores")) return AgrupacionVoz.PROVEEDOR;
        return null;
    }

    /** Rango [inicio, fin] inclusivo, o null si la frase no menciona fechas. */
    static LocalDate[] detectarRango(String t, LocalDate hoy) {
        Matcher n = ULTIMOS_N.matcher(t);
        if (n.find()) {
            int cantidad = Integer.parseInt(n.group(1) != null ? n.group(1) : n.group(3));
            String unidad = n.group(2) != null ? n.group(2) : n.group(4);
            LocalDate inicio = switch (unidad) {
                case "dias" -> hoy.minusDays(cantidad - 1L);
                case "semanas" -> hoy.minusWeeks(cantidad).plusDays(1);
                case "meses" -> hoy.minusMonths(cantidad).plusDays(1);
                default -> hoy.minusYears(cantidad).plusDays(1);
            };
            return new LocalDate[]{inicio, hoy};
        }

        Matcher m = MES_NOMBRE.matcher(t);
        if (m.find()) {
            int mes = List.of(MESES).indexOf(m.group(1)) + 1;
            int anio = m.group(2) != null ? Integer.parseInt(m.group(2))
                    : (mes > hoy.getMonthValue() ? hoy.getYear() - 1 : hoy.getYear());
            LocalDate inicio = LocalDate.of(anio, mes, 1);
            return new LocalDate[]{inicio, inicio.withDayOfMonth(inicio.lengthOfMonth())};
        }

        if (tiene(t, "anteayer")) return dia(hoy.minusDays(2));
        if (tiene(t, "ayer")) return dia(hoy.minusDays(1));
        if (tiene(t, "hoy", "del dia", "en el dia")) return dia(hoy);

        if (tiene(t, "semana pasada", "ultima semana", "semana anterior")) {
            LocalDate lunes = hoy.with(DayOfWeek.MONDAY).minusWeeks(1);
            return new LocalDate[]{lunes, lunes.plusDays(6)};
        }
        if (tiene(t, "esta semana", "semana actual")) {
            return new LocalDate[]{hoy.with(DayOfWeek.MONDAY), hoy};
        }
        if (tiene(t, "mes pasado", "ultimo mes", "mes anterior")) {
            LocalDate inicio = hoy.minusMonths(1).withDayOfMonth(1);
            return new LocalDate[]{inicio, inicio.withDayOfMonth(inicio.lengthOfMonth())};
        }
        if (tiene(t, "este mes", "mes actual", "va del mes", "en el mes")) {
            return new LocalDate[]{hoy.withDayOfMonth(1), hoy};
        }
        if (tiene(t, "ano pasado", "ultimo ano", "ano anterior", "gestion pasada", "gestion anterior")) {
            LocalDate inicio = LocalDate.of(hoy.getYear() - 1, 1, 1);
            return new LocalDate[]{inicio, inicio.withDayOfYear(inicio.lengthOfYear())};
        }
        if (tiene(t, "este ano", "ano actual", "va del ano", "esta gestion")) {
            return new LocalDate[]{hoy.withDayOfYear(1), hoy};
        }

        Matcher a = ANIO.matcher(t);
        if (a.find()) {
            LocalDate inicio = LocalDate.of(Integer.parseInt(a.group(1)), 1, 1);
            return new LocalDate[]{inicio, inicio.withDayOfYear(inicio.lengthOfYear())};
        }
        return null;
    }

    private static Integer detectarLimite(String t) {
        Matcher m = LIMITE.matcher(t);
        if (!m.find()) {
            return null;
        }
        int limite = Integer.parseInt(m.group(1) != null ? m.group(1) : m.group(2));
        return limite >= 1 && limite <= 100 ? limite : null;
    }

    /**
     * La entidad cuyas palabras más aparecen en la frase. Cada palabra pesa menos cuantas más
     * entidades la comparten, así "laptop" sola no elige nada pero "laptop TUF" sí. Si hay
     * empate no se elige ninguna: es mejor no filtrar que filtrar por la equivocada.
     */
    private static Long buscar(String t, List<Entidad> entidades) {
        Map<String, Integer> frecuencia = new HashMap<>();
        List<Set<String>> palabrasPorEntidad = entidades.stream().map(InterpreteLocalVoz::palabras).toList();
        palabrasPorEntidad.forEach(ps -> ps.forEach(p -> frecuencia.merge(p, 1, Integer::sum)));

        Long mejor = null;
        double mejorPuntaje = 0;
        double segundo = 0;
        for (int i = 0; i < entidades.size(); i++) {
            double puntaje = 0;
            for (String p : palabrasPorEntidad.get(i)) {
                if (t.contains(" " + p + " ")) {
                    puntaje += 1.0 / frecuencia.get(p);
                }
            }
            if (puntaje > mejorPuntaje) {
                segundo = mejorPuntaje;
                mejorPuntaje = puntaje;
                mejor = entidades.get(i).id();
            } else if (puntaje > segundo) {
                segundo = puntaje;
            }
        }
        return mejorPuntaje >= 0.5 && mejorPuntaje > segundo ? mejor : null;
    }

    private static Set<String> palabras(Entidad e) {
        String texto = normalizar((e.nombre() == null ? "" : e.nombre()) + " " + (e.alias() == null ? "" : e.alias()));
        Set<String> resultado = new LinkedHashSet<>();
        for (String p : texto.trim().split(" ")) {
            if (p.length() >= 3 && !VACIAS.contains(p)) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    private static LocalDate[] dia(LocalDate d) {
        return new LocalDate[]{d, d};
    }

    /** Alguna clave aparece al inicio de una palabra de la frase. */
    private static boolean tiene(String t, String... claves) {
        for (String clave : claves) {
            if (t.contains(" " + clave)) {
                return true;
            }
        }
        return false;
    }
}
