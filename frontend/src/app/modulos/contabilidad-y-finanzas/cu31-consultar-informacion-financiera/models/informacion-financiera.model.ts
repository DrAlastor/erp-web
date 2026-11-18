export interface NodoFinanciero {
  id: number;
  codigo: string;
  nombre: string;
  nivel: number;
  tipo: string;
  naturaleza: string;
  permiteMovimiento: boolean;
  saldo: number;
  porcentajeVertical: number;
  subcuentas: NodoFinanciero[];
}

export interface CuadreBalance {
  totalActivos: number;
  totalPasivos: number;
  totalPatrimonioCuentas: number;
  utilidadEjercicio: number;
  totalPatrimonioNeto: number;
  totalPasivoMasPatrimonio: number;
  diferencia: number;
  estaCuadrado: boolean;
}

export interface BalanceGeneralResponse {
  gestion: number;
  mesCorte: number;
  fechaCorte: string;
  periodoNombre: string;
  nivelDetalle: number;
  activos: NodoFinanciero[];
  pasivos: NodoFinanciero[];
  patrimonio: NodoFinanciero[];
  cuadre: CuadreBalance;
}

export interface RubroResultado {
  clave: string;
  titulo: string;
  monto: number;
  porcentajeVertical: number;
}

export interface EstadoResultadosResponse {
  gestion: number;
  fechaInicio: string;
  fechaFin: string;
  periodoNombre: string;
  nivelDetalle: number;
  totalIngresosOperativos: number;
  totalCostoVentas: number;
  utilidadBruta: number;
  totalGastosOperativos: number;
  resultadoOperativo: number;
  otrosIngresos: number;
  otrosGastos: number;
  utilidadNeta: number;
  margenBrutoPorcentaje: number;
  margenNetoPorcentaje: number;
  resumenEscalonado: RubroResultado[];
  detalleIngresos: NodoFinanciero[];
  detalleEgresos: NodoFinanciero[];
}

export interface FiltroBalance {
  gestion: number | null;
  mesCorte: number | null;
  fechaCorte: string | null;
  nivel: number;
}

export interface FiltroResultados {
  gestion: number | null;
  fechaInicio: string | null;
  fechaFin: string | null;
  nivel: number;
}
