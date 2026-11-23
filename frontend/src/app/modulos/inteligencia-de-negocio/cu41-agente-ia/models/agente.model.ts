/** CU41 — Agente de IA: contratos del backend (/api/inteligencia-negocio/agente). */

export type Fila = Record<string, unknown>;

export interface Visual {
  titulo: string;
  tipo: 'linea' | 'barras' | 'torta' | 'tabla';
  x: string | null;
  series: string[];
  rango: { bajo: string; alto: string } | null;
}

export interface ResultadoHerramienta {
  filas: Fila[];
  resumen: Fila | null;
  nota: string | null;
  disponible: boolean | null;
  columnasPrivadas: string[];
}

export interface HerramientaUsada {
  nombre: string;
  parametros: Record<string, unknown>;
  ok: boolean;
  error?: string | null;
  resultado?: ResultadoHerramienta | null;
  grafico?: Visual | null;
}

export interface RespuestaAgente {
  idConversacion: number;
  texto: string;
  herramientas: HerramientaUsada[];
  tokens: { entrada: number; salida: number };
}

export interface ConversacionResumen {
  id: number;
  titulo: string;
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface MensajeGuardado {
  id: number;
  rol: 'USUARIO' | 'ASISTENTE';
  contenido: string;
  herramientas: HerramientaUsada[] | null;
  fecha: string;
}

export interface ConversacionDetalle {
  id: number;
  titulo: string;
  mensajes: MensajeGuardado[];
}

export interface GraficoGuardado {
  id: number;
  titulo: string;
  herramienta: string;
  parametros: Record<string, unknown>;
  fechaCreacion: string;
}

export interface GraficoDatos {
  grafico: GraficoGuardado;
  ok: boolean;
  error: string | null;
  visual: Visual | null;
  resultado: ResultadoHerramienta | null;
}

export interface EstadoAgente {
  configurado: boolean;
  modelo: string;
  herramientas: number;
  preguntasPorDia: number;
  preguntasHoy: number;
}

/** Mensaje en pantalla (los del asistente pueden traer datos y gráficos de esta sesión). */
export interface MensajeChat {
  rol: 'USUARIO' | 'ASISTENTE';
  texto: string;
  herramientas: HerramientaUsada[];
  tokens?: { entrada: number; salida: number };
  pendiente?: boolean;
}
