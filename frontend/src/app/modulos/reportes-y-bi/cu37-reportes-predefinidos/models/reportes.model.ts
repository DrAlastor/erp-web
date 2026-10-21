export type TipoColumna = 'TEXTO' | 'FECHA' | 'ENTERO' | 'DECIMAL' | 'MONEDA';
export type FiltroReporte = 'DESDE' | 'HASTA' | 'SUCURSAL' | 'CATEGORIA' | 'LIMITE';
export type ValorCelda = string | number | null;

export interface ColumnaReporte {
  clave: string;
  etiqueta: string;
  tipo: TipoColumna;
  totalizar: boolean;
}

/** Un reporte del catálogo (`GET /api/reportes/predefinidos`). */
export interface DefinicionReporte {
  codigo: string;
  titulo: string;
  descripcion: string;
  categoria: string;
  categoriaEtiqueta: string;
  filtros: FiltroReporte[];
  columnas: ColumnaReporte[];
}

/** Un reporte generado (`GET /api/reportes/predefinidos/{codigo}`). */
export interface ResultadoReporte {
  codigo: string;
  titulo: string;
  categoria: string;
  generadoEn: string;
  generadoPor: string | null;
  filtros: Record<string, string>;
  columnas: ColumnaReporte[];
  filas: Record<string, ValorCelda>[];
  totales: Record<string, ValorCelda>;
}

export interface Opcion {
  id: number;
  nombre: string;
}

export interface OpcionesReporte {
  sucursales: Opcion[];
  categorias: Opcion[];
}

export interface ParametrosReporte {
  desde?: string | null;
  hasta?: string | null;
  sucursalId?: number | null;
  categoriaId?: number | null;
  limite?: number | null;
}

/** Error que devuelve el `GlobalExceptionHandler` del backend. */
export interface ErrorRespuesta {
  status: number;
  error: string;
  message: string;
}
