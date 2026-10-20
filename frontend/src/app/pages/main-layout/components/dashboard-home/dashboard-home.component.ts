import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../../modulos/seguridad-y-auditoria/cu01-gestionar-acceso-al-sistema/services/auth.service';
import { PermisosService } from '../../../../modulos/seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';
export type RolVista = 'AUTO' | 'ADMIN' | 'VENDEDOR' | 'ALMACENERO' | 'CAJERO';

@Component({
  selector: 'app-dashboard-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard-home.component.html',
  styleUrl: './dashboard-home.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardHomeComponent {
  readonly authService = inject(AuthService);
  readonly permisos = inject(PermisosService);

  // Selector para cambiar de perspectiva (para administradores / demostración)
  readonly vistaSeleccionada = signal<RolVista>('AUTO');

  // Rol inferido del usuario
  readonly rolEfectivo = computed<'ADMIN' | 'VENDEDOR' | 'ALMACENERO' | 'CAJERO' | 'CLIENTE'>(() => {
    if (this.authService.isClient()) return 'CLIENTE';

    const seleccion = this.vistaSeleccionada();
    if (seleccion !== 'AUTO') return seleccion;

    const roles = Array.from(this.permisos.roles()).map(r => r.toUpperCase());
    if (roles.some(r => r.includes('ALMACEN') || r.includes('BODEGA'))) return 'ALMACENERO';
    if (roles.some(r => r.includes('VENDEDOR') || r.includes('COMERCIAL'))) return 'VENDEDOR';
    if (roles.some(r => r.includes('CAJA') || r.includes('CONTAD'))) return 'CAJERO';
    return 'ADMIN'; // Default para administradores o acceso total
  });

  readonly esAdminReal = computed(() => {
    const roles = Array.from(this.permisos.roles()).map(r => r.toUpperCase());
    return roles.length === 0 || roles.some(r => r.includes('ADMIN') || r.includes('SUPER') || r.includes('GEREN'));
  });

  readonly fechaActualFormateada = computed(() => {
    const fecha = new Date();
    return fecha.toLocaleDateString('es-ES', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  });

  cambiarVista(vista: RolVista): void {
    this.vistaSeleccionada.set(vista);
  }
}
