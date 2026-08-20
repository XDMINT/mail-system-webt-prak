import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../../constants';
import { User } from '../../types/user';
import { KeycloakService } from './keycloak-service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly keycloak = inject(KeycloakService);
  private readonly currentUserState = signal<User | null>(null);

  public readonly currentUser = this.currentUserState.asReadonly();

  public async initialize(): Promise<void> {
    const authenticated = await this.keycloak.initialize();
    if (authenticated) {
      await this.loadCurrentUser();
    }
  }

  public async login(returnUrl = '/mails'): Promise<void> {
    await this.keycloak.login(returnUrl);
  }

  public async logout(): Promise<void> {
    this.currentUserState.set(null);
    await this.keycloak.logout();
  }

  public isAuthenticated(): boolean {
    return this.keycloak.isAuthenticated();
  }

  public getCurrentUser(): User | null {
    return this.currentUserState();
  }

  private async loadCurrentUser(): Promise<void> {
    const user = await firstValueFrom(this.http.get<User>(`${API_BASE_URL}/users/me`));
    this.currentUserState.set(user);
  }
}
