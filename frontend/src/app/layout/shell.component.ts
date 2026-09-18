import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { PermisosService } from '../core/permisos/permisos.service';
import { ITEMS_DE_MENU, ItemDeMenu } from './menu';

/**
 * Marco del área logueada: el menú de módulos, quién está dentro y la salida.
 *
 * El menú no está escrito a mano en la plantilla: se calcula filtrando el mapa de módulos
 * contra los permisos efectivos del usuario. Un cajero no ve la sección de seguridad
 * porque el filtro la descarta, no porque esté oculta con CSS.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly permisos = inject(PermisosService);
  private readonly router = inject(Router);

  readonly nombre = this.auth.nombre;
  readonly roles = this.permisos.roles;

  /** Los ítems que el usuario puede ver, según sus permisos efectivos. */
  readonly items = computed<ItemDeMenu[]>(() => {
    const permisos = this.permisos.permisos();
    return ITEMS_DE_MENU.filter((item) => item.permiso === null || permisos.has(item.permiso));
  });

  readonly cantidadDePermisos = this.permisos.cantidad;

  salir(): void {
    this.auth.logout();
    this.permisos.limpiar();
    void this.router.navigate(['/login']);
  }
}
