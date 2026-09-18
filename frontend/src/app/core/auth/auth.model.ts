/** Respuesta del backend al iniciar sesión. */
export interface RespuestaLogin {
  token: string;
  nombre: string;
  empresaId: string;
  minutosDeVigencia: number;
}

/** Sesión guardada en el navegador. No incluye permisos: se piden aparte y siempre frescos. */
export interface Sesion {
  token: string;
  nombre: string;
  empresaId: string;
}

/** Permisos efectivos del usuario, tal como los devuelve /api/seguridad/mis-permisos. */
export interface MisPermisos {
  usuarioId: string;
  empresaId: string;
  nombre: string;
  permisos: string[];
  roles: string[];
}

/** Forma de los errores del backend. */
export interface ErrorApi {
  error: string;
  mensaje: string;
  permisoRequerido?: string;
}
