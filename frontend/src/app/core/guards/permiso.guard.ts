import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { catchError, map, of } from 'rxjs';
import { PermisosService } from '../permisos/permisos.service';

/**
 * Exige un permiso concreto para entrar a una ruta. Es la tarea 6 de la HU-03.
 *
 * Esto es una comodidad para el usuario, no la seguridad del sistema: aunque alguien
 * escriba la URL a mano y se saltee el guard, el backend vuelve a verificar el permiso y
 * responde 403. La autoridad siempre es el backend.
 *
 * @example
 * { path: 'seguridad/roles', canActivate: [authGuard, permisoGuard('SEGURIDAD_CONSULTAR')], ... }
 */
export function permisoGuard(codigo: string): CanActivateFn {
  return () => {
    const permisos = inject(PermisosService);
    const router = inject(Router);

    return permisos.asegurarCargados().pipe(
      map(() => (permisos.tiene(codigo) ? true : router.createUrlTree(['/app/sin-acceso']))),
      catchError(() => of(router.createUrlTree(['/app/sin-acceso']))),
    );
  };
}
