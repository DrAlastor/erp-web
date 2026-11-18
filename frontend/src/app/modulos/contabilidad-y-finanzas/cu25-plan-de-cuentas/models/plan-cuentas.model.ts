export type TipoCuenta = 'ACTIVO' | 'PASIVO' | 'PATRIMONIO' | 'INGRESO' | 'EGRESO';
export type Naturaleza = 'DEUDORA' | 'ACREEDORA';

export const TIPOS_CUENTA: readonly TipoCuenta[] = ['ACTIVO', 'PASIVO', 'PATRIMONIO', 'INGRESO', 'EGRESO'];

export interface Cuenta {
  id: number;
  codigo: string;
  nombre: string;
  nivel: number;
  tipo: TipoCuenta;
  naturaleza: Naturaleza;
  permiteMovimiento: boolean;
  padreId: number | null;
  codigoPadre: string | null;
  activo: boolean;
  tieneHijos: boolean;
  tieneMovimientos: boolean;
  /** La usa la contabilización automática de ventas: no se puede desactivar. */
  cuentaIntegracion: boolean;
}

export interface CuentaNodo {
  cuenta: Cuenta;
  hijos: CuentaNodo[];
}

export interface CuentaRequest {
  codigo: string;
  nombre: string;
  padreId: number | null;
  tipo: TipoCuenta | null;
  naturaleza: Naturaleza | null;
  permiteMovimiento: boolean;
}

export interface CuentaActualizarRequest {
  nombre: string;
  permiteMovimiento: boolean;
}

/** Una fila visible del árbol aplanado. */
export interface FilaCuenta {
  cuenta: Cuenta;
  profundidad: number;
  expandible: boolean;
  expandida: boolean;
}

export interface FiltroCuentas {
  texto: string;
  tipo: TipoCuenta | '';
  /** 'true' activas, 'false' inactivas, '' todas. */
  activo: '' | 'true' | 'false';
}
