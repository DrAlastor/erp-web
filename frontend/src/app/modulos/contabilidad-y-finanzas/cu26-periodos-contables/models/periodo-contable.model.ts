export type EstadoPeriodo = 'ABIERTO' | 'CERRADO';

export interface PeriodoContable {
  id: number;
  gestion: number;
  mes: number;
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoPeriodo;
  fechaCierre: string | null;
  cantidadAsientos: number;
}

export interface Gestion {
  gestion: number;
  abiertos: number;
  cerrados: number;
  /** Periodos creados al generar la gestión (0 en una consulta). */
  creados: number;
  periodos: PeriodoContable[];
}

/** Qué acción admite un periodo según las reglas de cierre secuencial. */
export type AccionPeriodo = 'cerrar' | 'reabrir' | null;
