import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth';

/**
 * Si el servidor responde 401 (token vencido, inválido o cuenta desactivada),
 * se cierra la sesión y se vuelve al login en lugar de mostrar un error en la página.
 */
export const sesionInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !req.url.includes('/auth/login')) {
        auth.logout();
        router.navigate(['/login'], { queryParams: { motivo: 'sesion' } });
      }
      return throwError(() => error);
    }),
  );
};
