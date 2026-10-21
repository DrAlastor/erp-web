import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ReportesService } from '../services/reportes.service';
import {
  ColumnaReporte,
  DefinicionReporte,
  ErrorRespuesta,
  FiltroReporte,
  Opcion,
  ParametrosReporte,
  ResultadoReporte,
  ValorCelda,
} from '../models/reportes.model';
import { esNumerica, fechaIso, formatearFecha, formatearValor } from '../utils/formato';

interface GrupoReportes {
  categoria: string;
  etiqueta: string;
  reportes: DefinicionReporte[];
}

@Component({
  selector: 'app-reportes-predefinidos',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './reportes-predefinidos.component.html',
  styleUrl: './reportes-predefinidos.component.scss',
})
export class ReportesPredefinidosComponent implements OnInit {
  private readonly servicio = inject(ReportesService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly catalogo = signal<DefinicionReporte[]>([]);
  readonly sucursales = signal<Opcion[]>([]);
  readonly categorias = signal<Opcion[]>([]);
  readonly seleccionado = signal<DefinicionReporte | null>(null);
  readonly resultado = signal<ResultadoReporte | null>(null);

  readonly cargandoCatalogo = signal(true);
  readonly generando = signal(false);
  readonly descargando = signal(false);
  readonly error = signal<string | null>(null);

  readonly grupos = computed<GrupoReportes[]>(() => {
    const grupos = new Map<string, GrupoReportes>();
    for (const r of this.catalogo()) {
      const grupo = grupos.get(r.categoria) ?? { categoria: r.categoria, etiqueta: r.categoriaEtiqueta, reportes: [] };
      grupo.reportes.push(r);
      grupos.set(r.categoria, grupo);
    }
    return [...grupos.values()];
  });

  readonly formulario = this.fb.group({
    desde: [this.primerDiaDelMes()],
    hasta: [fechaIso(new Date())],
    sucursalId: [null as number | null],
    categoriaId: [null as number | null],
    limite: [10, [Validators.min(1), Validators.max(100)]],
  });

  ngOnInit(): void {
    this.servicio.catalogo().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (catalogo) => {
        this.catalogo.set(catalogo);
        this.cargandoCatalogo.set(false);
        if (catalogo.length) this.elegir(catalogo[0]);
      },
      error: (err) => {
        this.cargandoCatalogo.set(false);
        this.error.set(this.mensajeDe(err, 'No se pudo cargar el catálogo de reportes.'));
      },
    });
    this.servicio.opciones().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (o) => {
        this.sucursales.set(o.sucursales);
        this.categorias.set(o.categorias);
      },
      error: () => {
        // Sin opciones los combos quedan en "Todas"; el reporte se puede generar igual.
      },
    });
  }

  elegir(reporte: DefinicionReporte): void {
    this.seleccionado.set(reporte);
    this.resultado.set(null);
    this.error.set(null);
  }

  usa(filtro: FiltroReporte): boolean {
    return this.seleccionado()?.filtros.includes(filtro) ?? false;
  }

  generar(): void {
    const reporte = this.seleccionado();
    if (!reporte || this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    const problema = this.validarFechas();
    if (problema) {
      this.error.set(problema);
      return;
    }
    this.error.set(null);
    this.generando.set(true);
    this.servicio.generar(reporte.codigo, this.parametros()).subscribe({
      next: (r) => {
        this.resultado.set(r);
        this.generando.set(false);
      },
      error: (err) => {
        this.generando.set(false);
        this.error.set(this.mensajeDe(err, 'No se pudo generar el reporte.'));
      },
    });
  }

  descargarCsv(): void {
    const reporte = this.seleccionado();
    if (!reporte) return;
    this.descargando.set(true);
    this.servicio.descargarCsv(reporte.codigo, this.parametros()).subscribe({
      next: () => this.descargando.set(false),
      error: (err) => {
        this.descargando.set(false);
        this.error.set(this.mensajeDe(err, 'No se pudo descargar el CSV.'));
      },
    });
  }

  imprimir(): void {
    const r = this.resultado();
    if (r) this.servicio.imprimir(r);
  }

  valor(fila: Record<string, ValorCelda>, columna: ColumnaReporte): string {
    return formatearValor(fila[columna.clave], columna);
  }

  total(columna: ColumnaReporte, indice: number): string {
    const totales = this.resultado()?.totales ?? {};
    if (columna.clave in totales) return formatearValor(totales[columna.clave], columna);
    return indice === 0 ? 'Totales' : '';
  }

  hayTotales(): boolean {
    return Object.keys(this.resultado()?.totales ?? {}).length > 0;
  }

  numerica(columna: ColumnaReporte): boolean {
    return esNumerica(columna);
  }

  generadoEn(): string {
    const r = this.resultado();
    return r ? formatearFecha(r.generadoEn) : '';
  }

  filtrosAplicados(): [string, string][] {
    return Object.entries(this.resultado()?.filtros ?? {});
  }

  /** Validación previa al envío; el backend vuelve a comprobarla. */
  validarFechas(): string | null {
    if (!this.usa('DESDE')) return null;
    const { desde, hasta } = this.formulario.getRawValue();
    if (desde && hasta && desde > hasta) return 'La fecha inicial no puede ser posterior a la fecha final.';
    return null;
  }

  parametros(): ParametrosReporte {
    const v = this.formulario.getRawValue();
    return {
      desde: this.usa('DESDE') ? v.desde : null,
      hasta: this.usa('HASTA') ? v.hasta : null,
      sucursalId: this.usa('SUCURSAL') ? v.sucursalId : null,
      categoriaId: this.usa('CATEGORIA') ? v.categoriaId : null,
      limite: this.usa('LIMITE') ? v.limite : null,
    };
  }

  private primerDiaDelMes(): string {
    const hoy = new Date();
    return fechaIso(new Date(hoy.getFullYear(), hoy.getMonth(), 1));
  }

  private mensajeDe(err: unknown, porDefecto: string): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 403) return 'No tienes permiso para ver este reporte.';
      const cuerpo = err.error as Partial<ErrorRespuesta> | null;
      if (cuerpo && typeof cuerpo === 'object' && cuerpo.message) return cuerpo.message;
    }
    return porDefecto;
  }
}
