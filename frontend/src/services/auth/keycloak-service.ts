import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';

@Injectable({
  providedIn: 'root',
})
export class KeycloakService {
  private readonly keycloak = new Keycloak({
    url: `${window.location.origin}/auth`,
    realm: 'mail-system',
    clientId: 'mail-system-frontend',
  });

  public async initialize(): Promise<boolean> {
    return this.keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
  }

  public async login(returnUrl: string): Promise<void> {
    await this.keycloak.login({
      redirectUri: this.localUrl(returnUrl, '/mails'),
    });
  }

  public async logout(): Promise<void> {
    await this.keycloak.logout({
      redirectUri: new URL('/login', window.location.origin).toString(),
    });
  }

  public isAuthenticated(): boolean {
    return this.keycloak.authenticated === true;
  }

  public async getValidAccessToken(): Promise<string | null> {
    if (!this.isAuthenticated()) {
      return null;
    }

    try {
      await this.keycloak.updateToken(30);
      return this.keycloak.token ?? null;
    } catch {
      this.keycloak.clearToken();
      return null;
    }
  }

  public clearToken(): void {
    this.keycloak.clearToken();
  }

  private localUrl(path: string, fallback: string): string {
    const url = new URL(path, window.location.origin);
    return url.origin === window.location.origin
      ? url.toString()
      : new URL(fallback, window.location.origin).toString();
  }
}
