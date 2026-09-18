/** Un rol con su matriz de permisos, tal como lo devuelve el backend. */
export interface Rol {
  id: string;
  codigo: string;
  nombre: string;
  descripcion: string;
  activo: boolean;
  esSistema: boolean;
  permisos: string[];
}

/** Un permiso del catálogo, con sus etiquetas ya traducidas por el backend. */
export interface Permiso {
  codigo: string;
  modulo: string;
  accion: string;
  etiquetaModulo: string;
  etiquetaAccion: string;
  descripcion: string;
}

/** Un usuario de la empresa, para elegir a quién asignarle un rol. */
export interface UsuarioResumen {
  id: string;
  nombre: string;
  email: string;
  activo: boolean;
}

/** Un rol que un usuario tiene asignado. */
export interface Asignacion {
  rolId: string;
  codigo: string;
  nombre: string;
  activo: boolean;
  asignadoEn: string;
}
