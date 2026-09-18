import { RolesComponent } from '../../modulos/seguridad-y-auditoria/roles-y-permisos/components/roles.component';
import { PermisosService } from '../../modulos/seguridad-y-auditoria/roles-y-permisos/services/permisos.service';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../modulos/seguridad-y-auditoria/acceso-al-sistema/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';
import { UsuariosComponent } from '../../modulos/seguridad-y-auditoria/gestion-de-usuarios/components/usuarios.component';
import { MovimientoInventarioComponent } from '../../modulos/inventario-y-almacenes/movimientos-de-inventario/components/movimiento-inventario.component';
import { ClientesComponent } from '../../modulos/comercial-y-preventa/gestion-de-clientes/components/clientes.component';
import { ClientePortalComponent } from '../../modulos/comercial-y-preventa/gestion-de-clientes/components/cliente-portal.component';
import { ERP_MODULES } from '../../core/models/erp-navigation';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterLink, UsuariosComponent, RolesComponent, MovimientoInventarioComponent,
    ClientesComponent, ClientePortalComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {
  readonly permisos = inject(PermisosService);
  readonly modules = ERP_MODULES;
  constructor() { this.permisos.cargar().subscribe({ error: () => this.permisos.limpiar() }); }
  readonly functionCount = ERP_MODULES.reduce(
    (count, module) => count + module.functions.length,
    0,
  );
  readonly sidebarOpen = signal(false);
  readonly expanded = signal(new Set<string>());
  readonly authService = inject(AuthService);
  readonly themeService = inject(ThemeService);
  private readonly params = toSignal(inject(ActivatedRoute).queryParamMap);
  readonly selectedModule = computed(() =>
    this.modules.find((module) => module.id === this.params()?.get('modulo')),
  );
  readonly selectedFunction = computed(() =>
    this.selectedModule()?.functions.find((fn) => {
      const selected = fn.id === this.params()?.get('funcion');
      return selected && (!this.authService.isClient() || fn.id === 'perfil-personal');
    }),
  );
  readonly hasSelection = computed(
    () => !!this.params()?.get('modulo') || !!this.params()?.get('funcion'),
  );
  toggleModule(id: string): void {
    this.expanded.update((current) => {
      const next = new Set(current);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }
  iniciales(name: string | undefined): string {
    return (
      name
        ?.split(' ')
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0].toUpperCase())
        .join('') || 'US'
    );
  }
}
