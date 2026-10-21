import { PermisosService } from '../seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';

/** Roles que el backend acepta para las acciones reservadas al Administrador. */
const ROLES_ADMIN = ['ADMIN', 'ADMINISTRADOR', 'SUPERADMIN'];

/**
 * Si el usuario conectado es Administrador. Sólo decide qué botones se ven: la
 * autorización real la vuelve a verificar el backend en cada operación.
 */
export function esAdministrador(permisos: PermisosService): boolean {
  return permisos.roles().some((r) => ROLES_ADMIN.includes(r.toUpperCase()));
}
