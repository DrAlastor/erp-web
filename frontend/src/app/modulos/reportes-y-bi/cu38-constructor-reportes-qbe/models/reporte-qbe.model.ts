export type TipoCampo = 'TEXTO' | 'NUMERO' | 'MONEDA' | 'FECHA' | 'BOOLEANO';
export type AgregacionQbe = 'SUMA' | 'PROMEDIO' | 'CONTEO' | 'MINIMO' | 'MAXIMO';
export type GranularidadQbe = 'FECHA_HORA' | 'DIA' | 'MES' | 'ANIO';
export type OperadorLogico = 'AND' | 'OR';
export type FormatoExportacion = 'XLSX' | 'PDF';

export interface ColumnaQbe {
  campo: string;
  agregacion: AgregacionQbe | null;
  granularidad: GranularidadQbe | null;
}

export interface FiltroQbe {
  campo: string;
  operador: string;
  valor: string | null;
  valor2: string | null;
}

export interface OrdenQbe {
  /** Posición de la columna en el reporte, desde 0. */
  columna: number;
  descendente: boolean;
}

/** Definición del reporte: lo que se ejecuta y lo que se guarda como plantilla. */
export interface DefinicionQbe {
  entidad: string;
  columnas: ColumnaQbe[];
  filtros: FiltroQbe[];
  operadorLogico: OperadorLogico;
  orden: OrdenQbe[];
}

export interface CampoQbe {
  clave: string;
  etiqueta: string;
  tipo: TipoCampo;
}

export interface EntidadQbe {
  codigo: string;
  etiqueta: string;
  modulo: string;
  campos: CampoQbe[];
}

export interface OperadorQbe {
  codigo: string;
  etiqueta: string;
  valores: number;
  tipos: TipoCampo[];
}

export interface AgregacionOpcion {
  codigo: AgregacionQbe;
  etiqueta: string;
  tipos: TipoCampo[];
}

export interface CatalogoQbe {
  entidades: EntidadQbe[];
  operadores: OperadorQbe[];
  agregaciones: AgregacionOpcion[];
  granularidades: { codigo: GranularidadQbe; etiqueta: string }[];
}

export interface ColumnaResultado {
  clave: string;
  etiqueta: string;
  tipo: TipoCampo;
}

export interface ResultadoQbe {
  columnas: ColumnaResultado[];
  filas: Record<string, string | number | boolean | null>[];
  total: number;
  pagina: number;
  tamanio: number;
  totalPaginas: number;
  descripcionFiltros: string;
}

export interface PlantillaQbe {
  id: number;
  nombre: string;
  descripcion: string | null;
  modulo: string;
  entidad: string;
  entidadEtiqueta: string;
  autor: string | null;
  fechaCreacion: string;
  definicion: DefinicionQbe;
  advertencias: string[];
}

export interface PlantillaQbeRequest {
  nombre: string;
  descripcion: string | null;
  definicion: DefinicionQbe;
}
