import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { 
  SuperAdminService, 
  TenantMonitorItem, 
  BackupItem, 
  BitacoraItem,
  BitacoraInmutableItem 
} from '../../../core/services/superadmin.service';

@Component({
  selector: 'app-superadmin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './superadmin-dashboard.component.html',
  styleUrl: './superadmin-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SuperadminDashboardComponent implements OnInit {
  readonly superAdminService = inject(SuperAdminService);
  private readonly router = inject(Router);

  readonly activeTab = signal<'tenants' | 'backups' | 'bitacora'>('tenants');
  
  readonly tenants = signal<TenantMonitorItem[]>([]);
  readonly backups = signal<BackupItem[]>([]);
  readonly bitacora = signal<BitacoraItem[]>([]);
  readonly bitacoraInmutable = signal<BitacoraInmutableItem[]>([]);
  readonly verificandoCadena = signal<boolean>(false);
  readonly resultadoCadena = signal<{ valida: boolean; mensaje: string } | null>(null);

  readonly loading = signal<boolean>(false);
  readonly actionLoadingId = signal<number | null>(null);
  readonly notification = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  // Filtros
  readonly searchTerm = signal<string>('');
  readonly selectedEmpresaForBackup = signal<number | null>(null);

  // Filtros Bitácora Global
  readonly bitacoraTenantFilter = signal<number | null>(null);
  readonly bitacoraModuloFilter = signal<string>('TODOS');
  readonly bitacoraSeveridadFilter = signal<string>('TODAS');
  readonly bitacoraSearchTerm = signal<string>('');
  readonly cloudwatchStatus = signal<any>(null);

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.loading.set(true);
    this.superAdminService.listarTenants().subscribe({
      next: (data) => {
        this.tenants.set(data);
        if (data.length > 0 && !this.selectedEmpresaForBackup()) {
          this.selectedEmpresaForBackup.set(data[0].id);
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.showNotification('error', 'Error al cargar lista de tenants: ' + (err.error?.message || err.message));
      }
    });

    this.cargarBackups();
    this.cargarBitacora();
  }

  cargarBackups(): void {
    this.superAdminService.listarBackups().subscribe({
      next: (data) => this.backups.set(data),
      error: () => {}
    });
  }

  cargarBitacora(): void {
    this.superAdminService.obtenerEstadoCloudWatch().subscribe({
      next: (st) => this.cloudwatchStatus.set(st),
      error: () => this.cloudwatchStatus.set(null)
    });
    this.superAdminService.listarBitacora().subscribe({
      next: (data) => this.bitacora.set(data),
      error: () => {}
    });
    this.superAdminService.listarBitacoraInmutable({
      empresaId: this.bitacoraTenantFilter() || undefined,
      modulo: this.bitacoraModuloFilter(),
      severidad: this.bitacoraSeveridadFilter(),
      busqueda: this.bitacoraSearchTerm()
    }).subscribe({
      next: (data) => this.bitacoraInmutable.set(data),
      error: () => {}
    });
  }


  setTab(tab: 'tenants' | 'backups' | 'bitacora'): void {
    this.activeTab.set(tab);
    if (tab === 'backups') {
      this.cargarBackups();
    } else if (tab === 'bitacora') {
      this.cargarBitacora();
    }
  }

  verificarIntegridad(b: BackupItem): void {
    this.showNotification('success', `Verificando firma SHA-256 de ${b.nombreArchivo}...`);
    this.superAdminService.verificarIntegridadBackup(b.id).subscribe({
      next: (res) => {
        if (res.integro) {
          this.showNotification('success', `Integridad confirmada: ${b.nombreArchivo} validado con SHA-256.`);
        } else {
          this.showNotification('error', `Alerta: Checksum no coincide para ${b.nombreArchivo}.`);
        }
        this.cargarBackups();
      },
      error: (err) => {
        this.showNotification('error', 'Error verificando integridad: ' + (err.error?.message || err.message));
      }
    });
  }

  restaurarBackup(b: BackupItem): void {
    const confirmacion = prompt(`¿Está seguro de restaurar el respaldo '${b.nombreArchivo}' para '${b.razonSocial}'? Escriba 'CONFIRMAR' para proceder:`);
    if (confirmacion !== 'CONFIRMAR') {
      return;
    }
    this.showNotification('success', `Iniciando restauración de '${b.nombreArchivo}' con snapshot de reversión previo...`);
    this.superAdminService.restaurarBackup(b.id, 'Restauración autorizada por Super Admin').subscribe({
      next: (res) => {
        this.showNotification('success', `Restauración ejecutada con éxito (ID: ${res.id}).`);
        this.cargarBackups();
        this.cargarBitacora();
      },
      error: (err) => {
        this.showNotification('error', 'Error al restaurar: ' + (err.error?.message || err.message));
      }
    });
  }

  verificarCadenaBitacora(): void {
    this.verificandoCadena.set(true);
    this.superAdminService.verificarCadenaBitacora().subscribe({
      next: (res) => {
        this.resultadoCadena.set(res);
        this.verificandoCadena.set(false);
      },
      error: () => {
        this.verificandoCadena.set(false);
      }
    });
  }

  suspenderTenant(t: TenantMonitorItem): void {
    if (!confirm(`¿Está seguro de suspender el tenant '${t.razonSocial}' (${t.codigoEmpresa})? Los usuarios de esta empresa no podrán operar.`)) {
      return;
    }
    this.actionLoadingId.set(t.id);
    this.superAdminService.suspenderTenant(t.id).subscribe({
      next: () => {
        this.actionLoadingId.set(null);
        this.showNotification('success', `Tenant ${t.codigoEmpresa} suspendido correctamente.`);
        this.cargarDatos();
      },
      error: (err) => {
        this.actionLoadingId.set(null);
        this.showNotification('error', 'No se pudo suspender la empresa: ' + (err.error?.message || err.message));
      }
    });
  }

  reactivarTenant(t: TenantMonitorItem): void {
    this.actionLoadingId.set(t.id);
    this.superAdminService.reactivarTenant(t.id).subscribe({
      next: () => {
        this.actionLoadingId.set(null);
        this.showNotification('success', `Tenant ${t.codigoEmpresa} reactivado con éxito.`);
        this.cargarDatos();
      },
      error: (err) => {
        this.actionLoadingId.set(null);
        this.showNotification('error', 'No se pudo reactivar la empresa: ' + (err.error?.message || err.message));
      }
    });
  }

  generarBackup(empresaId: number): void {
    const t = this.tenants().find(item => item.id === empresaId);
    const nombre = t ? t.razonSocial : `Empresa #${empresaId}`;

    this.actionLoadingId.set(empresaId);
    this.showNotification('success', `Iniciando volcado físico y registro de backup para '${nombre}'...`);

    this.superAdminService.generarBackup(empresaId).subscribe({
      next: (res) => {
        this.actionLoadingId.set(null);
        this.showNotification('success', `Copia de seguridad completada con éxito: ${res.nombreArchivo} (${this.formatBytes(res.tamanoBytes)})`);
        this.cargarBackups();
        this.cargarBitacora();
      },
      error: (err) => {
        this.actionLoadingId.set(null);
        this.showNotification('error', 'Error al generar copia de seguridad: ' + (err.error?.message || err.message));
      }
    });
  }

  logout(): void {
    this.superAdminService.logout();
  }

  showNotification(type: 'success' | 'error', message: string): void {
    this.notification.set({ type, message });
    setTimeout(() => {
      if (this.notification()?.message === message) {
        this.notification.set(null);
      }
    }, 6000);
  }

  formatBytes(bytes: number): string {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  get filteredTenants(): TenantMonitorItem[] {
    const term = this.searchTerm().trim().toLowerCase();
    if (!term) return this.tenants();
    return this.tenants().filter(t => 
      t.razonSocial.toLowerCase().includes(term) ||
      t.codigoEmpresa.toLowerCase().includes(term) ||
      (t.nombreDatabase && t.nombreDatabase.toLowerCase().includes(term)) ||
      (t.nit && t.nit.toLowerCase().includes(term)) ||
      (t.email && t.email.toLowerCase().includes(term))
    );
  }

  get totalTenants(): number {
    return this.tenants().length;
  }

  get activeTenantsCount(): number {
    return this.tenants().filter(t => t.estadoEmpresa === 'ACTIVA').length;
  }

  get suspendedTenantsCount(): number {
    return this.tenants().filter(t => t.estadoEmpresa === 'SUSPENDIDA').length;
  }

  get totalBackupsCount(): number {
    return this.backups().length;
  }

  get totalBackupStorageBytes(): number {
    return this.backups().reduce((acc, b) => acc + (b.tamanoBytes || 0), 0);
  }
}
