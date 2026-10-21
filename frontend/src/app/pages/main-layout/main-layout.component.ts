import { RolesComponent } from '../../modulos/seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/components/roles.component';
import { PermisosService } from '../../modulos/seguridad-y-auditoria/cu03-gestionar-roles-y-permisos/services/permisos.service';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../modulos/seguridad-y-auditoria/cu01-gestionar-acceso-al-sistema/services/auth.service';
import { UsuariosComponent } from '../../modulos/seguridad-y-auditoria/cu02-gestionar-usuarios/components/usuarios.component';
import { MovimientoInventarioComponent } from '../../modulos/catalogo-e-inventario/cu11-movimientos-de-inventario/components/movimiento-inventario.component';
import { CatalogoComponent } from '../../modulos/catalogo-e-inventario/cu10-catalogo-de-articulos/components/catalogo.component';
import { ClientesComponent } from '../../modulos/ventas-y-facturacion/cu13-gestionar-clientes/components/clientes.component';
import { ClientePortalComponent } from '../../modulos/ventas-y-facturacion/cu13-gestionar-clientes/components/cliente-portal.component';
import { CotizacionesComponent } from '../../modulos/ventas-y-facturacion/cu15-gestionar-cotizaciones/components/cotizaciones.component';
import { DespachosComponent } from '../../modulos/ventas-y-facturacion/cu18-gestionar-despacho-de-venta/components/despachos.component';
import { ERP_MODULES, ErpModule } from '../../core/models/erp-navigation';
import { SaasEntitlementsService } from '../../core/services/saas-entitlements.service';

import { EmpleadosComponent } from '../../modulos/seguridad-y-auditoria/cu04-gestionar-empleados/components/empleados.component';
import { DatosMaestrosInventarioComponent } from '../../modulos/catalogo-e-inventario/cu09-datos-maestros-inventario/components/datos-maestros-inventario.component';
import { PreciosYListasComponent } from '../../modulos/ventas-y-facturacion/cu14-precios-y-listas-precio/components/precios-y-listas.component';
import { NotificationBellComponent } from '../../core/components/notification-bell/notification-bell.component';
import { DashboardHomeComponent } from './components/dashboard-home/dashboard-home.component';
import { ConsultarKardexComponent } from '../../modulos/catalogo-e-inventario/cu12-consultar-kardex/components/consultar-kardex.component';
import { ValidacionComercialComponent } from '../../modulos/ventas-y-facturacion/cu17-validacion-comercial-aprobacion/components/validacion-comercial.component';
import { FacturacionComponent } from '../../modulos/ventas-y-facturacion/cu19-registrar-venta-facturacion-y-anulacion/components/facturacion.component';
import { DevolucionesComponent } from '../../modulos/ventas-y-facturacion/cu20-devoluciones-de-venta/components/devoluciones.component';

import { PedidosVentaComponent } from '../../modulos/ventas-y-facturacion/cu16-pedidos-de-venta/components/pedidos-venta.component';
import { BackupsRestoreComponent } from '../../modulos/seguridad-y-auditoria/cu07-gestionar-backups/components/backups-restore.component';
import { BitacoraTenantComponent } from '../../modulos/seguridad-y-auditoria/cu08-consultar-bitacora/components/bitacora-tenant.component';
import { PersonalizarAparienciaComponent } from '../../modulos/seguridad-y-auditoria/cu06-personalizar-apariencia/components/personalizar-apariencia.component';
import { AparienciaService } from '../../modulos/seguridad-y-auditoria/cu06-personalizar-apariencia/services/apariencia.service';
import { ReportesPredefinidosComponent } from '../../modulos/reportes-y-bi/cu37-reportes-predefinidos/components/reportes-predefinidos.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterLink, UsuariosComponent, RolesComponent, MovimientoInventarioComponent,
    CatalogoComponent, ClientesComponent, ClientePortalComponent, EmpleadosComponent, DatosMaestrosInventarioComponent,
    PreciosYListasComponent, CotizacionesComponent, PedidosVentaComponent, DespachosComponent, NotificationBellComponent, DashboardHomeComponent,
    ConsultarKardexComponent, ValidacionComercialComponent, FacturacionComponent, DevolucionesComponent,
    BackupsRestoreComponent, BitacoraTenantComponent, PersonalizarAparienciaComponent, ReportesPredefinidosComponent],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {
  readonly permisos = inject(PermisosService);
  readonly saas = inject(SaasEntitlementsService);
  readonly modules = ERP_MODULES;
  readonly authService = inject(AuthService);
  readonly apariencia = inject(AparienciaService);

  constructor() {
    this.permisos.cargar().subscribe({ error: () => this.permisos.limpiar() });
    this.saas.load().subscribe({ error: () => this.saas.clear() });
    this.apariencia.cargar().subscribe({ error: () => {} });
    effect(() => {
      const mod = this.selectedModule();
      if (mod) {
        this.expanded.update((current) => {
          if (current.has(mod.id)) return current;
          const next = new Set(current);
          next.add(mod.id);
          return next;
        });
      }
    });
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
          if (!this.saas.allows(fn.name)) return false;
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
  private readonly route = inject(ActivatedRoute);
  private readonly pathParams = toSignal(this.route.paramMap);
  private readonly queryParams = toSignal(this.route.queryParamMap);

  readonly currentFuncionSlug = computed(() => {
    return this.pathParams()?.get('funcion') || this.queryParams()?.get('funcion') || null;
  });

  readonly selectedFunction = computed(() => {
    const rawSlug = this.currentFuncionSlug();
    if (!rawSlug) return null;

    const slug = rawSlug.toLowerCase();
    const cleanSlug = slug.replace(/^cu\d+-/, '');

    for (const mod of this.visibleModules()) {
      const fn = mod.functions.find(f =>
        f.id === slug ||
        f.slug === slug ||
        f.id === cleanSlug ||
        f.slug === cleanSlug
      );
      if (fn) {
        if (this.authService.isClient() && fn.id !== 'perfil-personal') return null;
        return fn;
      }
    }
    return null;
  });

  readonly selectedModule = computed(() => {
    const fn = this.selectedFunction();
    if (!fn) return null;
    return this.visibleModules().find(mod => mod.functions.some(f => f.id === fn.id)) || null;
  });

  readonly hasSelection = computed(() => !!this.currentFuncionSlug());

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
