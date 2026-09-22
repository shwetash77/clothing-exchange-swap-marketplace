import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { API_URL } from './config';

/**
 * Attaches the JWT to every API call (except login/signup) and logs the user
 * out when the session is no longer valid.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const isPublic = req.url.includes('/auth/');

  const request =
    token && req.url.startsWith(API_URL) && !isPublic
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(request).pipe(
    catchError((err: HttpErrorResponse) => {
      // Spring Security answers 403 (not 401) for a missing/expired JWT unless
      // a custom entry point is configured, so also check expiry locally.
      const sessionDead = !isPublic && (err.status === 401 || (err.status === 403 && auth.isTokenExpired()));
      if (sessionDead) auth.logout(true);
      return throwError(() => err);
    }),
  );
};
