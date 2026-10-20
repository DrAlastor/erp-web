import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Ciudad, Empresa, Parametro, Sucursal } from '../models/empresa-config.model';
import { EmpresaConfigService } from '../services/empresa-config.service';

type Pestana = 'empresa' | 'sucursales' | 'configuracion';

@Component({
  selector: 'app-empresa-config',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './empresa-config.component.html',
  styleUrl: './empresa-config.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmpresaConfigComponent implements OnInit {
  private readonly servicio = inject(EmpresaConfigService);
  private readonly fb = inject(FormBuilder);

  readonly pestana = signal<Pestana>('empresa');
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  // Empresa
  readonly empresa = signal<Empresa | null>(null);
  readonly formEmpresa: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(160)]],
    razonSocial: ['', [Validators.maxLength(200)]],
    nit: ['', [Validators.required, Validators.maxLength(20)]],
    direccion: ['', [Validators.maxLength(255)]],
    telefono: ['', [Validators.maxLength(30)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    monedaPrincipal: ['BOB', [Validators.required, Validators.maxLength(10)]],
    monedaSecundaria: ['', [Validators.maxLength(10)]],
    logoUrl: [''],
  });

  // Sucursales
  readonly sucursales = signal<Sucursal[]>([]);
  readonly ciudades = signal<Ciudad[]>([]);
  readonly filtroEstado = signal<string>('todos');
  readonly filtroTexto = signal<string>('');
  readonly modalSucursal = signal(false);
  readonly sucursalEnEdicion = signal<number | null>(null);
  readonly confirmarEstado = signal<Sucursal | null>(null);
  readonly formSucursal: FormGroup = this.fb.group({
    nombre: ['', [Validators.required, Validators.maxLength(120)]],
    ciudadId: [null, [Validators.required]],
    direccion: ['', [Validators.required, Validators.maxLength(255)]],
    telefono: ['', [Validators.maxLength(30)]],
    horaApertura: [''],
    horaCierre: [''],
  });

  // Configuración
  readonly parametros = signal<Parametro[]>([]);
  readonly formConfig: FormGroup = this.fb.group({});

  ngOnInit(): void {
    this.cargarEmpresa();
    this.cargarCiudades();
    this.cargarSucursales();
    this.cargarConfiguracion();
  }

  seleccionarPestana(p: Pestana): void {
    this.pestana.set(p);
    this.limpiarAlertas();
  }

  limpiarAlertas(): void {
    this.error.set(null);
    this.success.set(null);
  }

  // ---------------------------------------------------------------- Empresa

  cargarEmpresa(): void {
    this.servicio.obtenerEmpresa().subscribe({
      next: (e) => {
        this.empresa.set(e);
        this.formEmpresa.patchValue({
          nombre: e.nombre,
          razonSocial: e.razonSocial ?? '',
          nit: e.nit,
          direccion: e.direccion ?? '',
          telefono: e.telefono ?? '',
          email: e.email ?? '',
          monedaPrincipal: e.monedaPrincipal,
          monedaSecundaria: e.monedaSecundaria ?? '',
          logoUrl: e.logoUrl ?? '',
        });
      },
      error: (err) => this.error.set(this.mensaje(err, 'No se pudieron cargar los datos de la empresa.')),
    });
  }

  guardarEmpresa(): void {
    if (this.formEmpresa.invalid) {
      this.formEmpresa.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.limpiarAlertas();
    this.servicio.actualizarEmpresa(this.formEmpresa.getRawValue()).subscribe({
      next: (e) => {
        this.empresa.set(e);
        this.busy.set(false);
        this.success.set('Datos de la empresa actualizados correctamente.');
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(this.mensaje(err, 'No se pudo guardar la empresa.'));
      },
    });
  }

  // ------------------------------------------------------------- Sucursales

  cargarCiudades(): void {
    this.servicio.listarCiudades().subscribe({
      next: (c) => this.ciudades.set(c),
      error: () => this.ciudades.set([]),
    });
  }

  cargarSucursales(): void {
    const activo = this.filtroEstado() === 'activas' ? true : this.filtroEstado() === 'inactivas' ? false : undefined;
    const search = this.filtroTexto().trim() || undefined;
    this.servicio.listarSucursales({ activo, search }).subscribe({
      next: (s) => this.sucursales.set(s),
      error: (err) => this.error.set(this.mensaje(err, 'No se pudieron cargar las sucursales.')),
    });
  }

  abrirModalSucursal(s?: Sucursal): void {
    this.limpiarAlertas();
    this.sucursalEnEdicion.set(s?.id ?? null);
    this.formSucursal.reset({
      nombre: s?.nombre ?? '',
      ciudadId: s?.ciudadId ?? this.ciudades()[0]?.id ?? null,
      direccion: s?.direccion ?? '',
      telefono: s?.telefono ?? '',
      horaApertura: s?.horaApertura?.substring(0, 5) ?? '',
      horaCierre: s?.horaCierre?.substring(0, 5) ?? '',
    });
    this.modalSucursal.set(true);
  }

  cerrarModalSucursal(): void {
    this.modalSucursal.set(false);
  }

  guardarSucursal(): void {
    if (this.formSucursal.invalid) {
      this.formSucursal.markAllAsTouched();
      return;
    }
    const v = this.formSucursal.getRawValue();
    const dto = {
      ciudadId: Number(v.ciudadId),
      nombre: v.nombre,
      direccion: v.direccion,
      telefono: v.telefono || undefined,
      horaApertura: v.horaApertura || null,
      horaCierre: v.horaCierre || null,
    };
    const id = this.sucursalEnEdicion();
    this.busy.set(true);
    const peticion = id == null ? this.servicio.crearSucursal(dto) : this.servicio.actualizarSucursal(id, dto);
    peticion.subscribe({
      next: () => {
        this.busy.set(false);
        this.modalSucursal.set(false);
        this.success.set(id == null ? 'Sucursal creada correctamente.' : 'Sucursal actualizada correctamente.');
        this.cargarSucursales();
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(this.mensaje(err, 'No se pudo guardar la sucursal.'));
      },
    });
  }

  confirmarCambioEstado(s: Sucursal): void {
    this.limpiarAlertas();
    this.confirmarEstado.set(s);
  }

  ejecutarCambioEstado(): void {
    const s = this.confirmarEstado();
    if (!s) return;
    this.busy.set(true);
    this.servicio.cambiarEstadoSucursal(s.id, !s.activo).subscribe({
      next: () => {
        this.busy.set(false);
        this.confirmarEstado.set(null);
        this.success.set(`Sucursal ${s.activo ? 'desactivada' : 'activada'} correctamente.`);
        this.cargarSucursales();
      },
      error: (err) => {
        this.busy.set(false);
        this.confirmarEstado.set(null);
        this.error.set(this.mensaje(err, 'No se pudo cambiar el estado de la sucursal.'));
      },
    });
  }

  // ---------------------------------------------------------- Configuración

  cargarConfiguracion(): void {
    this.servicio.obtenerConfiguracion().subscribe({
      next: (lista) => {
        this.parametros.set(lista);
        lista.forEach((p) => {
          if (!this.formConfig.contains(p.clave)) {
            this.formConfig.addControl(p.clave, this.fb.control(p.valor, [Validators.required, Validators.maxLength(255)]));
          } else {
            this.formConfig.get(p.clave)!.setValue(p.valor);
          }
        });
      },
      error: (err) => this.error.set(this.mensaje(err, 'No se pudo cargar la configuración.')),
    });
  }

  guardarConfiguracion(): void {
    if (this.formConfig.invalid) {
      this.formConfig.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.limpiarAlertas();
    this.servicio.guardarConfiguracion(this.formConfig.getRawValue()).subscribe({
      next: (lista) => {
        this.parametros.set(lista);
        this.busy.set(false);
        this.success.set('Configuración guardada correctamente.');
      },
      error: (err) => {
        this.busy.set(false);
        this.error.set(this.mensaje(err, 'No se pudo guardar la configuración.'));
      },
    });
  }

  private mensaje(err: any, porDefecto: string): string {
    if (err?.status === 403) return 'No cuenta con permisos para realizar esta operación.';
    return err?.error?.message || err?.error?.detail || porDefecto;
  }
}
