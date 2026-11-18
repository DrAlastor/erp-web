import { PermisosService } from '../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';

const ROLES_ADMIN = ['ADMIN', 'ADMINISTRADOR', 'SUPERADMIN'];

/**
 * Si el usuario puede ejecutar la acción contable (CREAR, MODIFICAR...). Solo decide qué
 * botones se ven: el backend vuelve a verificar el permiso en cada operación.
 */
export function puedeContabilidad(permisos: PermisosService, accion: 'CREAR' | 'MODIFICAR'): boolean {
  return (
    permisos.tiene(`CONTABILIDAD_${accion}`) ||
    permisos.roles().some((r) => ROLES_ADMIN.includes(r.toUpperCase()))
  );
}

/** Mensaje para un error HTTP de las pantallas contables. */
export function mensajeError(status: number, mensajeServidor: string | undefined, recurso: string): string {
  if (status === 403) return `No tienes autorización para esta operación sobre ${recurso}.`;
  if (status === 0) return 'No se pudo conectar con el servidor. Intenta nuevamente.';
  if (status === 401) return 'La sesión ha expirado. Inicia sesión nuevamente.';
  if (status >= 500) return 'El servidor no pudo completar la operación.';
  return mensajeServidor || 'No se pudo completar la operación.';
}
