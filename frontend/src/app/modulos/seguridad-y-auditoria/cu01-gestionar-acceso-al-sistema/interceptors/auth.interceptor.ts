import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../../../environments/environment';

export function isApiRequest(url: string): boolean {
  return url === environment.apiUrl || url.startsWith(`${environment.apiUrl}/`);
}
export function isAuthRequest(url: string): boolean {
  return url.startsWith(`${environment.apiUrl}/auth/`) || url.startsWith(`${environment.apiUrl}/superadmin/auth/`);
}
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isApiRequest(req.url) || isAuthRequest(req.url)) return next(req);

  // Peticiones al subsistema Super Admin (Master)
  if (req.url.startsWith(`${environment.apiUrl}/superadmin/`)) {
    const superToken = typeof window !== 'undefined'
      ? (sessionStorage.getItem('erp_superadmin_token') || localStorage.getItem('erp_superadmin_token'))
      : null;
    if (superToken) {
      return next(req.clone({ setHeaders: { Authorization: `Bearer ${superToken}` } }));
    }
    return next(req);
  }

  // Peticiones estándar de Tenants
  return inject(AuthService).getValidAccessToken().pipe(
    switchMap(token => next(token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req))
  );
};
