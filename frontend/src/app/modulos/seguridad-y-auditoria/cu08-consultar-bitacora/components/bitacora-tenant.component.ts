import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BitacoraService, BitacoraEvento, VerificacionCadena, CloudWatchStatus } from '../services/bitacora.service';

@Component({
  selector: 'app-bitacora-tenant',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './bitacora-tenant.component.html',
  styleUrl: './bitacora-tenant.component.scss'
})
export class BitacoraTenantComponent implements OnInit {
  private readonly bitacoraService = inject(BitacoraService);

  readonly eventos = signal<BitacoraEvento[]>([]);
  readonly cargando = signal<boolean>(false);
  readonly verificando = signal<boolean>(false);
  readonly exportando = signal<boolean>(false);

  // Filtros
  readonly filtroTexto = signal<string>('');
  readonly filtroModulo = signal<string>('TODOS');
  readonly filtroSeveridad = signal<string>('TODAS');
  readonly filtroResultado = signal<string>('TODOS');
  readonly filtroDesde = signal<string>('');
  readonly filtroHasta = signal<string>('');

  // Estados de verificación y modales
  readonly resultadoVerificacion = signal<VerificacionCadena | null>(null);
  readonly cloudwatchStatus = signal<CloudWatchStatus | null>(null);
  readonly eventoDetalle = signal<BitacoraEvento | null>(null);

  readonly eventosFiltrados = computed(() => {
    const txt = this.filtroTexto().toLowerCase().trim();
    const mod = this.filtroModulo();
    const sev = this.filtroSeveridad();
    const res = this.filtroResultado();

    return this.eventos().filter(e => {
      const matchMod = mod === 'TODOS' || e.modulo === mod;
      const matchSev = sev === 'TODAS' || (e.severidad || 'MEDIA') === sev;
      const matchRes = res === 'TODOS' || e.resultado === res;
      const matchTxt = !txt ||
        e.tipo_operacion?.toLowerCase().includes(txt) ||
        e.actor?.toLowerCase().includes(txt) ||
        e.detalle?.toLowerCase().includes(txt) ||
        e.ip_origen?.includes(txt) ||
        e.evento_id?.toLowerCase().includes(txt);

      return matchMod && matchSev && matchRes && matchTxt;
    });
  });

  readonly totalEventos = computed(() => this.eventos().length);
  readonly totalCompletados = computed(() => this.eventos().filter(e => e.resultado === 'COMPLETADO' || e.resultado === 'EXITO' || e.resultado === 'VERIFICADO').length);
  readonly totalFallidos = computed(() => this.eventos().filter(e => e.resultado === 'FALLIDO' || e.resultado === 'RECHAZADO').length);
  readonly totalCloudWatch = computed(() => this.eventos().filter(e => e.cloudwatch_ingested).length);

  ngOnInit(): void {
    this.cargarEventos();
    this.consultarCloudWatch();
  }

  cargarEventos(): void {
    this.cargando.set(true);
    this.bitacoraService.listarEventosTenant({
      modulo: this.filtroModulo(),
      severidad: this.filtroSeveridad(),
      resultado: this.filtroResultado(),
      busqueda: this.filtroTexto(),
      desde: this.filtroDesde() || undefined,
      hasta: this.filtroHasta() || undefined
    }).subscribe({
      next: (data) => {
        this.eventos.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
      }
    });
  }

  consultarCloudWatch(): void {
    this.bitacoraService.obtenerEstadoCloudWatch().subscribe({
      next: (st) => this.cloudwatchStatus.set(st),
      error: () => this.cloudwatchStatus.set(null)
    });
  }

  verificarCadena(): void {
    this.verificando.set(true);
    this.bitacoraService.verificarIntegridadCadena().subscribe({
      next: (res) => {
        this.resultadoVerificacion.set(res);
        this.verificando.set(false);
      },
      error: () => {
        this.verificando.set(false);
      }
    });
  }

  exportarAuditoria(): void {
    this.exportando.set(true);
    this.bitacoraService.exportarAuditoria().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `auditoria_inmutable_iso27001_${new Date().toISOString().slice(0, 10)}.json`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.exportando.set(false);
      },
      error: () => {
        this.exportando.set(false);
      }
    });
  }

  limpiarFiltros(): void {
    this.filtroTexto.set('');
    this.filtroModulo.set('TODOS');
    this.filtroSeveridad.set('TODAS');
    this.filtroResultado.set('TODOS');
    this.filtroDesde.set('');
    this.filtroHasta.set('');
    this.cargarEventos();
  }

  verDetalle(e: BitacoraEvento): void {
    this.eventoDetalle.set(e);
  }

  cerrarDetalle(): void {
    this.eventoDetalle.set(null);
  }

  copiarTexto(txt: string): void {
    navigator.clipboard.writeText(txt);
  }
}
