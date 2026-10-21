import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../../../../core/services/theme.service';

@Component({
  selector: 'app-registro-cliente',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './registro-cliente.component.html',
  styleUrl: './registro-cliente.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegistroClienteComponent {
  readonly loading = signal(false);
  readonly showPassword = signal(false);
  readonly error = signal('');
  readonly themeService = inject(ThemeService);

  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    codigoEmpresa: ['', [Validators.required, Validators.maxLength(30)]],
    username: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    razonSocial: ['', [Validators.required, Validators.maxLength(150)]],
    nitCi: ['', [Validators.required, Validators.maxLength(30)]],
    telefono: ['', [Validators.maxLength(30)]],
    direccion: ['', [Validators.maxLength(255)]],
  });

  togglePassword(): void {
    this.showPassword.update((v) => !v);
  }

  isInvalid(field: string): boolean {
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
    this.error.set('');
    const request = this.form.getRawValue();
    this.auth.registerCliente({
      ...request,
      codigoEmpresa: request.codigoEmpresa.trim().toUpperCase(),
    }).subscribe({
      next: () => this.router.navigate(['/app']),
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        this.error.set(err.error?.message ?? 'No se pudo crear la cuenta. Verifica los datos.');
      },
    });
  }
}
