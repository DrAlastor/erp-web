import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MaestrosInventarioService } from '../services/maestros-inventario.service';
import {
  Almacen,
  Categoria,
  CategoriaNodo,
  CiudadItem,
  MaestrosTab,
  SucursalItem,
  UnidadMedida
} from '../models/maestros-inventario.model';

@Component({
  selector: 'app-datos-maestros-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './datos-maestros-inventario.component.html',
  styleUrls: ['./datos-maestros-inventario.component.scss']
})
export class DatosMaestrosInventarioComponent implements OnInit {
  private readonly maestrosService = inject(MaestrosInventarioService);
  private readonly fb = inject(FormBuilder);

  // Pestaña activa
  readonly tabActiva = signal<MaestrosTab>('almacenes');

  // Estado de carga y mensajes
  readonly cargando = signal<boolean>(false);
  readonly guardando = signal<boolean>(false);
  readonly errorMsg = signal<string | null>(null);
  readonly successMsg = signal<string | null>(null);

  // Listas de datos reactivas
  readonly almacenes = signal<Almacen[]>([]);
  readonly categoriasArbol = signal<CategoriaNodo[]>([]);
  readonly categoriasPlanas = signal<Categoria[]>([]);
  readonly unidades = signal<UnidadMedida[]>([]);
  readonly sucursales = signal<SucursalItem[]>([]);
  readonly ciudades = signal<CiudadItem[]>([
    { id: 1, nombre: 'Santa Cruz de la Sierra' },
    { id: 2, nombre: 'La Paz' },
    { id: 3, nombre: 'Cochabamba' },
    { id: 4, nombre: 'Sucre' },
    { id: 5, nombre: 'Tarija' },
    { id: 6, nombre: 'Oruro' },
    { id: 7, nombre: 'Potosí' },
    { id: 8, nombre: 'Trinidad (Beni)' },
    { id: 9, nombre: 'Cobija (Pando)' }
  ]);

  // Filtros de búsqueda
  readonly searchAlmacenes = signal<string>('');
  readonly filtroSucursalAlmacen = signal<string>('');
  readonly filtroEstadoAlmacen = signal<string>('todos');

  readonly searchCategorias = signal<string>('');
  readonly vistaArbol = signal<boolean>(true);
  readonly nodosExpandidos = signal<Set<number>>(new Set());

  readonly searchUnidades = signal<string>('');

  // Modales
  readonly modalAlmacenAbierto = signal<boolean>(false);
  readonly almacenEditando = signal<Almacen | null>(null);
  formAlmacen!: FormGroup;

  readonly modalCategoriaAbierto = signal<boolean>(false);
  readonly categoriaEditando = signal<Categoria | null>(null);
  readonly categoriaPadrePreseleccionadaId = signal<number | null>(null);
  formCategoria!: FormGroup;

  readonly modalUnidadAbierto = signal<boolean>(false);
  readonly unidadEditando = signal<UnidadMedida | null>(null);
  formUnidad!: FormGroup;

  // Modal Confirmación de Eliminación
  readonly modalEliminarAbierto = signal<boolean>(false);
  readonly entidadEliminarTipo = signal<'almacen' | 'categoria' | 'unidad' | null>(null);
  readonly entidadEliminarId = signal<number | null>(null);
  readonly entidadEliminarNombre = signal<string>('');

  // Computeds para filtros
  readonly almacenesFiltrados = computed(() => {
    const q = this.searchAlmacenes().trim().toLowerCase();
    const est = this.filtroEstadoAlmacen();

    return this.almacenes().filter(a => {
      const matchQ = !q ||
        a.nombre.toLowerCase().includes(q) ||
        a.codigo.toLowerCase().includes(q) ||
        (a.direccion && a.direccion.toLowerCase().includes(q)) ||
        (a.ciudadNombre && a.ciudadNombre.toLowerCase().includes(q));

      const matchEst = est === 'todos' ||
        (est === 'activos' && a.activo) ||
        (est === 'inactivos' && !a.activo);

      return matchQ && matchEst;
    });
  });

  readonly unidadesFiltradas = computed(() => {
    const q = this.searchUnidades().trim().toLowerCase();
    if (!q) return this.unidades();

    return this.unidades().filter(u =>
      u.codigo.toLowerCase().includes(q) ||
      u.nombre.toLowerCase().includes(q) ||
      u.abreviatura.toLowerCase().includes(q)
    );
  });

  // Métricas rápidas
  readonly totalAlmacenes = computed(() => this.almacenes().length);
  readonly totalAlmacenesActivos = computed(() => this.almacenes().filter(a => a.activo).length);
  readonly totalCategorias = computed(() => this.categoriasPlanas().length);
  readonly totalUnidades = computed(() => this.unidades().length);

  ngOnInit(): void {
    this.initForms();
    this.cargarTodo();
  }

  private initForms(): void {
    this.formAlmacen = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(120)]],
      ciudadId: [1, [Validators.required]],
      direccion: ['', [Validators.maxLength(255)]],
      telefono: ['', [Validators.maxLength(30)]],
      horaApertura: ['08:30'],
      horaCierre: ['19:00']
    });

    this.formCategoria = this.fb.group({
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      descripcion: ['', [Validators.maxLength(500)]],
      padreId: [null]
    });

    this.formUnidad = this.fb.group({
      codigo: ['', [Validators.required, Validators.maxLength(20)]],
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      abreviatura: ['', [Validators.required, Validators.maxLength(15)]]
    });
  }

  cambiarTab(tab: MaestrosTab): void {
    this.tabActiva.set(tab);
    this.limpiarMensajes();
  }

  cargarTodo(): void {
    this.cargando.set(true);
    this.maestrosService.listarSucursales().subscribe({
      next: (suc) => this.sucursales.set(suc),
      error: () => {}
    });

    this.maestrosService.listarCiudades().subscribe({
      next: (ciudades) => this.ciudades.set(ciudades),
      error: () => {}
    });

    this.cargarAlmacenes();
    this.cargarCategorias();
    this.cargarUnidades();
  }

  cargarAlmacenes(): void {
    this.maestrosService.listarAlmacenes().subscribe({
      next: (data) => {
        this.almacenes.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        this.cargando.set(false);
        this.mostrarError(err?.error?.message || 'Error al cargar las sucursales.');
      }
    });
  }

  cargarCategorias(): void {
    this.maestrosService.listarCategorias().subscribe({
      next: (data) => this.categoriasPlanas.set(data),
      error: () => {}
    });

    this.maestrosService.obtenerArbol().subscribe({
      next: (arbol) => {
        this.categoriasArbol.set(arbol);
        // Expandir por defecto las raíces
        const expandidos = new Set<number>();
        arbol.forEach(n => expandidos.add(n.id));
        this.nodosExpandidos.set(expandidos);
      },
      error: (err) => {
        this.mostrarError(err?.error?.message || 'Error al cargar el árbol de categorías.');
      }
    });
  }

  cargarUnidades(): void {
    this.maestrosService.listarUnidades().subscribe({
      next: (data) => this.unidades.set(data),
      error: (err) => {
        this.mostrarError(err?.error?.message || 'Error al cargar las unidades de medida.');
      }
    });
  }

  // ==========================================
  // Árbol de Categorías: Toggle expansión
  // ==========================================
  alternarExpansionNodo(nodoId: number): void {
    const current = new Set(this.nodosExpandidos());
    if (current.has(nodoId)) {
      current.delete(nodoId);
    } else {
      current.add(nodoId);
    }
    this.nodosExpandidos.set(current);
  }

  estaExpandido(nodoId: number): boolean {
    return this.nodosExpandidos().has(nodoId);
  }

  expandirTodo(): void {
    const expandidos = new Set<number>();
    const recorrer = (nodos: CategoriaNodo[]) => {
      nodos.forEach(n => {
        expandidos.add(n.id);
        if (n.subcategorias?.length) recorrer(n.subcategorias);
      });
    };
    recorrer(this.categoriasArbol());
    this.nodosExpandidos.set(expandidos);
  }

  colapsarTodo(): void {
    this.nodosExpandidos.set(new Set());
  }

  // ==========================================
  // OPERACIONES: SUCURSAL / ALMACÉN FÍSICO
  // ==========================================
  abrirModalCrearAlmacen(): void {
    this.almacenEditando.set(null);
    this.formAlmacen.reset({
      nombre: '',
      ciudadId: this.ciudades().length ? this.ciudades()[0].id : 1,
      direccion: '',
      telefono: '',
      horaApertura: '08:30',
      horaCierre: '19:00'
    });
    this.limpiarMensajes();
    this.modalAlmacenAbierto.set(true);
  }

  abrirModalEditarAlmacen(almacen: Almacen): void {
    this.almacenEditando.set(almacen);
    
    // Resolver ciudadId por ID o por nombre como fallback
    let ciudadIdResuelta = almacen.ciudadId;
    if (!ciudadIdResuelta && almacen.ciudadNombre) {
      const encontrada = this.ciudades().find(c =>
        c.nombre.toLowerCase().includes(almacen.ciudadNombre!.toLowerCase()) ||
        almacen.ciudadNombre!.toLowerCase().includes(c.nombre.toLowerCase())
      );
      if (encontrada) ciudadIdResuelta = encontrada.id;
    }
    if (!ciudadIdResuelta) ciudadIdResuelta = 1;

    this.formAlmacen.patchValue({
      nombre: almacen.nombre,
      ciudadId: ciudadIdResuelta,
      direccion: almacen.direccion || '',
      telefono: almacen.telefono || '',
      horaApertura: almacen.horaApertura || '08:30',
      horaCierre: almacen.horaCierre || '19:00'
    });
    this.limpiarMensajes();
    this.modalAlmacenAbierto.set(true);
  }

  cerrarModalAlmacen(): void {
    this.modalAlmacenAbierto.set(false);
    this.almacenEditando.set(null);
  }

  guardarAlmacen(): void {
    if (this.formAlmacen.invalid) {
      this.formAlmacen.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    const formVal = this.formAlmacen.value;
    const req = {
      nombre: formVal.nombre.trim(),
      ciudadId: Number(formVal.ciudadId) || 1,
      direccion: formVal.direccion ? formVal.direccion.trim() : '',
      telefono: formVal.telefono ? formVal.telefono.trim() : '',
      horaApertura: formVal.horaApertura || '08:30',
      horaCierre: formVal.horaCierre || '19:00'
    };

    const actual = this.almacenEditando();
    if (actual) {
      this.maestrosService.actualizarAlmacen(actual.id, req).subscribe({
        next: () => {
          this.guardando.set(false);
          this.cerrarModalAlmacen();
          this.mostrarExito('Sucursal actualizada correctamente.');
          this.cargarAlmacenes();
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.message || 'Error al actualizar la sucursal.');
        }
      });
    } else {
      this.maestrosService.crearAlmacen(req).subscribe({
        next: () => {
          this.guardando.set(false);
          this.cerrarModalAlmacen();
          this.mostrarExito('Sucursal registrada exitosamente.');
          this.cargarAlmacenes();
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.message || 'Error al registrar la sucursal.');
        }
      });
    }
  }

  cambiarEstadoAlmacen(almacen: Almacen): void {
    const nuevoEstado = !almacen.activo;
    this.maestrosService.cambiarEstadoAlmacen(almacen.id, nuevoEstado).subscribe({
      next: () => {
        this.mostrarExito(`Sucursal ${almacen.nombre} ${nuevoEstado ? 'activada' : 'desactivada'}.`);
        this.cargarAlmacenes();
      },
      error: (err) => {
        this.mostrarError(err?.error?.message || 'Error al modificar el estado de la sucursal.');
      }
    });
  }

  // ==========================================
  // OPERACIONES: CATEGORÍA
  // ==========================================
  abrirModalCrearCategoria(padreId: number | null = null): void {
    this.categoriaEditando.set(null);
    this.categoriaPadrePreseleccionadaId.set(padreId);
    this.formCategoria.reset({
      nombre: '',
      descripcion: '',
      padreId: padreId
    });
    this.limpiarMensajes();
    this.modalCategoriaAbierto.set(true);
  }

  abrirModalEditarCategoria(cat: Categoria | CategoriaNodo): void {
    const categoriaPlana = this.categoriasPlanas().find(c => c.id === cat.id) || cat;
    this.categoriaEditando.set(categoriaPlana as Categoria);
    this.formCategoria.patchValue({
      nombre: cat.nombre,
      descripcion: cat.descripcion || '',
      padreId: cat.padreId || null
    });
    this.limpiarMensajes();
    this.modalCategoriaAbierto.set(true);
  }

  cerrarModalCategoria(): void {
    this.modalCategoriaAbierto.set(false);
    this.categoriaEditando.set(null);
    this.categoriaPadrePreseleccionadaId.set(null);
  }

  guardarCategoria(): void {
    if (this.formCategoria.invalid) {
      this.formCategoria.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    const formVal = this.formCategoria.value;
    const req = {
      nombre: formVal.nombre,
      descripcion: formVal.descripcion,
      padreId: formVal.padreId ? Number(formVal.padreId) : null
    };

    const actual = this.categoriaEditando();
    if (actual) {
      this.maestrosService.actualizarCategoria(actual.id, req).subscribe({
        next: () => {
          this.guardando.set(false);
          this.cerrarModalCategoria();
          this.mostrarExito('Categoría actualizada exitosamente.');
          this.cargarCategorias();
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.message || 'Error al actualizar la categoría.');
        }
      });
    } else {
      this.maestrosService.crearCategoria(req).subscribe({
        next: () => {
          this.guardando.set(false);
          this.cerrarModalCategoria();
          this.mostrarExito('Categoría creada exitosamente.');
          this.cargarCategorias();
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.message || 'Error al crear la categoría.');
        }
      });
    }
  }

  cambiarEstadoCategoria(cat: Categoria | CategoriaNodo): void {
    const nuevoEstado = !cat.activo;
    this.maestrosService.cambiarEstadoCategoria(cat.id, nuevoEstado).subscribe({
      next: () => {
        this.mostrarExito(`Categoría ${cat.nombre} ${nuevoEstado ? 'activada' : 'desactivada'}.`);
        this.cargarCategorias();
      },
      error: (err) => {
        this.mostrarError(err?.error?.message || 'Error al modificar el estado de la categoría.');
      }
    });
  }

  // Lista de posibles categorías padre para el selector (evita a sí misma)
  categoriasPadreDisponibles = computed(() => {
    const editandoId = this.categoriaEditando()?.id;
    if (!editandoId) return this.categoriasPlanas();
    return this.categoriasPlanas().filter(c => c.id !== editandoId);
  });

  // ==========================================
  // OPERACIONES: UNIDAD DE MEDIDA
  // ==========================================
  abrirModalCrearUnidad(): void {
    this.unidadEditando.set(null);
    this.formUnidad.reset({
      codigo: '',
      nombre: '',
      abreviatura: ''
    });
    this.limpiarMensajes();
    this.modalUnidadAbierto.set(true);
  }

  abrirModalEditarUnidad(u: UnidadMedida): void {
    this.unidadEditando.set(u);
    this.formUnidad.patchValue({
      codigo: u.codigo,
      nombre: u.nombre,
      abreviatura: u.abreviatura
    });
    this.limpiarMensajes();
    this.modalUnidadAbierto.set(true);
  }

  cerrarModalUnidad(): void {
    this.modalUnidadAbierto.set(false);
    this.unidadEditando.set(null);
  }

  guardarUnidad(): void {
    if (this.formUnidad.invalid) {
      this.formUnidad.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    const formVal = this.formUnidad.value;
    const req = {
      codigo: formVal.codigo.trim().toUpperCase(),
      nombre: formVal.nombre.trim(),
      abreviatura: formVal.abreviatura.trim()
    };

    const actual = this.unidadEditando();
    if (actual) {
      this.maestrosService.actualizarUnidad(actual.id, req).subscribe({
        next: () => {
          this.guardando.set(false);
          this.cerrarModalUnidad();
          this.mostrarExito('Unidad de medida actualizada.');
          this.cargarUnidades();
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.message || 'Error al actualizar unidad de medida.');
        }
      });
    } else {
      this.maestrosService.crearUnidad(req).subscribe({
        next: () => {
          this.guardando.set(false);
          this.cerrarModalUnidad();
          this.mostrarExito('Unidad de medida creada.');
          this.cargarUnidades();
        },
        error: (err) => {
          this.guardando.set(false);
          this.mostrarError(err?.error?.message || 'Error al registrar unidad de medida.');
        }
      });
    }
  }

  cambiarEstadoUnidad(u: UnidadMedida): void {
    const nuevoEstado = !u.activo;
    this.maestrosService.cambiarEstadoUnidad(u.id, nuevoEstado).subscribe({
      next: () => {
        this.mostrarExito(`Unidad ${u.nombre} ${nuevoEstado ? 'activada' : 'desactivada'}.`);
        this.cargarUnidades();
      },
      error: (err) => {
        this.mostrarError(err?.error?.message || 'Error al modificar estado de la unidad.');
      }
    });
  }

  // ==========================================
  // CONFIRMACIÓN DE ELIMINACIÓN
  // ==========================================
  confirmarEliminar(tipo: 'almacen' | 'categoria' | 'unidad', id: number, nombre: string): void {
    this.entidadEliminarTipo.set(tipo);
    this.entidadEliminarId.set(id);
    this.entidadEliminarNombre.set(nombre);
    this.modalEliminarAbierto.set(true);
    this.limpiarMensajes();
  }

  cancelarEliminar(): void {
    this.modalEliminarAbierto.set(false);
    this.entidadEliminarTipo.set(null);
    this.entidadEliminarId.set(null);
    this.entidadEliminarNombre.set('');
  }

  ejecutarEliminacion(): void {
    const tipo = this.entidadEliminarTipo();
    const id = this.entidadEliminarId();
    if (!tipo || !id) return;

    this.guardando.set(true);

    if (tipo === 'almacen') {
      this.maestrosService.eliminarAlmacen(id).subscribe({
        next: () => {
          this.guardando.set(false);
          this.cancelarEliminar();
          this.mostrarExito('Sucursal eliminada correctamente.');
          this.cargarAlmacenes();
        },
        error: (err) => {
          this.guardando.set(false);
          this.cancelarEliminar();
          this.mostrarError(err?.error?.message || 'No se puede eliminar la sucursal debido a colaboradores o stock asociado.');
        }
      });
    } else if (tipo === 'categoria') {
      this.maestrosService.eliminarCategoria(id).subscribe({
        next: () => {
          this.guardando.set(false);
          this.cancelarEliminar();
          this.mostrarExito('Categoría eliminada.');
          this.cargarCategorias();
        },
        error: (err) => {
          this.guardando.set(false);
          this.cancelarEliminar();
          this.mostrarError(err?.error?.message || 'No se puede eliminar la categoría debido a dependencias.');
        }
      });
    } else if (tipo === 'unidad') {
      this.maestrosService.eliminarUnidad(id).subscribe({
        next: () => {
          this.guardando.set(false);
          this.cancelarEliminar();
          this.mostrarExito('Unidad de medida eliminada.');
          this.cargarUnidades();
        },
        error: (err) => {
          this.guardando.set(false);
          this.cancelarEliminar();
          this.mostrarError(err?.error?.message || 'No se puede eliminar la unidad de medida por artículos asociados.');
        }
      });
    }
  }

  // Utilidades de feedback
  mostrarError(msg: string): void {
    this.errorMsg.set(msg);
    this.successMsg.set(null);
  }

  mostrarExito(msg: string): void {
    this.successMsg.set(msg);
    this.errorMsg.set(null);
    setTimeout(() => {
      if (this.successMsg() === msg) {
        this.successMsg.set(null);
      }
    }, 4000);
  }

  limpiarMensajes(): void {
    this.errorMsg.set(null);
    this.successMsg.set(null);
  }
}
