import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { SuperAdminService } from '../../../core/services/superadmin.service';

@Component({
  selector: 'app-superadmin-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './superadmin-login.component.html',
  styleUrl: './superadmin-login.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SuperadminLoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly superAdminService = inject(SuperAdminService);
  private readonly router = inject(Router);

  readonly loading = signal(false);
  readonly showPassword = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly currentYear = new Date().getFullYear();

  readonly form = this.fb.nonNullable.group({
    email: ['yevaraponcealessandro@gmail.com', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  isInvalid(field: 'email' | 'password'): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && (control.touched || control.dirty);
  }

  submit(): void {
    if (this.loading()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const { email, password } = this.form.getRawValue();
    this.superAdminService.login({ email, password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/superadmin/dashboard']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Credenciales de Super Administrador inválidas');
      }
    });
  }
}
