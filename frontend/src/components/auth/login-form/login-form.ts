import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { FloatLabelModule } from 'primeng/floatlabel';
import {Router, RouterLink} from '@angular/router';
import { KeycloakService } from '../../../app/services/auth/keycloak.service';

@Component({
  selector: 'app-login-form',
  imports: [
    MessageModule,
    ToastModule,
    ButtonModule,
    InputTextModule,
    ReactiveFormsModule,
    FloatLabelModule,
    RouterLink,
  ],
  providers: [MessageService],
  templateUrl: './login-form.html',
})

export class LoginForm {
  protected loginForm = new FormGroup({
    email: new FormControl('', [Validators.email, Validators.required]),
    password: new FormControl('', [Validators.minLength(6), Validators.required]),
  });

  private messageService = inject(MessageService);
  private router = inject(Router);
  private keycloakService = inject(KeycloakService);

  protected formSubmitted = signal(false);

  async onSubmit() {
    console.log("Login");
    this.keycloakService.login();
  }

  isInvalid(controlName: string) {
    const control = this.loginForm.get(controlName);
    return control?.invalid && (control.touched || this.formSubmitted());
  }
}
