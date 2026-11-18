import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Observable, Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { AsientosService } from '../services/asientos.service';
import {
  Asiento,
  AsientoRequest,
  AsientoResumen,
  CuentaOpcion,
  FiltroAsientos,
  LineaAsientoRequest,
  PermisosAsientos,
  TIPOS_AUTOMATICOS,
} from '../models/asiento.model';
import { erroresAsiento, hoyIso, mensajeError, totales } from '../utils/asiento.utils';

type Vista = 'lista' | 'editor' | 'detalle';

/** Línea del editor: además del request guarda el texto visible del selector de cuenta. */
interface LineaEditor extends LineaAsientoRequest {
  cuentaTexto: string;
}

const etiquetaCuenta = (c: { codigo: string; nombre: string }) => `${c.codigo} — ${c.nombre}`;

/**
 * HU-27 / CU27 — Gestionar Asientos Contables.
 *
 * <p>Libro Diario con todos los asientos (manuales y los que generan ventas y cobros),
 * editor de partida doble que muestra el cuadre en vivo, y detalle con las acciones que
 * permite el estado: un BORRADOR se corrige o se contabiliza; uno manual se anula (si ya
 * estaba MAYORIZADO, el backend registra la reversión). Los botones dependen de
 * `opciones/permisos`, pero quien decide es el backend.
 */
@Component({
  selector: 'app-asientos-contables',
  standalone: true,
  imports: [FormsModule, DecimalPipe],
  templateUrl: './asientos-contables.component.html',
  styleUrl: './asientos-contables.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AsientosContablesComponent {
  private readonly api = inject(AsientosService);
  private readonly destroy = inject(DestroyRef);
  private readonly busquedaCuenta = new Subject<string>();
  private generation = 0;

  readonly vista = signal<Vista>('lista');
  readonly asientos = signal<AsientoResumen[]>([]);
  readonly detalle = signal<Asiento | null>(null);
  readonly tiposManuales = signal<string[]>(['DIARIO', 'INGRESO', 'EGRESO', 'TRASPASO', 'AJUSTE']);
  readonly tiposAutomaticos = TIPOS_AUTOMATICOS;
  readonly cuentas = signal<CuentaOpcion[]>([]);
  readonly permisos = signal<PermisosAsientos>({ editar: false, anular: false });
  readonly loading = signal(false);
  readonly busy = signal(false);
  readonly forbidden = signal(false);
  readonly error = signal('');
  readonly success = signal('');
  readonly total = signal(0);
  readonly pages = signal(0);

  filtro: FiltroAsientos = { search: '', tipo: '', estado: '', origen: '', desde: '', hasta: '' };
  page = 0;

  // editor
  readonly editandoId = signal<number | null>(null);
  readonly lineas = signal<LineaEditor[]>([]);
  readonly erroresForm = signal<string[]>([]);
  fecha = hoyIso();
  tipo = 'DIARIO';
  glosa = '';
  readonly cuadre = computed(() => totales(this.lineas()));
  private readonly etiquetas = new Map<string, number>();

  // anulación
  readonly anulando = signal(false);
  motivo = '';

  constructor() {
    this.load();
    this.api.tipos().pipe(takeUntilDestroyed(this.destroy)).subscribe({ next: (t) => this.tiposManuales.set(t), error: () => undefined });
    this.api.permisos().pipe(takeUntilDestroyed(this.destroy)).subscribe({ next: (p) => this.permisos.set(p), error: () => undefined });
    this.busquedaCuenta
      .pipe(debounceTime(250), distinctUntilChanged(), switchMap((t) => this.api.cuentas(t)), takeUntilDestroyed(this.destroy))
      .subscribe({ next: (c) => this.recordarCuentas(c), error: () => undefined });
    this.busquedaCuenta.next('');
  }

  // ------------------------------------------------------------- listado --

  load(reset = false): void {
    if (reset) this.page = 0;
    const generation = ++this.generation;
    this.loading.set(true);
    this.api
      .list(this.filtro, this.page)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (r) => {
          if (generation !== this.generation) return;
          this.asientos.set(r.content);
          this.total.set(r.totalElements);
          this.pages.set(r.totalPages);
          this.loading.set(false);
          this.forbidden.set(false);
        },
        error: (err: HttpErrorResponse) => {
          if (generation !== this.generation) return;
          this.loading.set(false);
          this.fail(err);
        },
      });
  }

  limpiarFiltros(): void {
    this.filtro = { search: '', tipo: '', estado: '', origen: '', desde: '', hasta: '' };
    this.load(true);
  }

  navigate(delta: number): void {
    this.page += delta;
    this.load();
  }

  volver(): void {
    this.vista.set('lista');
    this.detalle.set(null);
    this.anulando.set(false);
    this.load();
  }

  // -------------------------------------------------------------- editor --

  nuevo(): void {
    this.limpiarMensajes();
    this.editandoId.set(null);
    this.fecha = hoyIso();
    this.tipo = 'DIARIO';
    this.glosa = '';
    this.lineas.set([this.lineaVacia(), this.lineaVacia()]);
    this.erroresForm.set([]);
    this.vista.set('editor');
  }

  editar(a: Asiento): void {
    this.limpiarMensajes();
    this.editandoId.set(a.id);
    this.fecha = a.fecha;
    this.tipo = a.tipo;
    this.glosa = a.glosa;
    this.lineas.set(
      a.lineas.map((l) => {
        const texto = etiquetaCuenta({ codigo: l.cuentaCodigo, nombre: l.cuentaNombre });
        this.etiquetas.set(texto, l.cuentaId);
        return { cuentaId: l.cuentaId, cuentaTexto: texto, glosa: l.glosa, debe: l.debe || null, haber: l.haber || null };
      }),
    );
    this.erroresForm.set([]);
    this.vista.set('editor');
  }

  agregarLinea(): void {
    this.lineas.update((ls) => [...ls, this.lineaVacia()]);
  }

  quitarLinea(i: number): void {
    this.lineas.update((ls) => ls.filter((_, j) => j !== i));
  }

  /** Copia la glosa general a las líneas que no tienen una propia. */
  copiarGlosa(): void {
    this.lineas.update((ls) => ls.map((l) => ({ ...l, glosa: l.glosa?.trim() ? l.glosa : this.glosa.trim().slice(0, 255) })));
  }

  buscarCuenta(i: number, texto: string): void {
    const id = this.etiquetas.get(texto) ?? null;
    this.actualizarLinea(i, { cuentaTexto: texto, cuentaId: id });
    if (!id) this.busquedaCuenta.next(texto.split('—')[0].trim());
  }

  /** Al escribir un importe en un lado se limpia el otro, para que nunca queden los dos. */
  cambiarImporte(i: number, lado: 'debe' | 'haber', valor: number | null): void {
    const otro = lado === 'debe' ? 'haber' : 'debe';
    const cambio: Partial<LineaEditor> = { [lado]: valor };
    if (valor && valor > 0) cambio[otro] = null;
    this.actualizarLinea(i, cambio);
  }

  guardar(contabilizar: boolean): void {
    if (this.busy()) return;
    const errores = erroresAsiento(this.glosa, this.lineas());
    if (!this.fecha) errores.unshift('La fecha es obligatoria.');
    this.erroresForm.set(errores);
    if (errores.length) {
      this.error.set('Revisa el asiento antes de guardarlo.');
      return;
    }
    const body: AsientoRequest = {
      fecha: this.fecha,
      tipo: this.tipo,
      glosa: this.glosa.trim(),
      lineas: this.lineas().map((l) => ({
        cuentaId: l.cuentaId,
        glosa: l.glosa?.trim() || null,
        debe: Number(l.debe) || 0,
        haber: Number(l.haber) || 0,
      })),
    };
    const id = this.editandoId();
    const guardado = id ? this.api.update(id, body) : this.api.create(body);
    this.ejecutar(guardado, (a) => {
      if (!contabilizar) {
        this.mostrar(a, id ? `Asiento ${a.numero} actualizado.` : `Asiento ${a.numero} registrado en borrador.`);
        return;
      }
      // Si contabilizar falla, el borrador ya existe: se muestra para no duplicarlo al reintentar.
      this.busy.set(true);
      this.api.contabilizar(a.id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
        next: (c) => {
          this.busy.set(false);
          this.mostrar(c, `Asiento ${c.numero} registrado y contabilizado.`);
        },
        error: (err) => {
          this.busy.set(false);
          this.mostrar(a, '');
          this.fail(err);
        },
      });
    });
  }

  // ------------------------------------------------------------- detalle --

  abrir(id: number, mensaje = ''): void {
    if (!mensaje) this.limpiarMensajes();
    this.busy.set(true);
    this.api
      .detail(id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: (a) => {
          this.busy.set(false);
          this.mostrar(a, mensaje);
        },
        error: (err) => {
          this.busy.set(false);
          this.fail(err);
        },
      });
  }

  contabilizar(a: Asiento): void {
    this.ejecutar(this.api.contabilizar(a.id), (c) => this.mostrar(c, `Asiento ${c.numero} contabilizado. Ya no se puede modificar.`));
  }

  pedirAnulacion(): void {
    this.motivo = '';
    this.anulando.set(true);
  }

  anular(a: Asiento): void {
    if (this.motivo.trim().length < 5) return;
    const mayorizado = a.estado === 'MAYORIZADO';
    this.ejecutar(this.api.anular(a.id, this.motivo.trim()), (r) => {
      this.anulando.set(false);
      this.mostrar(r, mayorizado
        ? `Asiento ${r.numero} anulado: se registró su reversión con la fecha de hoy.`
        : `Borrador ${r.numero} anulado.`);
    });
  }

  // --------------------------------------------------------------- apoyo --

  etiqueta(c: CuentaOpcion): string {
    return etiquetaCuenta(c);
  }

  private mostrar(a: Asiento, mensaje: string): void {
    this.detalle.set(a);
    this.vista.set('detalle');
    this.erroresForm.set([]);
    if (mensaje) this.success.set(mensaje);
  }

  private recordarCuentas(cuentas: CuentaOpcion[]): void {
    for (const c of cuentas) this.etiquetas.set(etiquetaCuenta(c), c.id);
    this.cuentas.set(cuentas);
    // Una línea cuyo texto ya coincide con una cuenta recién llegada queda elegida.
    this.lineas.update((ls) => ls.map((l) => (l.cuentaId ? l : { ...l, cuentaId: this.etiquetas.get(l.cuentaTexto) ?? null })));
  }

  private actualizarLinea(i: number, cambio: Partial<LineaEditor>): void {
    this.lineas.update((ls) => ls.map((l, j) => (j === i ? { ...l, ...cambio } : l)));
  }

  private lineaVacia(): LineaEditor {
    return { cuentaId: null, cuentaTexto: '', glosa: null, debe: null, haber: null };
  }

  private ejecutar<T>(peticion: Observable<T>, alTerminar: (r: T) => void): void {
    this.busy.set(true);
    this.limpiarMensajes();
    peticion.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (r) => {
        this.busy.set(false);
        alTerminar(r);
      },
      error: (err) => {
        this.busy.set(false);
        this.fail(err);
      },
    });
  }

  private limpiarMensajes(): void {
    this.error.set('');
    this.success.set('');
  }

  private fail(err: HttpErrorResponse): void {
    this.forbidden.set(err.status === 403 && this.vista() === 'lista');
    this.error.set(mensajeError(err));
  }
}
