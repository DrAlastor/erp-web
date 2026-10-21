export type CondicionPago = 'CONTADO' | 'CREDITO';

export interface Proveedor {
  id: number;
  razonSocial: string;
  nitCi: string;
  contacto: string | null;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
  ciudad: string | null;
  condicionPago: CondicionPago;
  diasCredito: number;
  activo: boolean;
  fechaCreacion: string;
}

export interface ProveedorPage {
  content: Proveedor[];
  totalElements: number;
  totalPages: number;
}

export interface ProveedorRequest {
  razonSocial: string;
  nitCi: string;
  contacto: string | null;
  telefono: string | null;
  email: string | null;
  direccion: string | null;
  ciudad: string | null;
  condicionPago: CondicionPago;
  diasCredito: number;
}

export interface ProveedorOpcion {
  id: number;
  razonSocial: string;
  nitCi: string;
  condicionPago: CondicionPago;
  diasCredito: number;
}

export interface DocumentoHistorial {
  id: number;
  numero: string;
  fecha: string;
  total: number;
  estado: string;
}

export interface HistorialProveedor {
  proveedor: Proveedor;
  ordenes: DocumentoHistorial[];
  compras: DocumentoHistorial[];
  saldoPendiente: number;
  ordenesAbiertas: number;
}
