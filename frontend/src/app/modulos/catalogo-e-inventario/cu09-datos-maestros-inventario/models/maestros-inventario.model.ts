export interface SucursalItem {
  id: number;
  nombre: string;
  direccion?: string;
  ciudadNombre?: string;
}

export interface CiudadItem {
  id: number;
  nombre: string;
  pais?: string;
}

export interface Almacen {
  id: number;
  nombre: string;
  codigo: string;
  sucursalId: number;
  sucursalNombre: string;
  ciudadId?: number;
  ciudadNombre?: string;
  direccion?: string;
  telefono?: string;
  horaApertura?: string;
  horaCierre?: string;
  horario?: string;
  capacidadEstimada?: number;
  esPrincipal?: boolean;
  activo: boolean;
  fechaCreacion?: string;
}

export interface CrearAlmacenRequest {
  nombre: string;
  codigo?: string;
  sucursalId?: number;
  ciudadId?: number;
  direccion?: string;
  telefono?: string;
  horaApertura?: string;
  horaCierre?: string;
  capacidadEstimada?: number;
  esPrincipal?: boolean;
}

export interface ActualizarAlmacenRequest {
  nombre: string;
  codigo?: string;
  sucursalId?: number;
  ciudadId?: number;
  direccion?: string;
  telefono?: string;
  horaApertura?: string;
  horaCierre?: string;
  capacidadEstimada?: number;
  esPrincipal?: boolean;
}

export interface Categoria {
  id: number;
  nombre: string;
  descripcion?: string;
  padreId?: number | null;
  padreNombre?: string | null;
  activo: boolean;
  articulosCount: number;
  subcategoriasCount: number;
  fechaCreacion?: string;
}

export interface CategoriaNodo {
  id: number;
  nombre: string;
  descripcion?: string;
  padreId?: number | null;
  activo: boolean;
  articulosCount: number;
  subcategorias: CategoriaNodo[];
  fechaCreacion?: string;
}

export interface CrearCategoriaRequest {
  nombre: string;
  descripcion?: string;
  padreId?: number | null;
}

export interface ActualizarCategoriaRequest {
  nombre: string;
  descripcion?: string;
  padreId?: number | null;
}

export interface UnidadMedida {
  id: number;
  codigo: string;
  nombre: string;
  abreviatura: string;
  activo: boolean;
  productosCount: number;
  fechaCreacion?: string;
}

export interface CrearUnidadMedidaRequest {
  codigo: string;
  nombre: string;
  abreviatura: string;
}

export interface ActualizarUnidadMedidaRequest {
  codigo: string;
  nombre: string;
  abreviatura: string;
}

export type MaestrosTab = 'almacenes' | 'categorias' | 'unidades';
