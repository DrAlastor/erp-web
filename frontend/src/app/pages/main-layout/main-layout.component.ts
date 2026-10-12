import { RolesComponent } from '../../modulos/seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/components/roles.component';
import { PermisosService } from '../../modulos/seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../modulos/seguridad-y-auditoria/cu01-gestionar-acceso-al-sistema/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';
import { UsuariosComponent } from '../../modulos/seguridad-y-auditoria/cu02-gestionar-usuarios/components/usuarios.component';
import { MovimientoInventarioComponent } from '../../modulos/catalogo-e-inventario/cu11-movimientos-de-inventario/components/movimiento-inventario.component';
import { CatalogoComponent } from '../../modulos/catalogo-e-inventario/cu10-catalogo-de-articulos/components/catalogo.component';
import { ClientesComponent } from '../../modulos/ventas-y-facturacion/cu13-gestionar-clientes/components/clientes.component';
import { ClientePortalComponent } from '../../modulos/ventas-y-facturacion/cu13-gestionar-clientes/components/cliente-portal.component';
import { ERP_MODULES, ErpModule } from '../../core/models/erp-navigation';

import { EmpleadosComponent } from '../../modulos/seguridad-y-auditoria/cu04-gestionar-empleados/components/empleados.component';
import { DatosMaestrosInventarioComponent } from '../../modulos/catalogo-e-inventario/cu09-datos-maestros-inventario/components/datos-maestros-inventario.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterLink, UsuariosComponent, RolesComponent, MovimientoInventarioComponent,
    CatalogoComponent, ClientesComponent, ClientePortalComponent, EmpleadosComponent, DatosMaestrosInventarioComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {
  readonly permisos = inject(PermisosService);
  readonly modules = ERP_MODULES;
  readonly authService = inject(AuthService);
  readonly themeService = inject(ThemeService);

  constructor() {
    this.permisos.cargar().subscribe({ error: () => this.permisos.limpiar() });
  }

  // Módulos visibles filtrados según el rol del usuario conectado
  readonly visibleModules = computed<ErpModule[]>(() => {
    const roles = Array.from(this.permisos.roles());
    // Si aún no cargó o es admin/superadmin, mostrar completo
    const esAdmin = roles.length === 0 || roles.some(r => {
      const upper = r.toUpperCase();
      return upper.includes('ADMIN') || upper.includes('SUPER') || upper.includes('GEREN');
    });

    return this.modules
      .map(module => {
        const functions = module.functions.filter(fn => {
          if (esAdmin) return true;
          return fn.rolesPermitidos?.some(rp => roles.includes(rp)) ?? false;
        });
        return { ...module, functions };
      })
      .filter(module => module.functions.length > 0);
  });

  readonly functionCount = computed(() =>
    this.visibleModules().reduce((count, module) => count + module.functions.length, 0)
  );

  readonly sidebarOpen = signal(false);
  readonly expanded = signal(new Set<string>());
  private readonly params = toSignal(inject(ActivatedRoute).queryParamMap);

  readonly selectedModule = computed(() =>
    this.visibleModules().find((module) => module.id === this.params()?.get('modulo')),
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
