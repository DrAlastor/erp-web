package modulo.tesoreria_y_cajas.cu32_cuentas_bancarias_y_cajas.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

/**
 * Reglas puras de la caja POS (HU-34 / HU-35): quién puede operar un turno, en qué estado y
 * cuánto efectivo puede salir. Sin dependencias de base de datos.
 */
public final class ReglasCaja {

    /** Roles que pueden operar y consultar turnos ajenos. */
    public static final Set<String> ROLES_ADMIN = Set.of("ADMINISTRADOR", "ADMIN", "SUPERADMIN");

    private ReglasCaja() {}

    public static boolean esRolAdmin(String rolNombre) {
        return rolNombre != null && ROLES_ADMIN.contains(rolNombre.trim().toUpperCase(Locale.ROOT));
    }

    /** Solo el cajero dueño del turno o un administrador (403). */
    public static void validarAcceso(Long cajeroId, Long usuarioId, boolean admin) {
        if (admin) return;
        if (cajeroId == null || !cajeroId.equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el cajero dueño del turno o un administrador puede operar este turno de caja");
        }
    }

    /** El turno debe seguir ABIERTO (409). */
    public static void validarAbierto(Long turnoId, boolean abierto) {
        if (!abierto) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno de caja #" + turnoId + " ya está cerrado");
        }
    }

    /** Un egreso en efectivo no puede superar el efectivo disponible del turno (409). */
    public static void validarEgresoEfectivo(BigDecimal monto, BigDecimal disponible) {
        BigDecimal saldo = CalculadoraArqueo.escala(disponible);
        if (CalculadoraArqueo.escala(monto).compareTo(saldo) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El egreso (Bs " + CalculadoraArqueo.escala(monto).toPlainString()
                            + ") supera el efectivo disponible en caja (Bs " + saldo.toPlainString() + ")");
        }
    }

    /** Comprobante por defecto: MOV-<turnoId>-<n>. */
    public static String numeroComprobante(Long turnoId, long secuencia) {
        return "MOV-" + turnoId + "-" + secuencia;
    }
}
