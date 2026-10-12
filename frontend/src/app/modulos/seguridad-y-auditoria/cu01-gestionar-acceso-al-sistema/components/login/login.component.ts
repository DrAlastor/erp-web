import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../../../../core/services/theme.service';
import { ApiErrorResponse } from '../../../../../core/models/api-response.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  readonly loading = signal(false);
  readonly showPassword = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly accountLocked = signal(false);
  readonly currentYear = new Date().getFullYear();

  readonly themeService = inject(ThemeService);

  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    usernameOrEmail: ['', [Validators.required]],
    password: ['', [Validators.required]],
    rememberMe: [false],
  });

  togglePassword(): void {
    this.showPassword.update((value) => !value);
  }

  isInvalid(field: 'usernameOrEmail' | 'password'): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  submit(): void {
    if (this.loading()) {
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.accountLocked.set(false);

    const { usernameOrEmail, password, rememberMe } = this.form.getRawValue();

    this.authService
      .login({ usernameOrEmail: usernameOrEmail.trim(), password }, rememberMe)
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.router.navigate(['/app']);
        },
        error: (error: HttpErrorResponse) => {
          this.loading.set(false);
          const body = error.error as ApiErrorResponse | undefined;

          if (body?.error === 'ACCOUNT_LOCKED') {
            this.accountLocked.set(true);
            this.errorMessage.set(body.message);
          } else {
            this.errorMessage.set(
              body?.message ??
                (error.status === 0
                  ? 'No se pudo conectar con el servidor. Inténtalo de nuevo.'
                  : error.status >= 500
                    ? 'El servidor no está disponible. Inténtalo de nuevo.'
                    : 'Usuario o contraseña incorrectos'),
            );
          }
        },
      });
  }
}
