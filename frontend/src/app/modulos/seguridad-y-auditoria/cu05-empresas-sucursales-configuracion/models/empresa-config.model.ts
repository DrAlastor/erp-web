export interface Empresa {
  id: string;
  nombre: string;
  razonSocial?: string;
  nit: string;
  direccion?: string;
  telefono?: string;
  email?: string;
  monedaPrincipal: string;
  monedaSecundaria?: string;
  logoUrl?: string;
  activo: boolean;
}

export type ActualizarEmpresaDto = Omit<Empresa, 'id' | 'activo'>;

export interface Sucursal {
  id: number;
  ciudadId: number;
  ciudadNombre?: string;
  nombre: string;
  direccion: string;
  telefono?: string;
  horaApertura?: string;
  horaCierre?: string;
  activo: boolean;
}

export interface GuardarSucursalDto {
  ciudadId: number;
  nombre: string;
  direccion: string;
  telefono?: string;
  horaApertura?: string | null;
  horaCierre?: string | null;
}

export interface Ciudad {
  id: number;
  nombre: string;
  pais: string;
}

export interface Parametro {
  clave: string;
  valor: string;
  descripcion: string;
}
