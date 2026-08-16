import { inject, Injectable } from '@angular/core';
import { CanActivate } from '@angular/router';
import { KeycloakService } from '../../app/services/auth/keycloak.service';

@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  private keycloakService = inject(KeycloakService);

  canActivate(): boolean {
    if (this.keycloakService.isLoggedIn) {
      return true;
    } else {
      this.keycloakService.login();
      return false;
    }
  }
}
