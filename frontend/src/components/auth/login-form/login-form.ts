import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../../services/auth/auth-service';

@Component({
  selector: 'app-login-form',
  imports: [ButtonModule],
  templateUrl: './login-form.html',
})
export class LoginForm {
  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  protected login(): void {
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/mails';
    void this.authService.login(returnUrl);
  }
}
