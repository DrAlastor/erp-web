import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Catalogos, Compra, Devolucion } from '../models/compra.model';
import { CompraService } from '../services/compra.service';

type Pestana = 'compras' | 'devoluciones';

@Component({
  selector: 'app-registrar-compra',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './registrar-compra.component.html',
  styleUrl: './registrar-compra.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegistrarCompraComponent implements OnInit {
  private readonly servicio = inject(CompraService);
  private readonly fb = inject(FormBuilder);

  readonly pestana = signal<Pestana>('compras');
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly catalogos = signal<Catalogos>({ proveedores: [], almacenes: [], productos: [] });
  readonly compras = signal<Compra[]>([]);
  readonly devoluciones = signal<Devolucion[]>([]);

  // Filtros
  readonly filtroEstado = signal('');
  readonly filtroProveedor = signal('');
  readonly filtroTexto = signal('');

  // Detalle / modales
  readonly detalle = signal<Compra | null>(null);
  readonly modalCompra = signal(false);
  readonly anulando = signal<Compra | null>(null);
  readonly devolviendo = signal<Compra | null>(null);

  readonly hoy = new Date().toISOString().substring(0, 10);

  readonly formCompra: FormGroup = this.fb.group({
    proveedorId: [null, Validators.required],
    sucursalId: [null, Validators.required],
    numeroFactura: ['', [Validators.required, Validators.maxLength(50)]],
    numeroAutorizacion: ['', Validators.maxLength(60)],
    fecha: [this.hoy, Validators.required],
    tipoPago: ['CONTADO', Validators.required],
    observaciones: [''],
    lineas: this.fb.array([]),
  });

  readonly formAnular: FormGroup = this.fb.group({
    motivo: ['', [Validators.required, Validators.maxLength(200)]],
  });

  readonly formDevolucion: FormGroup = this.fb.group({
    motivo: ['', [Validators.required, Validators.maxLength(255)]],
    fecha: [this.hoy, Validators.required],
    lineas: this.fb.array([]),
  });

  readonly totalesCompra = signal({ subtotal: 0 });
  readonly totalDevolucion = signal(0);

  readonly hayCompras = computed(() => this.compras().length > 0);

  get lineas(): FormArray { return this.formCompra.get('lineas') as FormArray; }
  get lineasDev(): FormArray { return this.formDevolucion.get('lineas') as FormArray; }

  ngOnInit(): void {
    this.servicio.catalogos().subscribe({
      next: (c) => this.catalogos.set(c),
      error: (err) => this.error.set(this.mensaje(err, 'No se pudieron cargar proveedores, almacenes y productos.')),
    });
    this.cargarCompras();
    this.cargarDevoluciones();
  }

  seleccionarPestana(p: Pestana): void {
    this.pestana.set(p);
    this.limpiarAlertas();
  }

  limpiarAlertas(): void {
    this.error.set(null);
    this.success.set(null);
  }

  // ----------------------------------------------------------------- Listados

  cargarCompras(): void {
    this.servicio.listar({
      proveedorId: this.filtroProveedor() ? Number(this.filtroProveedor()) : undefined,
      estado: this.filtroEstado() || undefined,
      search: this.filtroTexto().trim() || undefined,
    }).subscribe({
      next: (l) => this.compras.set(l),
      error: (err) => this.error.set(this.mensaje(err, 'No se pudo cargar el listado de compras.')),
    });
  }

  cargarDevoluciones(): void {
    this.servicio.listarDevoluciones().subscribe({
      next: (l) => this.devoluciones.set(l),
      error: () => this.devoluciones.set([]),
    });
  }

  verDetalle(c: Compra): void {
    this.servicio.obtener(c.id).subscribe({
      next: (d) => this.detalle.set(d),
      error: (err) => this.error.set(this.mensaje(err, 'No se pudo cargar la compra.')),
    });
  }

  // --------------------------------------------------------------- Nueva compra

  abrirNuevaCompra(): void {
    this.limpiarAlertas();
    this.lineas.clear();
    this.formCompra.reset({
      proveedorId: null,
      sucursalId: this.catalogos().almacenes[0]?.id ?? null,
      numeroFactura: '',
      numeroAutorizacion: '',
      fecha: this.hoy,
      tipoPago: 'CONTADO',
      observaciones: '',
    });
    this.agregarLinea();
    this.modalCompra.set(true);
  }

  agregarLinea(): void {
    const g = this.fb.group({
      productoId: [null, Validators.required],
      cantidad: [1, [Validators.required, Validators.min(0.01)]],
      costoUnitario: [0, [Validators.required, Validators.min(0.01)]],
    });
    g.get('productoId')!.valueChanges.subscribe((id) => {
      const p = this.catalogos().productos.find((x) => x.id === Number(id));
      if (p?.costoPromedio) g.get('costoUnitario')!.setValue(p.costoPromedio);
    });
    g.valueChanges.subscribe(() => this.recalcular());
    this.lineas.push(g);
    this.recalcular();
  }

  quitarLinea(i: number): void {
    if (this.lineas.length > 1) {
      this.lineas.removeAt(i);
      this.recalcular();
    }
  }

  subtotalLinea(i: number): number {
    const v = this.lineas.at(i).value;
    return (Number(v.cantidad) || 0) * (Number(v.costoUnitario) || 0);
  }

  private recalcular(): void {
    const subtotal = this.lineas.controls.reduce((acc, _, i) => acc + this.subtotalLinea(i), 0);
    this.totalesCompra.set({ subtotal });
  }

  guardarCompra(): void {
    if (this.formCompra.invalid) {
      this.formCompra.markAllAsTouched();
      return;
    }
    const v = this.formCompra.getRawValue();
    this.busy.set(true);
    this.servicio.registrar({
      proveedorId: Number(v.proveedorId),
      sucursalId: Number(v.sucursalId),
      numeroFactura: v.numeroFactura,
      numeroAutorizacion: v.numeroAutorizacion || undefined,
      fecha: v.fecha,
      tipoPago: v.tipoPago,
      observaciones: v.observaciones || undefined,
      lineas: v.lineas.map((l: any) => ({
        productoId: Number(l.productoId),
        cantidad: Number(l.cantidad),
        costoUnitario: Number(l.costoUnitario),
      })),
    }).subscribe({
      next: (c) => {
        this.busy.set(false);
        this.modalCompra.set(false);
        this.success.set(`Compra registrada (factura ${c.numeroFactura}). El stock del almacén fue actualizado.`);
        this.cargarCompras();
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(this.mensaje(err, 'No se pudo registrar la compra.'));
      },
    });
  }

  // -------------------------------------------------------------------- Anular

  abrirAnular(c: Compra): void {
    this.limpiarAlertas();
    this.formAnular.reset({ motivo: '' });
    this.detalle.set(null);
    this.anulando.set(c);
  }

  confirmarAnular(): void {
    const c = this.anulando();
    if (!c || this.formAnular.invalid) {
      this.formAnular.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.servicio.anular(c.id, this.formAnular.value.motivo).subscribe({
      next: () => {
        this.busy.set(false);
        this.anulando.set(null);
        this.detalle.set(null);
        this.success.set('Compra anulada. El stock ingresado fue revertido.');
        this.cargarCompras();
      },
      error: (err) => {
        this.busy.set(false);
        this.anulando.set(null);
        this.error.set(this.mensaje(err, 'No se pudo anular la compra.'));
      },
    });
  }

  // ---------------------------------------------------------------- Devolución

  abrirDevolucion(c: Compra): void {
    this.limpiarAlertas();
    this.servicio.obtener(c.id).subscribe({
      next: (completa) => {
        this.lineasDev.clear();
        completa.detalles.forEach((d) => {
          const g = this.fb.group({
            compraDetalleId: [d.id],
            cantidad: [0, [Validators.min(0), Validators.max(d.cantidad - d.cantidadDevuelta)]],
          });
          g.valueChanges.subscribe(() => this.recalcularDevolucion());
          this.lineasDev.push(g);
        });
        this.formDevolucion.patchValue({ motivo: '', fecha: this.hoy });
        this.totalDevolucion.set(0);
        this.detalle.set(null);
        this.devolviendo.set(completa);
      },
      error: (err) => this.error.set(this.mensaje(err, 'No se pudo cargar la compra.')),
    });
  }

  pendienteLinea(i: number): number {
    const d = this.devolviendo()?.detalles[i];
    return d ? d.cantidad - d.cantidadDevuelta : 0;
  }

  private recalcularDevolucion(): void {
    const c = this.devolviendo();
    if (!c) return;
    const sub = this.lineasDev.controls.reduce(
      (acc, ctrl, i) => acc + (Number(ctrl.value.cantidad) || 0) * c.detalles[i].costoUnitario, 0);
    const prop = c.subtotal ? c.iva / c.subtotal : 0;
    this.totalDevolucion.set(sub * (1 + prop));
  }

  confirmarDevolucion(): void {
    const c = this.devolviendo();
    if (!c) return;
    const lineas = this.lineasDev.controls
      .map((ctrl) => ({ compraDetalleId: ctrl.value.compraDetalleId, cantidad: Number(ctrl.value.cantidad) || 0 }))
      .filter((l) => l.cantidad > 0);
    if (this.formDevolucion.invalid || lineas.length === 0) {
      this.formDevolucion.markAllAsTouched();
      if (lineas.length === 0) this.error.set('Indique la cantidad a devolver de al menos un producto.');
      return;
    }
    this.busy.set(true);
    this.servicio.registrarDevolucion(c.id, {
      motivo: this.formDevolucion.value.motivo,
      fecha: this.formDevolucion.value.fecha,
      lineas,
    }).subscribe({
      next: (d) => {
        this.busy.set(false);
        this.devolviendo.set(null);
        this.detalle.set(null);
        this.success.set(`Devolución ${d.numeroDevolucion} registrada. El stock fue descontado del almacén.`);
        this.cargarCompras();
        this.cargarDevoluciones();
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(this.mensaje(err, 'No se pudo registrar la devolución.'));
      },
    });
  }

  puedeDevolver(c: Compra): boolean {
    return this.estaVigente(c) || c.estado === 'DEVUELTA_PARCIAL';
  }

  /** REGISTRADA, o COMPLETADA (estado con que vienen las compras anteriores a este módulo). */
  estaVigente(c: Compra): boolean {
    return c.estado === 'REGISTRADA' || c.estado === 'COMPLETADA';
  }

  etiquetaEstado(e: string): string {
    return ({ REGISTRADA: 'Registrada', COMPLETADA: 'Completada', DEVUELTA_PARCIAL: 'Devuelta parcial', DEVUELTA: 'Devuelta', ANULADA: 'Anulada' } as any)[e] ?? e;
  }

  private mensaje(err: any, porDefecto: string): string {
    if (err?.status === 403) return 'No cuenta con permisos para realizar esta operación.';
    return err?.error?.message || err?.error?.detail || porDefecto;
  }
}
