package modulo.seguridad_y_auditoria.roles_y_permisos.catalogo;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Los siete roles de sistema del ERP con su matriz de permisos.
 *
 * <p>Es la transcripción literal de la <em>Matriz de permisos por rol</em> de la HU-03.
 * Las celdas se escriben en el mismo orden de columnas del documento y con la misma
 * notación, para que una fila de acá se pueda comparar de un vistazo con la fila
 * equivalente del documento:
 *
 * <pre>
 * Rol                 Seguridad   Comercial   Inventario   Contabilidad   Facturación   Reportes
 * Administrador       C L M A     C L M A     C L M A      C L M A        C L M A       C L M A
 * Gerente General     –           L           L            L              L             L
 * Contador            –           L           L            C L M A        L             C L
 * Cajero              –           C L         L            –              C L A         –
 * Encargado Almacén   –           L           C L M        –              –             L
 * Preventista         –           C L         L            –              –             –
 * Auditor Interno     L           L           L            L              L             L
 * </pre>
 *
 * <p>Estos roles no se crean ni se borran desde la aplicación: el administrador edita su
 * matriz de permisos y puede activarlos o desactivarlos. El aprovisionamiento los siembra
 * en cada empresa a partir de esta definición.
 */
public final class MatrizRolesDeSistema {

    /** Los siete roles, en el orden en que los lista el documento. */
    public static final List<RolDeSistema> ROLES = List.of(
            rol("ADMINISTRADOR", "Administrador",
                    "Control total del sistema, incluida la gestión de roles y permisos",
                    celdas("C L M A", "C L M A", "C L M A", "C L M A", "C L M A", "C L M A")),

            rol("GERENTE_GENERAL", "Gerente General",
                    "Visión completa del negocio en modo consulta, sin operar los módulos",
                    celdas("–", "L", "L", "L", "L", "L")),

            rol("CONTADOR", "Contador",
                    "Responsable de la contabilidad; consulta el resto de la operación",
                    celdas("–", "L", "L", "C L M A", "L", "C L")),

            rol("CAJERO", "Cajero",
                    "Registra ventas y facturación en el punto de venta",
                    celdas("–", "C L", "L", "–", "C L A", "–")),

            rol("ENCARGADO_ALMACEN", "Encargado de Almacén",
                    "Administra el inventario y los movimientos de mercadería",
                    celdas("–", "L", "C L M", "–", "–", "L")),

            rol("PREVENTISTA", "Preventista",
                    "Toma pedidos en ruta y consulta la disponibilidad de stock",
                    celdas("–", "C L", "L", "–", "–", "–")),

            rol("AUDITOR_INTERNO", "Auditor Interno",
                    "Consulta toda la información del sistema, sin modificar nada",
                    celdas("L", "L", "L", "L", "L", "L")));

    private static final Map<String, RolDeSistema> POR_CODIGO = indexar();

    private MatrizRolesDeSistema() {
    }

    /** Códigos de los siete roles de sistema. */
    public static Set<String> codigos() {
        return POR_CODIGO.keySet();
    }

    /**
     * Devuelve la definición de un rol de sistema.
     *
     * @throws IllegalArgumentException si el código no es de un rol de sistema
     */
    public static RolDeSistema porCodigo(String codigo) {
        RolDeSistema rol = POR_CODIGO.get(codigo);
        if (rol == null) {
            throw new IllegalArgumentException("No existe el rol de sistema " + codigo);
        }
        return rol;
    }

    /** Indica si un código corresponde a un rol de sistema. */
    public static boolean esDeSistema(String codigo) {
        return POR_CODIGO.containsKey(codigo);
    }

    private static RolDeSistema rol(String codigo, String nombre, String descripcion,
                                    Map<ModuloErp, String> matriz) {
        return RolDeSistema.desdeMatriz(codigo, nombre, descripcion, matriz);
    }

    /** Las seis celdas de una fila de la matriz, en el orden de columnas del documento. */
    private static Map<ModuloErp, String> celdas(String seguridad, String comercial, String inventario,
                                                 String contabilidad, String facturacion, String reportes) {
        Map<ModuloErp, String> fila = new EnumMap<>(ModuloErp.class);
        fila.put(ModuloErp.SEGURIDAD, seguridad);
        fila.put(ModuloErp.COMERCIAL, comercial);
        fila.put(ModuloErp.INVENTARIO, inventario);
        fila.put(ModuloErp.CONTABILIDAD, contabilidad);
        fila.put(ModuloErp.FACTURACION, facturacion);
        fila.put(ModuloErp.REPORTES, reportes);
        return fila;
    }

    private static Map<String, RolDeSistema> indexar() {
        Map<String, RolDeSistema> indice = new java.util.LinkedHashMap<>();
        for (RolDeSistema rol : ROLES) {
            indice.put(rol.codigo(), rol);
        }
        return Map.copyOf(indice);
    }
}
