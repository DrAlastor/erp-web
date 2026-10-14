export type MetodoCosteo = 'PROMEDIO' | 'PEPS';

export interface KardexLinea {
  id: number;
  fecha: string;
  tipoMovimiento: string;
  referenciaDoc: string | null;
  motivo: string | null;
  creadoPor: string;
  entradaCantidad: number | null;
  entradaCostoUnitario: number | null;
  entradaValor: number | null;
  salidaCantidad: number | null;
  salidaCostoUnitario: number | null;
  salidaValor: number | null;
  saldoCantidad: number;
  saldoCostoUnitario: number;
  saldoValor: number;
}

export interface KardexConsulta {
  productoId: number;
  productoNombre: string;
  productoSku: string;
  unidadMedida: string;
  almacenId: number;
  almacenNombre: string;
  metodo: MetodoCosteo;
  metodoEtiqueta: string;
  fechaInicio: string | null;
  fechaFin: string | null;
  saldoInicialCantidad: number;
  saldoInicialValor: number;
  totalEntradasCantidad: number;
  totalEntradasValor: number;
  totalSalidasCantidad: number;
  totalSalidasValor: number;
  saldoFinalCantidad: number;
  saldoFinalCostoUnitario: number;
  saldoFinalValor: number;
  movimientos: KardexLinea[];
}

export interface KardexValoracionItem {
  productoId: number;
  productoNombre: string;
  productoSku: string;
  unidadMedida: string;
  cantidad: number;
  costoUnitario: number;
  valor: number;
}

export interface KardexValoracion {
  almacenId: number;
  almacenNombre: string;
  metodo: MetodoCosteo;
  metodoEtiqueta: string;
  totalCantidad: number;
  totalValor: number;
  items: KardexValoracionItem[];
}

export interface KardexFiltros {
  productoId: number;
  almacenId: number;
  metodo: MetodoCosteo;
  /** Fecha en formato yyyy-MM-dd. */
  fechaInicio?: string;
  /** Fecha en formato yyyy-MM-dd. */
  fechaFin?: string;
  tipoMovimiento?: string;
}
