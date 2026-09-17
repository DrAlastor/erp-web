import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { isApiRequest, isAuthRequest } from './auth.interceptor';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  return next(req).pipe(catchError((error: HttpErrorResponse) => {
    if (error.status !== 401 || !isApiRequest(req.url) || isAuthRequest(req.url) ||
        !authService.getRefreshToken()) return throwError(() => error);
    // Un solo refresh compartido y un solo reintento.
    const refreshToken = authService.getRefreshToken();
    return authService.refresh().pipe(
      catchError(refreshError => {
        if (refreshError instanceof HttpErrorResponse && (refreshError.status === 401 || refreshError.status === 403) &&
            (!authService.getRefreshToken() || authService.getRefreshToken() === refreshToken)) {
          authService.logout();
        }
        return throwError(() => refreshError);
      }),
      switchMap(response => next(req.clone({ setHeaders: { Authorization: `Bearer ${response.accessToken}` } })).pipe(
        catchError(retryError => {
          if (retryError instanceof HttpErrorResponse && retryError.status === 401 &&
              authService.getRefreshToken() === refreshToken) authService.logout();
          return throwError(() => retryError);
        })
      ))
    );
  }));
};
