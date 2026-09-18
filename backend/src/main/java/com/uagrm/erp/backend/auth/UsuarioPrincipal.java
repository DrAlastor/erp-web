package com.uagrm.erp.backend.auth;

import java.util.UUID;

/**
 * Identidad del usuario que hizo la petición: quién es y a qué empresa pertenece.
 *
 * <p>Es el único punto de contacto entre la autenticación y la autorización. El RBAC
 * depende de este tipo y de nada más del paquete {@code auth}, así que cuando la CU-01
 * entregue la autenticación definitiva basta con que siga dejando un
 * {@code UsuarioPrincipal} en el contexto de seguridad.
 *
 * <p>Notar que no tiene permisos ni roles: los permisos se resuelven contra la base de
 * datos en cada petición, no se transportan.
 */
public record UsuarioPrincipal(UUID usuarioId, UUID empresaId, String nombre, String email) {

    public UsuarioPrincipal {
        if (usuarioId == null || empresaId == null) {
            throw new IllegalArgumentException("El principal necesita usuario y empresa");
        }
    }
}
