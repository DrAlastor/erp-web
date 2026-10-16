import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ThemeService } from '../../../../../core/services/theme.service';
import { NotificacionesService } from '../../../../../core/services/notificaciones.service';

@Component({
  selector: 'app-recuperar-contrasena',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './recuperar-contrasena.component.html',
  styleUrl: './recuperar-contrasena.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecuperarContrasenaComponent {
  readonly step = signal<'request' | 'reset'>('request');
  readonly loading = signal(false);
  readonly successMessage = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly showNewPassword = signal(false);
  readonly showConfirmPassword = signal(false);

  readonly themeService = inject(ThemeService);
  private readonly notificacionesService = inject(NotificacionesService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  // Formulario 1: Solicitar código / enlace
  readonly requestForm = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  // Formulario 2: Crear nueva contraseña
  readonly resetForm = this.fb.nonNullable.group({
    code: [''],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required, Validators.minLength(8)]],
  });

  setStep(newStep: 'request' | 'reset'): void {
    this.step.set(newStep);
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  toggleNewPassword(): void {
    this.showNewPassword.update((v) => !v);
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword.update((v) => !v);
  }

  isInvalidRequest(field: 'email'): boolean {
    const c = this.requestForm.get(field);
    return !!c && c.invalid && (c.touched || c.dirty);
  }

  isInvalidReset(field: 'newPassword' | 'confirmPassword'): boolean {
    const c = this.resetForm.get(field);
    return !!c && c.invalid && (c.touched || c.dirty);
  }

  submitRequest(): void {
    if (this.loading()) return;
    if (this.requestForm.invalid) {
      this.requestForm.markAllAsTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    // Simulación de envío de correo de recuperación
    setTimeout(() => {
      this.loading.set(false);
      this.successMessage.set(
        'Hemos enviado las instrucciones y el código a tu correo.'
      );
      // Avanzar al paso de creación de nueva contraseña
      this.step.set('reset');
    }, 800);
  }

  submitReset(): void {
    if (this.loading()) return;
    if (this.resetForm.invalid) {
      this.resetForm.markAllAsTouched();
      return;
    }

    const { newPassword, confirmPassword } = this.resetForm.getRawValue();
    if (newPassword !== confirmPassword) {
      this.errorMessage.set('Las contraseñas no coinciden. Por favor verifícalas.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    setTimeout(() => {
      this.loading.set(false);
      this.successMessage.set('Contraseña actualizada con éxito. Redirigiendo...');
      const email = this.requestForm.getRawValue().email;
      this.notificacionesService.notificarCambioPassword(email);
      setTimeout(() => {
        this.router.navigate(['/login']);
      }, 1200);
    }, 900);
  }
}
