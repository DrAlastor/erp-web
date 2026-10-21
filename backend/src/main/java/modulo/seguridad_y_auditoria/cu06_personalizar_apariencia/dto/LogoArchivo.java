package modulo.seguridad_y_auditoria.cu06_personalizar_apariencia.dto;

/** Logo guardado, listo para devolverse como imagen. */
public record LogoArchivo(byte[] contenido, String tipo, int version) {
}
