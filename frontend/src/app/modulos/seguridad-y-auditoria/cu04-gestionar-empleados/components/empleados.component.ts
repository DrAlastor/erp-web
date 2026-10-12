import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Empleado, SucursalItem } from '../models/empleado.model';
import { EmpleadoService } from '../services/empleado.service';

@Component({
  selector: 'app-empleados',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './empleados.component.html',
  styleUrl: './empleados.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmpleadosComponent implements OnInit {
  private readonly empleadoService = inject(EmpleadoService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);

  // Estados Reactivos
  readonly empleados = signal<Empleado[]>([]);
  readonly sucursales = signal<SucursalItem[]>([]);
  readonly loading = signal<boolean>(false);
  readonly busy = signal<boolean>(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  // Filtros
  readonly filtroSucursal = signal<string>('');
  readonly filtroCargo = signal<string>('');
  readonly filtroEstado = signal<string>('todos');
  readonly filtroSearch = signal<string>('');

  // Modal de Registro / Edición
  readonly modalAbierto = signal<boolean>(false);
  readonly modoEdicion = signal<boolean>(false);
  readonly empleadoEnEdicionId = signal<number | null>(null);

  // Modal de Confirmación de Baja / Alta Lógica
  readonly confirmacionModal = signal<Empleado | null>(null);

  // Departamentos para expedición de CI en Bolivia
  readonly departamentosBolivia = [
    { sigla: 'SC', nombre: 'Santa Cruz' },
    { sigla: 'LP', nombre: 'La Paz' },
    { sigla: 'CB', nombre: 'Cochabamba' },
    { sigla: 'OR', nombre: 'Oruro' },
    { sigla: 'PT', nombre: 'Potosí' },
    { sigla: 'TJ', nombre: 'Tarija' },
    { sigla: 'CH', nombre: 'Chuquisaca' },
    { sigla: 'BE', nombre: 'Beni' },
    { sigla: 'PD', nombre: 'Pando' },
    { sigla: 'EXT', nombre: 'Extranjero' }
  ];

  // Cargos comunes para sugerencias rápidas
  readonly cargosPredefinidos = [
    'Gerente General',
    'Ejecutivo de Ventas',
    'Cajero Principal',
    'Cajero de Turno',
    'Jefe de Almacén',
    'Auxiliar de Almacén',
    'Contador General',
    'Técnico de Soporte',
    'Encargado de Sucursal'
  ];

  // Formulario Reactivo
  readonly form: FormGroup = this.fb.group({
    // Sección 1: Datos Personales
    ciNumero: ['', [Validators.required, Validators.pattern(/^[0-9A-Za-z-]{4,20}$/)]],
    ciExpedido: ['SC', [Validators.required]],
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    apellido: ['', [Validators.required, Validators.maxLength(100)]],
    telefono: ['', [Validators.maxLength(30)]],
    email: ['', [Validators.email, Validators.maxLength(100)]],
    direccion: ['', [Validators.maxLength(255)]],

    // Sección 2: Datos Organizacionales
    cargo: ['', [Validators.required, Validators.maxLength(100)]],
    fechaIngreso: [new Date().toISOString().substring(0, 10), [Validators.required]],
    sucursalId: [null, [Validators.required]]
  });

  ngOnInit(): void {
    this.cargarSucursales();
    this.cargarEmpleados();
  }

  cargarSucursales(): void {
    this.empleadoService.listarSucursales().subscribe({
      next: (data) => this.sucursales.set(data),
      error: () => {
        // Fallback si el catálogo está cargando
        this.sucursales.set([
          { id: 1, nombre: 'Sucursal Central Equipetrol', activo: true },
          { id: 2, nombre: 'Sucursal Calacoto La Paz', activo: true }
        ]);
      }
    });
  }

  cargarEmpleados(): void {
    this.loading.set(true);
    this.error.set(null);

    const sucursalId = this.filtroSucursal() ? Number(this.filtroSucursal()) : undefined;
    const cargo = this.filtroCargo() || undefined;
    const activo = this.filtroEstado() === 'activos'
      ? true
      : this.filtroEstado() === 'inactivos'
      ? false
      : undefined;
    const search = this.filtroSearch().trim() || undefined;

    this.empleadoService.listar({ sucursalId, cargo, activo, search }).subscribe({
      next: (data) => {
        this.empleados.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        const mensaje = err.error?.message ||
          (err.status === 403 ? 'No cuenta con permisos para consultar el catálogo de empleados.' :
           err.status === 500 ? 'Error en el servidor al consultar el registro de colaboradores.' :
           'Error al cargar la lista de colaboradores.');
        this.error.set(mensaje);
      }
    });
  }

  onFiltroChange(): void {
    this.cargarEmpleados();
  }

  abrirModalNuevo(): void {
    this.modoEdicion.set(false);
    this.empleadoEnEdicionId.set(null);
    this.error.set(null);
    this.success.set(null);

    const sucursalPorDefecto = this.sucursales().length > 0 ? this.sucursales()[0].id : null;

    this.form.reset({
      ciNumero: '',
      ciExpedido: 'SC',
      nombre: '',
      apellido: '',
      telefono: '',
      email: '',
      direccion: '',
      cargo: 'Ejecutivo de Ventas',
      fechaIngreso: new Date().toISOString().substring(0, 10),
      sucursalId: sucursalPorDefecto
    });

    this.modalAbierto.set(true);
  }

  abrirModalEditar(emp: Empleado): void {
    this.modoEdicion.set(true);
    this.empleadoEnEdicionId.set(emp.id);
    this.error.set(null);
    this.success.set(null);

    // Separar CI de expedido si viene con formato '1234567-SC' o espacio
    let ciNum = emp.ci;
    let exp = 'SC';
    if (emp.ci.includes('-')) {
      const parts = emp.ci.split('-');
      ciNum = parts[0];
      exp = parts[1] || 'SC';
    }

    this.form.patchValue({
      ciNumero: ciNum,
      ciExpedido: exp,
      nombre: emp.nombre,
      apellido: emp.apellido,
      telefono: emp.telefono || '',
      email: emp.email || '',
      direccion: emp.direccion || '',
      cargo: emp.cargo,
      fechaIngreso: emp.fechaIngreso,
      sucursalId: emp.sucursalId
    });

    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    this.form.reset();
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.busy.set(true);
    this.error.set(null);

    const values = this.form.value;
    const ciCompleto = `${values.ciNumero.trim()}-${values.ciExpedido}`;

    if (this.modoEdicion()) {
      const id = this.empleadoEnEdicionId()!;
      this.empleadoService.actualizar(id, {
        ci: ciCompleto,
        sucursalId: Number(values.sucursalId),
        nombre: values.nombre.trim(),
        apellido: values.apellido.trim(),
        cargo: values.cargo.trim(),
        telefono: values.telefono?.trim() || undefined,
        email: values.email?.trim() || undefined,
        direccion: values.direccion?.trim() || undefined,
        fechaIngreso: values.fechaIngreso
      }).subscribe({
        next: () => {
          this.busy.set(false);
          this.cerrarModal();
          this.success.set('Colaborador actualizado exitosamente.');
          this.cargarEmpleados();
        },
        error: (err) => {
          this.busy.set(false);
          this.error.set(err.error?.message || 'Error al actualizar el colaborador.');
        }
      });
    } else {
      this.empleadoService.crear({
        ci: ciCompleto,
        sucursalId: Number(values.sucursalId),
        nombre: values.nombre.trim(),
        apellido: values.apellido.trim(),
        cargo: values.cargo.trim(),
        telefono: values.telefono?.trim() || undefined,
        email: values.email?.trim() || undefined,
        direccion: values.direccion?.trim() || undefined,
        fechaIngreso: values.fechaIngreso
      }).subscribe({
        next: () => {
          this.busy.set(false);
          this.cerrarModal();
          this.success.set('Colaborador registrado exitosamente en el maestro.');
          this.cargarEmpleados();
        },
        error: (err) => {
          this.busy.set(false);
          this.error.set(err.error?.message || 'Error al registrar el colaborador.');
        }
      });
    }
  }

  solicitarCambioEstado(emp: Empleado): void {
    this.confirmacionModal.set(emp);
  }

  cancelarCambioEstado(): void {
    this.confirmacionModal.set(null);
  }

  confirmarCambioEstado(): void {
    const emp = this.confirmacionModal();
    if (!emp) return;

    this.busy.set(true);
    const nuevoEstado = !emp.activo;

    this.empleadoService.cambiarEstado(emp.id, nuevoEstado).subscribe({
      next: () => {
        this.busy.set(false);
        this.confirmacionModal.set(null);
        this.success.set(
          nuevoEstado
            ? `Colaborador '${emp.nombreCompleto}' reactivado exitosamente.`
            : `Colaborador '${emp.nombreCompleto}' desactivado. Su acceso al sistema ha sido inhabilitado.`
        );
        this.cargarEmpleados();
      },
      error: (err) => {
        this.busy.set(false);
        this.confirmacionModal.set(null);
        this.error.set(err.error?.message || 'Error al actualizar el estado del colaborador.');
      }
    });
  }

  limpiarAlertas(): void {
    this.error.set(null);
    this.success.set(null);
  }

  crearUsuarioParaEmpleado(emp: Empleado): void {
    this.router.navigate(['/app'], {
      queryParams: {
        modulo: 'seguridad-y-auditoria',
        funcion: 'cu02-gestionar-usuarios',
        empleadoId: emp.id
      }
    });
  }
}
