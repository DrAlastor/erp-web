import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BackupsService, BackupItem, RestauracionItem, PoliticaBackup, VerificacionIntegridad } from '../services/backups.service';

@Component({
  selector: 'app-backups-restore',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './backups-restore.component.html',
  styleUrl: './backups-restore.component.scss'
})
export class BackupsRestoreComponent implements OnInit {
  private readonly backupsService = inject(BackupsService);

  readonly tabActiva = signal<'backups' | 'restauraciones' | 'politica'>('backups');
  readonly cargando = signal<boolean>(false);
  readonly guardando = signal<boolean>(false);
  readonly mensaje = signal<{ tipo: 'exito' | 'error' | 'info'; texto: string } | null>(null);

  // Datos
  readonly backups = signal<BackupItem[]>([]);
  readonly restauraciones = signal<RestauracionItem[]>([]);
  readonly politica = signal<PoliticaBackup | null>(null);
  readonly tablasDisponibles = signal<string[]>([]);

  // Modales
  readonly modalCrearBackup = signal<boolean>(false);
  readonly modalRestaurarCompleta = signal<boolean>(false);
  readonly modalRestaurarSelectiva = signal<boolean>(false);
  readonly modalPolitica = signal<boolean>(false);
  readonly modalDetalleHash = signal<BackupItem | null>(null);
  readonly hashAComparar = signal<string>('');

  readonly coincidenciaHash = computed(() => {
    const input = this.hashAComparar().trim().toLowerCase();
    const target = this.modalDetalleHash()?.checksumSha256?.trim().toLowerCase();
    if (!input || !target) return null;
    return input === target;
  });

  // Selecciones para restauración
  readonly backupSeleccionado = signal<BackupItem | null>(null);
  readonly tablasSeleccionadas = signal<Set<string>>(new Set());
  readonly motivoRestauracion = signal<string>('');
  readonly confirmacionTexto = signal<string>('');

  // Formulario de Política
  formPolitica: PoliticaBackup = {
    empresaId: 0,
    frecuencia: 'DIARIO',
    horaEjecucion: '02:00',
    diasRetencion: 30,
    activo: true,
    notificarEmail: ''
  };

  // KPIs computados
  readonly totalPuntos = computed(() => this.backups().filter(b => b.estado === 'COMPLETADO').length);
  readonly ultimoBackup = computed(() => {
    const list = this.backups().filter(b => b.estado === 'COMPLETADO');
    return list.length > 0 ? list[0] : null;
  });
  readonly totalRestauraciones = computed(() => this.restauraciones().length);

  ngOnInit(): void {
    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando.set(true);
    this.backupsService.listarBackups().subscribe({
      next: (data) => {
        this.backups.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        this.mostrarMensaje('error', 'Error al cargar historial de respaldos: ' + (err.error?.message || err.message));
        this.cargando.set(false);
      }
    });

    this.backupsService.listarRestauraciones().subscribe({
      next: (data) => this.restauraciones.set(data),
      error: () => {}
    });

    this.backupsService.obtenerPolitica().subscribe({
      next: (data) => {
        this.politica.set(data);
        this.formPolitica = { ...data };
      },
      error: () => {}
    });

    this.backupsService.obtenerTablasDisponibles().subscribe({
      next: (data) => this.tablasDisponibles.set(data),
      error: () => {}
    });
  }

  cambiarTab(tab: 'backups' | 'restauraciones' | 'politica'): void {
    this.tabActiva.set(tab);
    if (tab === 'restauraciones') {
      this.backupsService.listarRestauraciones().subscribe(data => this.restauraciones.set(data));
    }
  }

  abrirCrearBackup(): void {
    this.modalCrearBackup.set(true);
  }

  cerrarCrearBackup(): void {
    this.modalCrearBackup.set(false);
  }

  ejecutarCrearBackup(tipo: string = 'BAJO_DEMANDA'): void {
    this.guardando.set(true);
    this.mostrarMensaje('info', 'Generando respaldo lógico de la base de datos... Por favor espere.');
    this.cerrarCrearBackup();

    this.backupsService.crearBackup(tipo).subscribe({
      next: (nuevo) => {
        this.guardando.set(false);
        this.mostrarMensaje('exito', `Respaldo '${nuevo.nombreArchivo}' generado y verificado con éxito (SHA-256).`);
        this.cargarDatos();
      },
      error: (err) => {
        this.guardando.set(false);
        this.mostrarMensaje('error', 'Fallo al generar respaldo: ' + (err.error?.message || err.message));
      }
    });
  }

  verificarIntegridad(b: BackupItem): void {
    this.mostrarMensaje('info', `Verificando firma SHA-256 de ${b.nombreArchivo}...`);
    this.backupsService.verificarIntegridad(b.id).subscribe({
      next: (res: VerificacionIntegridad) => {
        if (res.integro) {
          this.mostrarMensaje('exito', `Integridad confirmada para ${b.nombreArchivo}. SHA-256 verificado.`);
        } else {
          this.mostrarMensaje('error', `ALERTA: El respaldo ${b.nombreArchivo} no coincide con el checksum esperado. Puede estar corrupto.`);
        }
        this.cargarDatos();
      },
      error: (err) => {
        this.mostrarMensaje('error', 'Error al verificar integridad: ' + (err.error?.message || err.message));
      }
    });
  }

  descargarBackup(b: BackupItem): void {
    this.backupsService.descargarArchivo(b.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = b.nombreArchivo;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        this.mostrarMensaje('error', 'Error al descargar respaldo: ' + (err.error?.message || err.message));
      }
    });
  }

  // Restauración Completa
  abrirRestaurarCompleta(b: BackupItem): void {
    this.backupSeleccionado.set(b);
    this.motivoRestauracion.set('');
    this.confirmacionTexto.set('');
    this.modalRestaurarCompleta.set(true);
  }

  cerrarRestaurarCompleta(): void {
    this.modalRestaurarCompleta.set(false);
  }

  ejecutarRestaurarCompleta(): void {
    const b = this.backupSeleccionado();
    if (!b) return;

    if (this.confirmacionTexto().trim().toUpperCase() !== 'CONFIRMAR') {
      this.mostrarMensaje('error', 'Debe escribir "CONFIRMAR" para autorizar la restauración completa.');
      return;
    }

    this.guardando.set(true);
    this.cerrarRestaurarCompleta();
    this.mostrarMensaje('info', `Iniciando restauración completa desde '${b.nombreArchivo}'. Creando snapshot de reversión previo...`);

    this.backupsService.restaurarCompleta(b.id, this.motivoRestauracion()).subscribe({
      next: (res) => {
        this.guardando.set(false);
        this.mostrarMensaje('exito', `Restauración completa finalizada con éxito. ID de operación: ${res.id}`);
        this.cargarDatos();
        this.cambiarTab('restauraciones');
      },
      error: (err) => {
        this.guardando.set(false);
        this.mostrarMensaje('error', 'Error durante la restauración: ' + (err.error?.message || err.message));
        this.cargarDatos();
      }
    });
  }

  // Restauración Selectiva
  abrirRestaurarSelectiva(b: BackupItem): void {
    this.backupSeleccionado.set(b);
    this.tablasSeleccionadas.set(new Set(['clientes', 'productos']));
    this.motivoRestauracion.set('');
    this.modalRestaurarSelectiva.set(true);
  }

  cerrarRestaurarSelectiva(): void {
    this.modalRestaurarSelectiva.set(false);
  }

  toggleTabla(tabla: string): void {
    const current = new Set(this.tablasSeleccionadas());
    if (current.has(tabla)) {
      current.delete(tabla);
    } else {
      current.add(tabla);
    }
    this.tablasSeleccionadas.set(current);
  }

  seleccionarTodasTablas(): void {
    this.tablasSeleccionadas.set(new Set(this.tablasDisponibles()));
  }

  deseleccionarTodasTablas(): void {
    this.tablasSeleccionadas.set(new Set());
  }

  ejecutarRestaurarSelectiva(): void {
    const b = this.backupSeleccionado();
    if (!b) return;

    const tablas = Array.from(this.tablasSeleccionadas());
    if (tablas.length === 0) {
      this.mostrarMensaje('error', 'Debe seleccionar al menos una tabla para la restauración selectiva.');
      return;
    }

    this.guardando.set(true);
    this.cerrarRestaurarSelectiva();
    this.mostrarMensaje('info', `Restaurando tablas [${tablas.join(', ')}] desde '${b.nombreArchivo}'...`);

    this.backupsService.restaurarSelectiva(b.id, tablas, this.motivoRestauracion()).subscribe({
      next: (res) => {
        this.guardando.set(false);
        this.mostrarMensaje('exito', `Restauración selectiva completada con éxito. Tablas recuperadas: ${res.tablasSeleccionadas}`);
        this.cargarDatos();
        this.cambiarTab('restauraciones');
      },
      error: (err) => {
        this.guardando.set(false);
        this.mostrarMensaje('error', 'Error en restauración selectiva: ' + (err.error?.message || err.message));
        this.cargarDatos();
      }
    });
  }

  // Política
  abrirModalPolitica(): void {
    if (this.politica()) {
      this.formPolitica = { ...this.politica()! };
    }
    this.modalPolitica.set(true);
  }

  cerrarModalPolitica(): void {
    this.modalPolitica.set(false);
  }

  guardarPolitica(): void {
    this.guardando.set(true);
    this.backupsService.guardarPolitica(this.formPolitica).subscribe({
      next: (actualizada) => {
        this.politica.set(actualizada);
        this.guardando.set(false);
        this.cerrarModalPolitica();
        this.mostrarMensaje('exito', 'Política de respaldos automáticos actualizada.');
      },
      error: (err) => {
        this.guardando.set(false);
        this.mostrarMensaje('error', 'Error al guardar política: ' + (err.error?.message || err.message));
      }
    });
  }

  verHashCompleto(b: BackupItem): void {
    this.hashAComparar.set('');
    this.modalDetalleHash.set(b);
  }

  cerrarModalHash(): void {
    this.modalDetalleHash.set(null);
  }

  copiarAlPortapapeles(texto: string): void {
    navigator.clipboard.writeText(texto).then(() => {
      this.mostrarMensaje('info', 'Copiado al portapapeles: ' + texto.substring(0, 16) + '...');
    });
  }

  mostrarMensaje(tipo: 'exito' | 'error' | 'info', texto: string): void {
    this.mensaje.set({ tipo, texto });
    setTimeout(() => {
      if (this.mensaje()?.texto === texto) {
        this.mensaje.set(null);
      }
    }, 6000);
  }
}
