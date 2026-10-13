export interface Empleado {
  id: number;
  sucursalId: number;
  sucursalNombre: string;
  ci: string;
  nombre: string;
  apellido: string;
  nombreCompleto: string;
  cargo: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  fechaIngreso: string;
  activo: boolean;
  tieneUsuario: boolean;
  usuarioId?: number;
  usuarioEmail?: string;
  username?: string;
  usuarioActivo?: boolean;
}

export interface SucursalItem {
  id: number;
  nombre: string;
  direccion?: string;
  telefono?: string;
  activo: boolean;
}

export interface CrearEmpleadoDto {
  ci: string;
  sucursalId: number;
  nombre: string;
  apellido: string;
  cargo: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  fechaIngreso: string;
}

export interface ActualizarEmpleadoDto {
  ci: string;
  sucursalId: number;
  nombre: string;
  apellido: string;
  cargo: string;
  telefono?: string;
  email?: string;
  direccion?: string;
  fechaIngreso: string;
  activo?: boolean;
}
