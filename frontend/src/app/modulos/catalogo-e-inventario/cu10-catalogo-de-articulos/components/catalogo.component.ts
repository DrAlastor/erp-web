import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { ArticuloService } from '../services/articulo.service';
import { Articulo, ArticuloRequest, Categoria } from '../models/articulo.model';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './catalogo.component.html',
  styleUrl: './catalogo.component.css'
})
export class CatalogoComponent implements OnInit {
  private readonly articuloService = inject(ArticuloService);

  readonly articulos = signal<Articulo[]>([]);
  readonly categorias = signal<Categoria[]>([]);
  readonly cargando = signal(true);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  // Filtros
  readonly busqueda = signal('');
  readonly categoriaFiltro = signal<string>('');
  readonly estadoFiltro = signal<string>('todos');

  // Modales
  readonly modalForm = signal(false);
  readonly modoEdicion = signal(false);
  readonly articuloEditandoId = signal<number | null>(null);
  readonly modalDetalle = signal<Articulo | null>(null);
  readonly modalEliminar = signal<Articulo | null>(null);

  // Campos del formulario
  formSku = '';
  formNombre = '';
  formDescripcion = '';
  formPrecio: number = 0;
  formStock: number = 0;
  formImagenUrl = '';
  formCategoriaId: number | null = null;
  formActivo = true;

  // Lista computada con filtros combinados
  readonly articulosFiltrados = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    const catId = this.categoriaFiltro();
    const estado = this.estadoFiltro();
    let lista = this.articulos();

    if (termino) {
      lista = lista.filter(
        (a) =>
          a.nombre.toLowerCase().includes(termino) ||
          a.sku.toLowerCase().includes(termino) ||
          (a.descripcion && a.descripcion.toLowerCase().includes(termino))
      );
    }

    if (catId) {
      const numId = Number(catId);
      lista = lista.filter((a) => a.categoria && a.categoria.id === numId);
    }

    if (estado === 'activos') {
      lista = lista.filter((a) => a.activo);
    } else if (estado === 'inactivos') {
      lista = lista.filter((a) => !a.activo);
    }

    return lista;
  });

  ngOnInit(): void {
    this.cargarArticulos();
    this.cargarCategorias();
  }

  cargarArticulos(): void {
    this.cargando.set(true);
    this.error.set(null);

    this.articuloService.listarTodos().subscribe({
      next: (data) => {
        this.articulos.set(data);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se pudo cargar el catálogo de artículos. Verifique la conexión.');
        this.cargando.set(false);
      }
    });
  }

  cargarCategorias(): void {
    this.articuloService.listarCategorias().subscribe({
      next: (data) => this.categorias.set(data),
      error: () => {}
    });
  }

  abrirCrear(): void {
    this.error.set(null);
    this.success.set(null);
    this.modoEdicion.set(false);
    this.articuloEditandoId.set(null);

    this.formSku = '';
    this.formNombre = '';
    this.formDescripcion = '';
    this.formPrecio = 0;
    this.formStock = 0;
    this.formImagenUrl = '';
    this.formCategoriaId = this.categorias().length > 0 ? this.categorias()[0].id : null;
    this.formActivo = true;

    this.modalForm.set(true);
  }

  abrirEditar(articulo: Articulo): void {
    this.error.set(null);
    this.success.set(null);
    this.modoEdicion.set(true);
    this.articuloEditandoId.set(articulo.id);

    this.formSku = articulo.sku;
    this.formNombre = articulo.nombre;
    this.formDescripcion = articulo.descripcion || '';
    this.formPrecio = articulo.precio;
    this.formStock = articulo.stock ?? 0;
    this.formImagenUrl = articulo.imagenUrl || '';
    this.formCategoriaId = articulo.categoria ? articulo.categoria.id : null;
    this.formActivo = articulo.activo;

    this.modalForm.set(true);
  }

  abrirVer(articulo: Articulo): void {
    this.error.set(null);
    this.success.set(null);
    this.modalDetalle.set(articulo);
  }

  abrirEliminar(articulo: Articulo): void {
    this.error.set(null);
    this.success.set(null);
    this.modalEliminar.set(articulo);
  }

  cerrarModales(): void {
    this.modalForm.set(false);
    this.modalDetalle.set(null);
    this.modalEliminar.set(null);
  }

  guardar(): void {
    if (!this.formSku.trim()) {
      this.error.set('El código SKU es obligatorio.');
      return;
    }
    if (!this.formNombre.trim()) {
      this.error.set('El nombre del artículo es obligatorio.');
      return;
    }
    if (this.formPrecio <= 0) {
      this.error.set('El precio debe ser mayor a 0 Bs.');
      return;
    }
    if (this.formStock < 0) {
      this.error.set('El stock no puede ser negativo.');
      return;
    }

    this.busy.set(true);
    this.error.set(null);

    const payload: ArticuloRequest = {
      sku: this.formSku.trim().toUpperCase(),
      nombre: this.formNombre.trim(),
      descripcion: this.formDescripcion.trim() || undefined,
      precio: Number(this.formPrecio),
      stock: Number(this.formStock),
      imagenUrl: this.formImagenUrl.trim() || undefined,
      categoriaId: this.formCategoriaId ? Number(this.formCategoriaId) : null,
      activo: this.formActivo
    };

    if (this.modoEdicion()) {
      const id = this.articuloEditandoId()!;
      this.articuloService.actualizar(id, payload).subscribe({
        next: (res) => {
          this.busy.set(false);
          this.cerrarModales();
          this.success.set(`Artículo '${res.nombre}' actualizado correctamente.`);
          this.cargarArticulos();
        },
        error: (err: HttpErrorResponse) => {
          this.busy.set(false);
          this.error.set(err.error?.message || 'Error al actualizar el artículo.');
        }
      });
    } else {
      this.articuloService.crear(payload).subscribe({
        next: (res) => {
          this.busy.set(false);
          this.cerrarModales();
          this.success.set(`Artículo '${res.nombre}' registrado con éxito.`);
          this.cargarArticulos();
        },
        error: (err: HttpErrorResponse) => {
          this.busy.set(false);
          this.error.set(err.error?.message || 'Error al crear el artículo. Verifique que el SKU no esté duplicado.');
        }
      });
    }
  }

  confirmarEliminar(): void {
    const articulo = this.modalEliminar();
    if (!articulo || this.busy()) return;

    this.busy.set(true);
    this.error.set(null);

    this.articuloService.eliminar(articulo.id).subscribe({
      next: () => {
        this.busy.set(false);
        this.cerrarModales();
        this.success.set(`Artículo '${articulo.nombre}' dado de baja exitosamente.`);
        this.cargarArticulos();
      },
      error: (err: HttpErrorResponse) => {
        this.busy.set(false);
        this.error.set(err.error?.message || 'Error al desactivar el artículo.');
      }
    });
  }

  stockClass(stock: number | null | undefined): string {
    const valor = stock ?? 0;
    if (valor === 0) return 'stock-badge stock-rojo';
    if (valor < 10) return 'stock-badge stock-amarillo';
    return 'stock-badge stock-verde';
  }

  imagenSrc(articulo: Articulo): string {
    return articulo.imagenUrl?.trim()
      ? articulo.imagenUrl
      : 'https://placehold.co/80x80/f1f5f9/64748b?text=SKU';
  }
}
