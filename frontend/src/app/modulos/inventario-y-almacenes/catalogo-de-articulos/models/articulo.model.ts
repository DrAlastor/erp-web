export interface Categoria {
  id: number;
  nombre: string;
  descripcion?: string | null;
}

export interface Articulo {
  id: number;
  sku: string;
  nombre: string;
  descripcion?: string | null;
  precio: number;
  stock: number | null;
  imagenUrl?: string | null;
  categoria?: Categoria | null;
  activo: boolean;
  fechaCreacion?: string;
}
