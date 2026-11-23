import {
  ChangeDetectionStrategy, Component, DestroyRef, OnDestroy, WritableSignal, computed, effect, inject, input, signal, untracked,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { InteligenciaNegocioService } from '../services/inteligencia-negocio.service';
import {
  Anomalia, ClientesRiesgo, Frecuencia, Lista, Opciones, Pronostico, ReglaAsociacion, Resumen, Segmentos,
  SugerenciaCompra, VistaIn,
} from '../models/inteligencia-negocio.model';
import { VISTAS, bs, indicadores, mensajeError, pct } from '../utils/ml.utils';
import { GraficoPronosticoComponent } from './grafico-pronostico.component';

/**
 * CU40 — Inteligencia de Negocio: resultados de los 6 modelos de Machine Learning (solo lectura)
 * y entrenamiento a pedido. Cada pestaña es una entrada del sidebar (`vista`).
 */
@Component({
  selector: 'app-inteligencia-negocio',
  standalone: true,
  imports: [DecimalPipe, FormsModule, RouterLink, GraficoPronosticoComponent],
  templateUrl: './inteligencia-negocio.component.html',
  styleUrl: './inteligencia-negocio.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InteligenciaNegocioComponent implements OnDestroy {
  private readonly api = inject(InteligenciaNegocioService);
  private readonly destroy = inject(DestroyRef);
  private sondeo: ReturnType<typeof setInterval> | null = null;
  private idAlEsperar: number | null = null;

  readonly vista = input<VistaIn>('panel');
  readonly vistas = VISTAS;
  readonly pct = pct;
  readonly bs = bs;
  readonly indicadores = indicadores;

  readonly resumen = signal<Resumen | null>(null);
  readonly opciones = signal<Opciones>({ sucursales: [], categorias: [] });
  readonly cargandoResumen = signal(false);
  readonly entrenando = signal(false);
  readonly mensaje = signal('');
  readonly error = signal('');

  // Datos de cada pestaña
  readonly cargando = signal(false);
  readonly errorVista = signal('');
  readonly pronostico = signal<Pronostico | null>(null);
  readonly riesgo = signal<ClientesRiesgo | null>(null);
  readonly segmentos = signal<Segmentos | null>(null);
  readonly canasta = signal<Lista<ReglaAsociacion> | null>(null);
  readonly anomalias = signal<Lista<Anomalia> | null>(null);
  readonly reposicion = signal<Lista<SugerenciaCompra> | null>(null);

  // Filtros
  frecuencia: Frecuencia = 'MENSUAL';
  filtroSerie = 'TOTAL'; // TOTAL | S:<id> | C:<categoria>
  horizonte: number | null = null;
  nivel: string | null = null;
  segmentoSel: string | null = null;
  producto = '';
  sucursalId: number | null = null;
  categoria: string | null = null;
  desde: string | null = null;
  hasta: string | null = null;

  readonly enCurso = computed(() => this.resumen()?.ultimaEjecucion?.estado === 'EN_CURSO');
  readonly totalRiesgo = computed(() => {
    const n = this.riesgo()?.porNivel ?? {};
    return (n.ALTO ?? 0) + (n.MEDIO ?? 0) + (n.BAJO ?? 0);
  });
  readonly maxSegmento = computed(() => Math.max(1, ...(this.segmentos()?.segmentos ?? []).map((s) => s.clientes)));
  readonly costoReposicion = computed(() =>
    (this.reposicion()?.items ?? []).reduce((a, s) => a + Number(s.costoEstimado || 0), 0));
  readonly totalPronosticado = computed(() =>
    (this.pronostico()?.pronostico ?? []).reduce((a, p) => a + Number(p.valor || 0), 0));

  constructor() {
    this.cargarResumen();
    this.api.opciones().pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (o) => this.opciones.set(o),
      error: () => this.opciones.set({ sucursales: [], categorias: [] }),
    });
    effect(() => {
      const v = this.vista();
      untracked(() => this.cargarVista(v));
    });
  }

  ngOnDestroy(): void {
    this.detenerSondeo();
  }

  // ------------------------------------------------------------------ resumen y entrenamiento

  cargarResumen(silencioso = false): void {
    if (!silencioso) this.cargandoResumen.set(true);
    this.api.resumen().pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (r) => {
        this.resumen.set(r);
        this.cargandoResumen.set(false);
        this.error.set('');
        const ultima = r.ultimaEjecucion;
        if (!this.sondeo && (r.entrenamientoAutomatico || ultima?.estado === 'EN_CURSO')) {
          if (r.entrenamientoAutomatico) {
            this.mensaje.set('Los resultados estaban desactualizados: se encoló un nuevo entrenamiento.');
          }
          this.iniciarSondeo();
        }
        // Terminó un entrenamiento posterior al que había al empezar a esperar.
        if (this.sondeo && ultima && ultima.estado !== 'EN_CURSO' && ultima.id !== this.idAlEsperar) {
          this.mensaje.set(`Entrenamiento terminado: ${ultima.estado}. Resultados actualizados.`);
          this.entrenando.set(false);
          this.detenerSondeo();
          this.cargarVista(this.vista());
        }
      },
      error: (e: HttpErrorResponse) => {
        this.cargandoResumen.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }

  entrenar(): void {
    this.entrenando.set(true);
    this.mensaje.set('');
    this.error.set('');
    this.api.entrenar().pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (r) => {
        this.mensaje.set(r.mensaje);
        this.iniciarSondeo();
      },
      error: (e: HttpErrorResponse) => {
        this.entrenando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }

  private iniciarSondeo(): void {
    if (this.sondeo) return;
    const ultima = this.resumen()?.ultimaEjecucion;
    // Si ya hay uno EN_CURSO, esperamos a que ESE termine; si no, a uno nuevo.
    this.idAlEsperar = ultima && ultima.estado !== 'EN_CURSO' ? ultima.id : null;
    let vueltas = 0;
    this.sondeo = setInterval(() => {
      if (++vueltas > 60) {
        this.entrenando.set(false);
        this.detenerSondeo();
        return;
      }
      this.cargarResumen(true);
    }, 5000);
  }

  private detenerSondeo(): void {
    if (this.sondeo) clearInterval(this.sondeo);
    this.sondeo = null;
  }

  // ------------------------------------------------------------------ pestañas

  cargarVista(v: VistaIn): void {
    this.errorVista.set('');
    switch (v) {
      case 'pronostico': {
        const [tipo, valor] = this.filtroSerie === 'TOTAL' ? ['TOTAL', ''] : [this.filtroSerie[0], this.filtroSerie.slice(2)];
        this.pedir(this.api.pronostico(this.frecuencia, tipo === 'S' ? Number(valor) : null,
          tipo === 'C' ? valor : null, this.horizonte), this.pronostico);
        break;
      }
      case 'abandono':
        this.pedir(this.api.clientesRiesgo(this.nivel), this.riesgo);
        break;
      case 'segmentacion':
        this.pedir(this.api.segmentos(this.segmentoSel), this.segmentos);
        break;
      case 'canasta':
        this.pedir(this.api.canasta(this.producto.trim() || null), this.canasta);
        break;
      case 'anomalias':
        this.pedir(this.api.anomalias(this.desde || null, this.hasta || null, this.sucursalId), this.anomalias);
        break;
      case 'reposicion':
        this.pedir(this.api.sugerenciaCompra(this.sucursalId, this.categoria), this.reposicion);
        break;
      default:
        break;
    }
  }

  private pedir<T>(obs: Observable<T>, destino: WritableSignal<T | null>): void {
    this.cargando.set(true);
    obs.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (d) => {
        destino.set(d);
        this.cargando.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorVista.set(mensajeError(e));
      },
    });
  }

  recargar(): void {
    this.cargarVista(this.vista());
  }

  filtrarNivel(nivel: string | null): void {
    this.nivel = this.nivel === nivel ? null : nivel;
    this.recargar();
  }

  filtrarSegmento(segmento: string | null): void {
    this.segmentoSel = this.segmentoSel === segmento ? null : segmento;
    this.recargar();
  }

  limpiarFiltros(): void {
    this.sucursalId = null;
    this.categoria = null;
    this.desde = null;
    this.hasta = null;
    this.producto = '';
    this.recargar();
  }

  nombreSerie(p: Pronostico | null): string {
    if (!p || p.ambito === 'TOTAL') return 'Toda la empresa';
    if (p.ambito === 'CATEGORIA') return `Categoría ${p.clave}`;
    return this.opciones().sucursales.find((s) => String(s.id) === p.clave)?.nombre ?? `Sucursal ${p.clave}`;
  }

  fecha(iso: string | null | undefined): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('es-BO', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  diaSemana(iso: string): string {
    const [a, m, d] = iso.split('-').map(Number);
    return new Date(a, m - 1, d).toLocaleDateString('es-BO', { weekday: 'long' });
  }
}
