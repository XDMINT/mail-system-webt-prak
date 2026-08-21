import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
import { API_BASE_URL } from '../constants';
import { KeycloakService } from '../services/auth/keycloak-service';

export const authInterceptorInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith(API_BASE_URL)) {
    return next(request);
  }

  const keycloak = inject(KeycloakService);

  return from(keycloak.getValidAccessToken()).pipe(
    switchMap((token) => {
      const authenticatedRequest = token
        ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
        : request;

      return next(authenticatedRequest);
    }),
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        keycloak.clearToken();
      }
      return throwError(() => error);
    }),
  );
};
