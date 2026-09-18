import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/**
 * Agrega la cabecera `Authorization: Bearer ...` a las peticiones del ERP.
 *
 * No la agrega al login —que es justamente donde todavía no hay token— ni a peticiones
 * fuera de la API.
 */
export const authInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  const token = inject(AuthService).token();

  if (!token || peticion.url.includes('/auth/login')) {
    return siguiente(peticion);
  }

  return siguiente(
    peticion.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    }),
  );
};
