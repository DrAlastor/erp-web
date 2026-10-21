import {
  ChangeDetectionStrategy, Component, DestroyRef, LOCALE_ID, OnInit, computed, effect, inject, signal, untracked
} from '@angular/core';
import { CommonModule, formatNumber } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable } from 'rxjs';
import { ReporteVozService } from '../services/reporte-voz.service';
import { ReconocimientoVozService } from '../services/reconocimiento-voz.service';
import {
  AgrupacionVoz,
  ColumnaReporte,
  ConsultaReporteVoz,
  MetricaOpcion,
  MetricaVoz,
  OpcionesReporteVoz,
  ReporteVoz,
  TipoDato
} from '../models/reporte-voz.model';

interface Barra {
  etiqueta: string;
  valor: number;
  texto: string;
  porcentaje: number;
}

interface PuntoLinea {
  x: number;
  y: number;
  etiqueta: string;
  texto: string;
}

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
const ANCHO = 640;
const ALTO = 240;
const MARGEN = { izq: 16, der: 16, sup: 20, inf: 32 };

@Component({
  selector: 'app-reportes-por-voz',
  standalone: true,
  imports: [CommonModule, FormsModule],
  providers: [ReconocimientoVozService],
  templateUrl: './reportes-por-voz.component.html',
  styleUrl: './reportes-por-voz.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReportesPorVozComponent implements OnInit {
  private readonly api = inject(ReporteVozService);
  private readonly destroy = inject(DestroyRef);
  private readonly locale = inject(LOCALE_ID);
  readonly voz = inject(ReconocimientoVozService);

  readonly opciones = signal<OpcionesReporteVoz | null>(null);
  readonly reporte = signal<ReporteVoz | null>(null);
  readonly loading = signal(false);
  readonly exportando = signal(false);
  readonly error = signal('');
  readonly vista = signal<'grafico' | 'tabla'>('grafico');
  readonly medida = signal('');
  readonly refinando = signal(false);

  /** Texto a enviar: lo dictado, que el usuario puede corregir antes de confirmar. */
  readonly texto = signal('');

  // Formulario de refinamiento
  fMetrica: MetricaVoz = 'VENTAS';
  fAgrupacion: AgrupacionVoz = 'NINGUNA';
  fFechaInicio = '';
  fFechaFin = '';
  fSucursalId: number | '' = '';
  fProductoId: number | '' = '';
  fSoloCriticos = false;
  fLimite = 10;

  readonly ancho = ANCHO;
  readonly alto = ALTO;

  readonly medidas = computed<ColumnaReporte[]>(() =>
    (this.reporte()?.columnas ?? []).filter(c => c.tipo === 'NUMERO' || c.tipo === 'MONEDA'));

  readonly medidaActual = computed(() =>
    this.medidas().find(m => m.clave === this.medida()) ?? this.medidas()[0] ?? null);

  readonly barras = computed<Barra[]>(() => {
    const r = this.reporte();
    const m = this.medidaActual();
    if (!r || !m) return [];
    const valores = r.filas.map(f => Number(f[m.clave] ?? 0));
    const maximo = Math.max(...valores.map(Math.abs), 0) || 1;
    return r.filas.map((f, i) => ({
      etiqueta: this.etiquetaGrupo(f['grupo']),
      valor: valores[i],
      texto: this.formatear(valores[i], m.tipo),
      porcentaje: Math.max((Math.abs(valores[i]) / maximo) * 100, valores[i] !== 0 ? 1.5 : 0)
    }));
  });

  readonly puntos = computed<PuntoLinea[]>(() => {
    const barras = this.barras();
    if (barras.length === 0) return [];
    const valores = barras.map(b => b.valor);
    const minimo = Math.min(0, ...valores);
    const maximo = Math.max(...valores);
    const rango = maximo - minimo || 1;
    const anchoUtil = ANCHO - MARGEN.izq - MARGEN.der;
    const altoUtil = ALTO - MARGEN.sup - MARGEN.inf;
    const paso = barras.length > 1 ? anchoUtil / (barras.length - 1) : 0;
    return barras.map((b, i) => ({
      x: MARGEN.izq + (barras.length > 1 ? i * paso : anchoUtil / 2),
      y: MARGEN.sup + altoUtil - ((b.valor - minimo) / rango) * altoUtil,
      etiqueta: b.etiqueta,
      texto: b.texto
    }));
  });

  readonly polilinea = computed(() => this.puntos().map(p => `${p.x},${p.y}`).join(' '));

  readonly area = computed(() => {
    const p = this.puntos();
    if (p.length < 2) return '';
    const base = ALTO - MARGEN.inf;
    return `M${p[0].x},${base} ` + p.map(q => `L${q.x},${q.y}`).join(' ') + ` L${p[p.length - 1].x},${base} Z`;
  });

  /** Solo algunas etiquetas del eje X para que no se encimen. */
  readonly etiquetasEje = computed(() => {
    const p = this.puntos();
    if (p.length <= 6) return p;
    const cada = Math.ceil(p.length / 6);
    return p.filter((_, i) => i % cada === 0 || i === p.length - 1);
  });

  constructor() {
    // Lo que se va transcribiendo pasa al cuadro de texto para que el usuario lo confirme.
    effect(() => {
      const grabando = this.voz.grabando();
      const transcrito = this.voz.transcripcion();
      if (grabando || transcrito) {
        untracked(() => this.texto.set(transcrito));
      }
    });
  }

  ngOnInit(): void {
    this.api.opciones()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (o) => this.opciones.set(o),
        error: () => this.opciones.set(null)
      });
  }

  alternarMicrofono(): void {
    if (this.voz.grabando()) {
      this.voz.detener();
    } else {
      this.error.set('');
      this.voz.iniciar();
    }
  }

  usarEjemplo(ejemplo: string): void {
    this.texto.set(ejemplo);
    this.generar();
  }

  /** HU39.2: envía el texto confirmado al backend, que lo hace interpretar por N8N. */
  generar(): void {
    const frase = this.texto().trim();
    if (!frase) {
      this.error.set('Dicta o escribe la consulta que deseas realizar.');
      return;
    }
    if (this.voz.grabando()) this.voz.detener();
    this.cargar(this.api.interpretar(frase));
  }

  abrirRefinar(): void {
    const c = this.reporte()?.consulta;
    if (c) {
      this.fMetrica = c.metrica;
      this.fAgrupacion = c.agrupacion ?? 'NINGUNA';
      this.fFechaInicio = c.fechaInicio ?? '';
      this.fFechaFin = c.fechaFin ?? '';
      this.fSucursalId = c.sucursalId ?? '';
      this.fProductoId = c.productoId ?? '';
      this.fSoloCriticos = !!c.soloCriticos;
      this.fLimite = c.limite ?? 10;
    }
    this.refinando.set(true);
  }

  metricaSeleccionada(): MetricaOpcion | null {
    return this.opciones()?.metricas.find(m => m.codigo === this.fMetrica) ?? null;
  }

  cambiarMetrica(): void {
    const m = this.metricaSeleccionada();
    if (m && !m.agrupaciones.some(a => a.codigo === this.fAgrupacion)) {
      this.fAgrupacion = m.agrupaciones[0].codigo as AgrupacionVoz;
    }
  }

  usaLimite(): boolean {
    return this.fAgrupacion !== 'NINGUNA' && this.fAgrupacion !== 'DIA' && this.fAgrupacion !== 'MES';
  }

  /** HU39.3: vuelve a ejecutar el reporte con los filtros ajustados a mano. */
  aplicarRefinamiento(): void {
    if (this.fFechaInicio && this.fFechaFin && this.fFechaInicio > this.fFechaFin) {
      this.error.set('La fecha de inicio no puede ser posterior a la fecha de fin.');
      return;
    }
    this.cargar(this.api.ejecutar(this.consultaFormulario()), true);
  }

  exportar(): void {
    const r = this.reporte();
    if (!r) return;
    this.exportando.set(true);
    this.api.exportar(r.consulta)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const enlace = document.createElement('a');
          enlace.href = url;
          enlace.download = `reporte-voz-${r.consulta.metrica.toLowerCase()}.csv`;
          enlace.click();
          URL.revokeObjectURL(url);
          this.exportando.set(false);
        },
        error: () => {
          this.exportando.set(false);
          this.error.set('No se pudo exportar el reporte.');
        }
      });
  }

  origenEtiqueta(origen: ReporteVoz['origen']): string {
    return origen === 'N8N' ? 'Interpretado por N8N' : origen === 'LOCAL' ? 'Intérprete local' : 'Refinado manualmente';
  }

  celda(fila: Record<string, string | number | null>, col: ColumnaReporte): string {
    const valor = fila[col.clave];
    return col.clave === 'grupo' ? this.etiquetaGrupo(valor) : this.formatear(valor, col.tipo);
  }

  total(col: ColumnaReporte): string {
    const valor = this.reporte()?.totales?.[col.clave];
    return valor === undefined || valor === null ? '' : this.formatear(valor, col.tipo);
  }

  tieneTotales(): boolean {
    return Object.keys(this.reporte()?.totales ?? {}).length > 0;
  }

  esNumerica(col: ColumnaReporte): boolean {
    return col.tipo === 'NUMERO' || col.tipo === 'MONEDA';
  }

  formatear(valor: string | number | null | undefined, tipo: TipoDato): string {
    if (valor === null || valor === undefined || valor === '') return '—';
    if (tipo === 'MONEDA') return 'Bs. ' + formatNumber(Number(valor), this.locale, '1.2-2');
    if (tipo === 'NUMERO') return formatNumber(Number(valor), this.locale, '1.0-2');
    if (tipo === 'FECHA') return this.fecha(String(valor));
    return String(valor);
  }

  private etiquetaGrupo(valor: string | number | null | undefined): string {
    if (valor === null || valor === undefined) return '—';
    const texto = String(valor);
    if (/^\d{4}-\d{2}$/.test(texto)) {
      const [anio, mes] = texto.split('-');
      return `${MESES[Number(mes) - 1]} ${anio}`;
    }
    if (/^\d{4}-\d{2}-\d{2}/.test(texto)) return this.fecha(texto);
    return texto;
  }

  private fecha(iso: string): string {
    const [anio, mes, dia] = iso.substring(0, 10).split('-');
    return `${dia}/${mes}/${anio}`;
  }

  private consultaFormulario(): ConsultaReporteVoz {
    const m = this.metricaSeleccionada();
    return {
      metrica: this.fMetrica,
      agrupacion: this.fAgrupacion,
      fechaInicio: m?.usaFechas && this.fFechaInicio ? this.fFechaInicio : null,
      fechaFin: m?.usaFechas && this.fFechaFin ? this.fFechaFin : null,
      sucursalId: this.fSucursalId === '' ? null : Number(this.fSucursalId),
      productoId: m?.admiteProducto && this.fProductoId !== '' ? Number(this.fProductoId) : null,
      soloCriticos: m?.admiteCriticos && this.fSoloCriticos ? true : null,
      limite: this.usaLimite() ? Math.min(Math.max(Number(this.fLimite) || 10, 1), 100) : null
    };
  }

  private cargar(peticion: Observable<ReporteVoz>, cerrarRefinar = false): void {
    this.loading.set(true);
    this.error.set('');
    peticion.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (r) => {
        this.reporte.set(r);
        this.medida.set(this.medidas()[0]?.clave ?? '');
        this.vista.set(r.visualizacion === 'TABLA' ? 'tabla' : 'grafico');
        if (cerrarRefinar) this.refinando.set(false);
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.manejarError(e);
      }
    });
  }

  private manejarError(err: unknown): void {
    if (err instanceof HttpErrorResponse) {
      const msg = err.error?.message || err.error?.error;
      this.error.set(msg || 'Ocurrió un error inesperado al generar el reporte.');
    } else {
      this.error.set('No se pudo establecer conexión con el servidor.');
    }
  }
}
