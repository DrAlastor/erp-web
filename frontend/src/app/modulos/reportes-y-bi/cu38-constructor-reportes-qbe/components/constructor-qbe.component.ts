import { ChangeDetectionStrategy, Component, DestroyRef, LOCALE_ID, OnInit, inject, signal } from '@angular/core';
import { CommonModule, formatNumber } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ReporteQbeService } from '../services/reporte-qbe.service';
import {
  AgregacionOpcion,
  CampoQbe,
  CatalogoQbe,
  ColumnaQbe,
  DefinicionQbe,
  EntidadQbe,
  FiltroQbe,
  FormatoExportacion,
  OperadorLogico,
  OperadorQbe,
  OrdenQbe,
  PlantillaQbe,
  ResultadoQbe,
  TipoCampo
} from '../models/reporte-qbe.model';

const MESES = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];

@Component({
  selector: 'app-constructor-qbe',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './constructor-qbe.component.html',
  styleUrl: './constructor-qbe.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConstructorQbeComponent implements OnInit {
  private readonly api = inject(ReporteQbeService);
  private readonly destroy = inject(DestroyRef);
  private readonly locale = inject(LOCALE_ID);

  readonly catalogo = signal<CatalogoQbe | null>(null);
  readonly plantillas = signal<PlantillaQbe[]>([]);
  readonly resultado = signal<ResultadoQbe | null>(null);
  readonly loading = signal(false);
  readonly guardando = signal(false);
  readonly exportando = signal<FormatoExportacion | null>(null);
  readonly error = signal('');
  readonly mensaje = signal('');
  readonly advertencias = signal<string[]>([]);

  // Definición en edición
  entidadCodigo = '';
  columnas: ColumnaQbe[] = [];
  filtros: FiltroQbe[] = [];
  operadorLogico: OperadorLogico = 'AND';
  orden: OrdenQbe[] = [];

  // Plantilla
  plantillaId: number | null = null;
  plantillaSeleccionada: number | '' = '';
  nombre = '';
  descripcion = '';

  tamanio = 25;

  ngOnInit(): void {
    this.api.catalogo()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (c) => this.catalogo.set(c),
        error: (e) => this.manejarError(e)
      });
    this.cargarPlantillas();
  }

  // ---------- HU38.1: entidad y campos ----------

  entidad(): EntidadQbe | null {
    return this.catalogo()?.entidades.find(e => e.codigo === this.entidadCodigo) ?? null;
  }

  modulos(): { modulo: string; entidades: EntidadQbe[] }[] {
    const grupos = new Map<string, EntidadQbe[]>();
    for (const e of this.catalogo()?.entidades ?? []) {
      grupos.set(e.modulo, [...(grupos.get(e.modulo) ?? []), e]);
    }
    return [...grupos.entries()].map(([modulo, entidades]) => ({ modulo, entidades }));
  }

  campo(clave: string): CampoQbe | null {
    return this.entidad()?.campos.find(c => c.clave === clave) ?? null;
  }

  cambiarEntidad(): void {
    this.columnas = [];
    this.filtros = [];
    this.orden = [];
    this.resultado.set(null);
    this.error.set('');
  }

  seleccionado(clave: string): boolean {
    return this.columnas.some(c => c.campo === clave);
  }

  agregarColumna(clave: string): void {
    this.columnas = [...this.columnas, { campo: clave, agregacion: null, granularidad: null }];
  }

  quitarColumna(i: number): void {
    this.columnas = this.columnas.filter((_, j) => j !== i);
    this.orden = this.orden
      .filter(o => o.columna !== i)
      .map(o => ({ ...o, columna: o.columna > i ? o.columna - 1 : o.columna }));
  }

  /** Reordena las columnas; el orden del resultado sigue apuntando a la misma columna. */
  mover(i: number, delta: number): void {
    const j = i + delta;
    if (j < 0 || j >= this.columnas.length) return;
    const copia = [...this.columnas];
    [copia[i], copia[j]] = [copia[j], copia[i]];
    this.columnas = copia;
    this.orden = this.orden.map(o => ({
      ...o, columna: o.columna === i ? j : o.columna === j ? i : o.columna
    }));
  }

  agregacionesPara(clave: string): AgregacionOpcion[] {
    const tipo = this.campo(clave)?.tipo;
    return (this.catalogo()?.agregaciones ?? []).filter(a => tipo && a.tipos.includes(tipo));
  }

  hayAgregaciones(): boolean {
    return this.columnas.some(c => c.agregacion);
  }

  etiquetaColumna(i: number): string {
    const c = this.columnas[i];
    if (!c) return '';
    const base = this.campo(c.campo)?.etiqueta ?? c.campo;
    const agregacion = this.catalogo()?.agregaciones.find(a => a.codigo === c.agregacion);
    return agregacion ? `${agregacion.etiqueta} de ${base}` : base;
  }

  // ---------- HU38.2: filtros, agrupación y orden ----------

  operadoresPara(clave: string): OperadorQbe[] {
    const tipo = this.campo(clave)?.tipo;
    return (this.catalogo()?.operadores ?? []).filter(o => tipo && o.tipos.includes(tipo));
  }

  operador(codigo: string): OperadorQbe | null {
    return this.catalogo()?.operadores.find(o => o.codigo === codigo) ?? null;
  }

  agregarFiltro(): void {
    const primero = this.entidad()?.campos[0];
    if (!primero) return;
    this.filtros = [...this.filtros, { campo: primero.clave, operador: this.operadoresPara(primero.clave)[0].codigo, valor: null, valor2: null }];
  }

  cambiarCampoFiltro(f: FiltroQbe): void {
    f.operador = this.operadoresPara(f.campo)[0]?.codigo ?? '';
    f.valor = null;
    f.valor2 = null;
  }

  quitarFiltro(i: number): void {
    this.filtros = this.filtros.filter((_, j) => j !== i);
  }

  tipoEntrada(f: FiltroQbe): string {
    if (f.operador === 'ULTIMOS_DIAS') return 'number';
    const tipo = this.campo(f.campo)?.tipo;
    return tipo === 'FECHA' ? 'date' : tipo === 'NUMERO' || tipo === 'MONEDA' ? 'number' : 'text';
  }

  agregarOrden(): void {
    if (this.columnas.length === 0) return;
    this.orden = [...this.orden, { columna: 0, descendente: false }];
  }

  quitarOrden(i: number): void {
    this.orden = this.orden.filter((_, j) => j !== i);
  }

  // ---------- HU38.3: ejecución, exportación y plantillas ----------

  ejecutar(pagina = 0): void {
    const definicion = this.definicion();
    if (!definicion) return;
    this.loading.set(true);
    this.error.set('');
    this.mensaje.set('');
    this.api.vistaPrevia(definicion, pagina, this.tamanio)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => {
          this.resultado.set(r);
          this.loading.set(false);
        },
        error: (e) => {
          this.loading.set(false);
          this.manejarError(e);
        }
      });
  }

  exportar(formato: FormatoExportacion): void {
    const definicion = this.definicion();
    if (!definicion) return;
    const titulo = this.nombre.trim() || `Reporte de ${this.entidad()?.etiqueta ?? ''}`;
    this.exportando.set(formato);
    this.api.exportar(definicion, formato, titulo)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (blob) => {
          const url = URL.createObjectURL(blob);
          const enlace = document.createElement('a');
          enlace.href = url;
          enlace.download = `${this.slug(titulo)}.${formato.toLowerCase()}`;
          enlace.click();
          URL.revokeObjectURL(url);
          this.exportando.set(null);
        },
        error: () => {
          this.exportando.set(null);
          this.error.set(`No se pudo exportar el reporte a ${formato}.`);
        }
      });
  }

  guardar(comoNueva: boolean): void {
    const definicion = this.definicion();
    if (!definicion) return;
    if (!this.nombre.trim()) {
      this.error.set('Ponle un nombre a la plantilla para guardarla.');
      return;
    }
    const request = { nombre: this.nombre.trim(), descripcion: this.descripcion.trim() || null, definicion };
    const peticion = comoNueva || this.plantillaId === null
      ? this.api.crearPlantilla(request)
      : this.api.actualizarPlantilla(this.plantillaId, request);
    this.guardando.set(true);
    this.error.set('');
    peticion.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (p) => {
        this.plantillaId = p.id;
        this.plantillaSeleccionada = p.id;
        this.guardando.set(false);
        this.mensaje.set(`Plantilla "${p.nombre}" guardada.`);
        this.cargarPlantillas();
      },
      error: (e) => {
        this.guardando.set(false);
        this.manejarError(e);
      }
    });
  }

  cargarPlantilla(): void {
    const p = this.plantillas().find(x => x.id === Number(this.plantillaSeleccionada));
    if (!p) {
      this.nuevo();
      return;
    }
    const d = p.definicion;
    this.entidadCodigo = d.entidad;
    this.columnas = d.columnas.map(c => ({ ...c }));
    this.filtros = (d.filtros ?? []).map(f => ({ ...f }));
    this.operadorLogico = d.operadorLogico ?? 'AND';
    this.orden = (d.orden ?? []).map(o => ({ ...o }));
    this.plantillaId = p.id;
    this.nombre = p.nombre;
    this.descripcion = p.descripcion ?? '';
    this.advertencias.set(p.advertencias ?? []);
    this.mensaje.set('');
    if (this.columnas.length > 0) this.ejecutar();
  }

  eliminarPlantilla(): void {
    const id = this.plantillaId;
    if (id === null || !confirm(`¿Eliminar la plantilla "${this.nombre}"? Esta acción no se puede deshacer.`)) return;
    this.api.eliminarPlantilla(id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.nuevo();
          this.mensaje.set('Plantilla eliminada.');
          this.cargarPlantillas();
        },
        error: (e) => this.manejarError(e)
      });
  }

  nuevo(): void {
    this.entidadCodigo = '';
    this.cambiarEntidad();
    this.operadorLogico = 'AND';
    this.plantillaId = null;
    this.plantillaSeleccionada = '';
    this.nombre = '';
    this.descripcion = '';
    this.advertencias.set([]);
    this.mensaje.set('');
  }

  esNumerica(tipo: TipoCampo): boolean {
    return tipo === 'NUMERO' || tipo === 'MONEDA';
  }

  formatear(valor: string | number | boolean | null | undefined, tipo: TipoCampo): string {
    if (valor === null || valor === undefined || valor === '') return '—';
    if (typeof valor === 'boolean') return valor ? 'Sí' : 'No';
    if (tipo === 'MONEDA') return 'Bs. ' + formatNumber(Number(valor), this.locale, '1.2-2');
    if (tipo === 'NUMERO') return formatNumber(Number(valor), this.locale, '1.0-2');
    const texto = String(valor);
    if (tipo === 'FECHA' && /^\d{4}-\d{2}-\d{2}/.test(texto)) {
      const [a, m, d] = texto.substring(0, 10).split('-');
      const hora = texto.length > 10 ? ' ' + texto.substring(11, 16) : '';
      return `${d}/${m}/${a}${hora}`;
    }
    if (/^\d{4}-\d{2}$/.test(texto)) {
      const [a, m] = texto.split('-');
      return `${MESES[Number(m) - 1]} ${a}`;
    }
    return texto;
  }

  private definicion(): DefinicionQbe | null {
    if (!this.entidadCodigo) {
      this.error.set('Selecciona la entidad del reporte.');
      return null;
    }
    if (this.columnas.length === 0) {
      this.error.set('Selecciona al menos un campo para el reporte.');
      return null;
    }
    return {
      entidad: this.entidadCodigo,
      columnas: this.columnas,
      filtros: this.filtros.map(f => ({
        ...f,
        valor: f.valor === null || f.valor === undefined ? null : String(f.valor),
        valor2: f.valor2 === null || f.valor2 === undefined ? null : String(f.valor2)
      })),
      operadorLogico: this.operadorLogico,
      orden: this.orden.map(o => ({ columna: Number(o.columna), descendente: !!o.descendente }))
    };
  }

  private cargarPlantillas(): void {
    this.api.listarPlantillas()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (p) => this.plantillas.set(p),
        error: () => this.plantillas.set([])
      });
  }

  private slug(texto: string): string {
    return texto.normalize('NFD').replace(/\p{M}/gu, '').toLowerCase()
      .replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '') || 'reporte';
  }

  private manejarError(err: unknown): void {
    if (err instanceof HttpErrorResponse) {
      const msg = err.error?.message || err.error?.error;
      this.error.set(msg || 'Ocurrió un error inesperado al procesar el reporte.');
    } else {
      this.error.set('No se pudo establecer conexión con el servidor.');
    }
  }
}
