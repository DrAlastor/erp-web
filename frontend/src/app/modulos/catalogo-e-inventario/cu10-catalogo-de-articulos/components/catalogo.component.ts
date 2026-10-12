import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ArticuloService } from '../services/articulo.service';
import { Articulo } from '../models/articulo.model';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './catalogo.component.html',
  styleUrl: './catalogo.component.css'
})
export class CatalogoComponent implements OnInit {
  private readonly articuloService = inject(ArticuloService);

  readonly articulos = signal<Articulo[]>([]);
  readonly cargando = signal(true);
  readonly error = signal<string | null>(null);
  readonly busqueda = signal('');

  readonly articulosFiltrados = computed(() => {
    const termino = this.busqueda().trim().toLowerCase();
    const lista = this.articulos();
    if (!termino) {
      return lista;
    }
    return lista.filter((a) => a.nombre.toLowerCase().includes(termino));
  });

  ngOnInit(): void {
    this.cargarArticulos();
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
        this.error.set('No se pudo cargar el catálogo. Verifique que el backend esté en marcha.');
        this.cargando.set(false);
      }
    });
  }

  onBusquedaChange(valor: string): void {
    this.busqueda.set(valor);
  }

  stockClass(stock: number | null | undefined): string {
    const valor = stock ?? 0;
    if (valor === 0) {
      return 'stock-rojo';
    }
    if (valor < 10) {
      return 'stock-amarillo';
    }
    return 'stock-verde';
  }

  imagenSrc(articulo: Articulo): string {
    return articulo.imagenUrl?.trim()
      ? articulo.imagenUrl
      : 'https://placehold.co/400x300/e2e8f0/64748b?text=Sin+imagen';
  }
}
