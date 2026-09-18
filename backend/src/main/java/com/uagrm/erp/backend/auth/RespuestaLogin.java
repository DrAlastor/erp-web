package com.uagrm.erp.backend.auth;

import java.util.UUID;

/**
 * Respuesta de un inicio de sesión correcto.
 *
 * <p>No incluye los permisos: el cliente los pide aparte a
 * {@code GET /api/seguridad/mis-permisos}, que siempre devuelve el estado actual.
 */
public record RespuestaLogin(String token, String nombre, UUID empresaId, long minutosDeVigencia) {
}
