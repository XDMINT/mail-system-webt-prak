import { HttpInterceptorFn} from '@angular/common/http';
import { inject } from '@angular/core';
import {KeycloakService} from '../app/services/auth/keycloak.service';

export const authInterceptorInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloakService = inject(KeycloakService);
  const token = keycloakService.accessToken;

  if (!token) {
    return next(req);
  }

  return next(
    req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    })
  );
};
