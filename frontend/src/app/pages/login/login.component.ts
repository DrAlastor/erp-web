import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { switchMap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { PermisosService } from '../../core/permisos/permisos.service';
import { environment } from '../../../environments/environment';
import { ErrorApi } from '../../core/auth/auth.model';

/**
 * Pantalla de acceso.
 *
 * Después de entrar pide los permisos efectivos antes de navegar: así el menú ya se dibuja
 * completo en la primera pantalla, sin un parpadeo de ítems apareciendo.
 */
@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly permisos = inject(PermisosService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  readonly email = signal('');
  readonly password = signal('');
  readonly enviando = signal(false);
  readonly error = signal<string | null>(null);

  /** Las credenciales de demostración solo se muestran fuera de producción. */
  readonly mostrarDemo = !environment.production;

  entrar(): void {
    if (this.enviando()) {
      return;
    }
    this.error.set(null);
    this.enviando.set(true);

    this.auth
      .login(this.email(), this.password())
      .pipe(switchMap(() => this.permisos.cargar()))
      .subscribe({
        next: () => {
          this.enviando.set(false);
          const destino = this.ruta.snapshot.queryParamMap.get('volverA') ?? '/app';
          void this.router.navigateByUrl(destino);
        },
        error: (fallo: HttpErrorResponse) => {
          this.enviando.set(false);
          this.auth.logout();
          this.error.set(this.mensajeDe(fallo));
        },
      });
  }

  usar(email: string, password: string): void {
    this.email.set(email);
    this.password.set(password);
  }

  private mensajeDe(fallo: HttpErrorResponse): string {
    if (fallo.status === 0) {
      return 'No se pudo conectar con el servidor. Verificá que el backend esté corriendo en el puerto 8080.';
    }
    const cuerpo = fallo.error as ErrorApi | null;
    return cuerpo?.mensaje ?? 'No se pudo iniciar sesión. Intentá de nuevo.';
  }
}
