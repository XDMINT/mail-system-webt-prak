import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';

@Injectable({
  providedIn: 'root',
})
export class KeycloakService {
  constructor(private oauthService: OAuthService) {
    this.oauthService.configure(authConfig);
  }

  async init() {
    await this.oauthService.loadDiscoveryDocumentAndTryLogin();
    console.log("Login URL:", this.oauthService.loginUrl);
    console.log("Issuer:", this.oauthService.issuer);
    console.log("Discovery:", this.oauthService.loadDiscoveryDocument);
    console.log("Access Token:", this.oauthService.getAccessToken());
    console.log("ID Token:", this.oauthService.getIdToken());
    console.log("Logged in:", this.oauthService.hasValidAccessToken());
  }

  login() {
    console.log("Vor Login");

    this.oauthService.initLoginFlow();

    console.log("Nach Login");
    console.log("Aktuelle URL:", window.location.href);
    console.log(window.crypto);
    console.log(window.crypto?.subtle);
  }

  logout() {
    this.oauthService.logOut();
  }

  get accessToken(): string {
    return this.oauthService.getAccessToken();
  }

  get isLoggedIn(): boolean {
    return this.oauthService.hasValidAccessToken();
  }
}
