export interface PeriodoContableSimple {
  id: number;
  gestion: number;
  mes: number;
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  estado: string;
}

export interface CuentaContableSimple {
  id: number;
  codigo: string;
  nombre: string;
  nivel: number;
  tipo: string;
  naturaleza: string;
  permiteMovimiento: boolean;
}

export interface LineaAsiento {
  id: number;
  cuentaId: number;
  cuentaCodigo: string;
  cuentaNombre: string;
  glosa?: string;
  debe: number;
  haber: number;
}

export interface AsientoDiario {
  id: number;
  numeroAsiento: string;
  fecha: string;
  tipoAsiento: string;
  glosa: string;
  totalDebe: number;
  totalHaber: number;
  estado: string;
  detalles: LineaAsiento[];
}

export interface LibroDiarioResponse {
  periodoSeleccionado: string;
  totalDebeGeneral: number;
  totalHaberGeneral: number;
  estaCuadrado: boolean;
  totalAsientos: number;
  asientos: AsientoDiario[];
}

export interface MovimientoMayor {
  fecha: string;
  numeroAsiento: string;
  tipoAsiento: string;
  glosa: string;
  debe: number;
  haber: number;
  saldoAcumulado: number;
}

export interface CuentaMayor {
  cuentaId: number;
  codigo: string;
  nombre: string;
  tipo: string;
  naturaleza: string;
  saldoAnterior: number;
  totalDebe: number;
  totalHaber: number;
  saldoFinal: number;
  movimientos: MovimientoMayor[];
}

export interface LibroMayorResponse {
  periodoSeleccionado: string;
  fechaInicio?: string;
  fechaFin?: string;
  totalCuentas: number;
  cuentas: CuentaMayor[];
}

export interface FiltroDiario {
  periodoId?: number | null;
  fechaInicio?: string | null;
  fechaFin?: string | null;
  tipoAsiento?: string | null;
  estado?: string | null;
  busqueda?: string | null;
}

export interface FiltroMayor {
  cuentaId?: number | null;
  periodoId?: number | null;
  fechaInicio?: string | null;
  fechaFin?: string | null;
}
