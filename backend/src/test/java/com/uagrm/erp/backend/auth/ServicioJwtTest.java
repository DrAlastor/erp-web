package com.uagrm.erp.backend.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El token lleva identidad y empresa, nunca permisos: si los llevara, quitarle un rol a
 * alguien no tendría efecto hasta que el token expirara. Estas pruebas fijan ese contrato
 * y el comportamiento ante tokens que no se pueden confiar.
 */
class ServicioJwtTest {

    private static final String SECRETO = "secreto-de-desarrollo-para-pruebas-con-largo-suficiente-hs256";

    private static final UsuarioPrincipal USUARIO = new UsuarioPrincipal(
            UUID.fromString("55555555-5555-5555-5555-555555555555"),
            UUID.fromString("66666666-6666-6666-6666-666666666666"),
            "Santiago Arteaga",
            "admin@demo.bo");

    private ServicioJwt servicio(long minutos) {
        return new ServicioJwt(SECRETO, minutos);
    }

    @Test
    @DisplayName("el token va y vuelve conservando usuario, empresa, nombre y email")
    void idaYVuelta() {
        ServicioJwt servicio = servicio(60);

        String token = servicio.generar(USUARIO);
        Optional<UsuarioPrincipal> leido = servicio.leer(token);

        assertThat(leido).contains(USUARIO);
    }

    @Test
    @DisplayName("el token no lleva permisos adentro")
    void elTokenNoLlevaPermisos() {
        String token = servicio(60).generar(USUARIO);

        String cuerpo = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertThat(cuerpo).doesNotContainIgnoringCase("permiso");
        assertThat(cuerpo).doesNotContainIgnoringCase("authorit");
        assertThat(cuerpo).doesNotContainIgnoringCase("rol");
    }

    @Test
    @DisplayName("un token firmado con otro secreto se rechaza")
    void firmaInvalida() {
        String token = new ServicioJwt("otro-secreto-completamente-distinto-pero-largo-igual", 60).generar(USUARIO);

        assertThat(servicio(60).leer(token)).isEmpty();
    }

    @Test
    @DisplayName("un token vencido se rechaza")
    void tokenVencido() {
        ServicioJwt servicio = servicio(-5);

        String token = servicio.generar(USUARIO);

        assertThat(servicio.leer(token)).isEmpty();
    }

    @Test
    @DisplayName("un token con basura se rechaza sin lanzar excepción")
    void tokenIlegible() {
        ServicioJwt servicio = servicio(60);

        assertThat(servicio.leer("esto-no-es-un-token")).isEmpty();
        assertThat(servicio.leer("")).isEmpty();
        assertThat(servicio.leer(null)).isEmpty();
        assertThat(servicio.leer("a.b.c")).isEmpty();
    }

    @Test
    @DisplayName("un secreto demasiado corto para HS256 no se acepta al construir el servicio")
    void secretoCorto() {
        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> new ServicioJwt("corto", 60))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
