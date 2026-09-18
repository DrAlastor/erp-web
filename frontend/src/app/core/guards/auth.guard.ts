import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { catchError, map, of } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { PermisosService } from '../permisos/permisos.service';

/**
 * Deja entrar solo a quien tenga sesión, y se asegura de que sus permisos efectivos estén
 * cargados antes de dibujar la pantalla.
 *
 * Si el token está vencido, el backend responde 401 al pedir los permisos: entonces se
 * cierra la sesión y se manda al login, en lugar de mostrar una pantalla vacía.
 */
export const authGuard: CanActivateFn = (_ruta, estado) => {
  const auth = inject(AuthService);
  const permisos = inject(PermisosService);
  const router = inject(Router);

  if (!auth.estaAutenticado()) {
    return router.createUrlTree(['/login'], { queryParams: { volverA: estado.url } });
  }

  return permisos.asegurarCargados().pipe(
    map(() => true),
    catchError(() => {
      auth.logout();
      permisos.limpiar();
      return of(router.createUrlTree(['/login'], { queryParams: { volverA: estado.url } }));
    }),
  );
};
