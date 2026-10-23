export type MetricaVoz =
  | 'VENTAS'
  | 'PRODUCTOS_VENDIDOS'
  | 'STOCK'
  | 'MOVIMIENTOS_KARDEX'
  | 'CUENTAS_POR_COBRAR'
  | 'COMPRAS';

export type AgrupacionVoz =
  | 'NINGUNA' | 'DIA' | 'MES' | 'SUCURSAL' | 'CLIENTE' | 'VENDEDOR'
  | 'PRODUCTO' | 'CATEGORIA' | 'PROVEEDOR' | 'TIPO' | 'ESTADO';

export type Visualizacion = 'KPI' | 'BARRAS' | 'LINEAS' | 'TABLA';

export type TipoDato = 'TEXTO' | 'FECHA' | 'NUMERO' | 'MONEDA';

/** Consulta estructurada en la que termina un comando de voz; se reenvía al refinar. */
export interface ConsultaReporteVoz {
  metrica: MetricaVoz;
  agrupacion?: AgrupacionVoz | null;
  fechaInicio?: string | null;
  fechaFin?: string | null;
  sucursalId?: number | null;
  productoId?: number | null;
  soloCriticos?: boolean | null;
  limite?: number | null;
}

export interface ColumnaReporte {
  clave: string;
  etiqueta: string;
  tipo: TipoDato;
}

export interface ReporteVoz {
  textoOriginal: string | null;
  origen: 'GEMINI' | 'LOCAL' | 'MANUAL';
  consulta: ConsultaReporteVoz;
  titulo: string;
  interpretacion: string;
  visualizacion: Visualizacion;
  columnas: ColumnaReporte[];
  filas: Record<string, string | number | null>[];
  totales: Record<string, number>;
  advertencias: string[];
  generadoEn: string;
}

export interface OpcionCatalogo {
  codigo: string;
  etiqueta: string;
}

export interface MetricaOpcion {
  codigo: MetricaVoz;
  etiqueta: string;
  usaFechas: boolean;
  admiteProducto: boolean;
  admiteCriticos: boolean;
  agrupaciones: OpcionCatalogo[];
}

export interface Referencia {
  id: number;
  nombre: string;
}

export interface OpcionesReporteVoz {
  metricas: MetricaOpcion[];
  sucursales: Referencia[];
  productos: Referencia[];
  ejemplos: string[];
  iaHabilitada: boolean;
}

/** Turno de la conversación que se reenvía al backend para preguntas de seguimiento. */
export interface TurnoConversacion {
  rol: 'usuario' | 'asistente';
  texto: string;
}

/** Mensaje mostrado en el chat. */
export interface MensajeChat extends TurnoConversacion {
  origen?: AsistenteRespuesta['origen'];
  advertencias?: string[];
  conReporte?: boolean;
}

export interface AsistenteRespuesta {
  respuesta: string;
  reporte: ReporteVoz | null;
  origen: 'GEMINI' | 'LOCAL';
  advertencias: string[];
}
