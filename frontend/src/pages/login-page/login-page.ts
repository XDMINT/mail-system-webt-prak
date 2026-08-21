import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import {LoginForm} from '../../components/auth/login-form/login-form';
import { AuthService } from '../../services/auth/auth-service';

@Component({
  selector: 'app-login-page',
  imports: [LoginForm],
  templateUrl: './login-page.html',
})
export class LoginPage implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  public ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      void this.router.navigate(['/mails']);
    }
  }
}
