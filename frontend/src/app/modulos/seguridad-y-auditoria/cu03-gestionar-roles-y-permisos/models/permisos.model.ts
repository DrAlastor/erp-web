/** Permisos efectivos del usuario, tal como los devuelve /api/seguridad/mis-permisos. */
export interface MisPermisos {
  usuarioId: number;
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
