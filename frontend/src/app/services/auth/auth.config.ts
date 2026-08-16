import { AuthConfig } from 'angular-oauth2-oidc';

export const authConfig: AuthConfig = {
  issuer: 'http://localhost:9080/realms/master',
  clientId: 'http://localhost/app/',
  redirectUri: window.location.origin,
  responseType: 'code',
  scope: 'openid profile email',
  strictDiscoveryDocumentValidation: false,
  oidc: true
};
