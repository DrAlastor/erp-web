/** CU40 — Inteligencia de Negocio: contratos del backend (/api/inteligencia-negocio). */

export type VistaIn = 'panel' | 'pronostico' | 'abandono' | 'segmentacion' | 'canasta' | 'anomalias' | 'reposicion';
export type Frecuencia = 'MENSUAL' | 'SEMANAL';
export type NivelRiesgo = 'ALTO' | 'MEDIO' | 'BAJO';

export interface Ejecucion {
  id: number;
  estado: 'EN_CURSO' | 'OK' | 'PARCIAL' | 'ERROR';
  inicio: string;
  fin: string | null;
  error: string | null;
}

export type Metricas = Record<string, unknown> & { omitido?: string; error?: string };

export interface ModeloInfo {
  clave: string;
  titulo: string;
  tipo: string;
  tecnica: string;
  pregunta: string;
  metricas: Metricas | null;
}

export interface Resumen {
  servicioConfigurado: boolean;
  entrenado: boolean;
  entrenamientoAutomatico: boolean;
  ultimaEjecucion: Ejecucion | null;
  modelos: ModeloInfo[];
}

export interface EntrenarResponse {
  encolado: boolean;
  mensaje: string;
}

export interface Opcion {
  id: number;
  nombre: string;
}

export interface Opciones {
  sucursales: Opcion[];
  categorias: string[];
}

export interface Lista<T> {
  entrenado: boolean;
  aviso: string | null;
  items: T[];
}

export interface PuntoHistoria {
  periodo: string;
  valor: number;
}

export interface PuntoPronostico {
  periodo: string;
  valor: number;
  bajo: number;
  alto: number;
}

export interface Pronostico {
  entrenado: boolean;
  disponible: boolean;
  aviso: string | null;
  frecuencia: Frecuencia;
  ambito: 'TOTAL' | 'SUCURSAL' | 'CATEGORIA';
  clave: string;
  modelo: string | null;
  origen: 'PRECALCULADO' | 'AL_MOMENTO';
  mape: number | null;
  mapeIngenuo: number | null;
  historia: PuntoHistoria[];
  pronostico: PuntoPronostico[];
}

export interface Factor {
  variable: string;
  valor: number;
}

export interface ClienteRiesgo {
  clienteId: number;
  cliente: string | null;
  nitCi: string | null;
  probabilidad: number;
  nivel: NivelRiesgo;
  factores: Factor[];
}

export interface ClientesRiesgo {
  entrenado: boolean;
  aviso: string | null;
  porNivel: Partial<Record<NivelRiesgo, number>>;
  clientes: ClienteRiesgo[];
}

export interface Segmento {
  segmento: string;
  clientes: number;
  recenciaPromedio: number;
  frecuenciaPromedio: number;
  montoPromedio: number;
  montoTotal: number;
}

export interface ClienteSegmento {
  clienteId: number;
  cliente: string | null;
  segmento: string;
  recenciaDias: number;
  frecuencia: number;
  monto: number;
}

export interface Segmentos {
  entrenado: boolean;
  aviso: string | null;
  segmentos: Segmento[];
  clientes: ClienteSegmento[];
}

export interface ReglaAsociacion {
  productoAId: number;
  productoA: string;
  productoBId: number;
  productoB: string;
  soporte: number;
  confianza: number;
  lift: number;
  ventas: number;
}

export interface Anomalia {
  fecha: string;
  sucursalId: number;
  sucursal: string | null;
  metrica: string;
  valorReal: number;
  valorEsperado: number;
  desviacion: number;
  gravedad: 'ALTA' | 'MEDIA';
}

export interface SugerenciaCompra {
  productoId: number;
  codigo: string | null;
  producto: string | null;
  categoria: string;
  sucursalId: number;
  sucursal: string | null;
  demandaEsperada: number;
  stockActual: number;
  stockSeguridad: number;
  cantidadSugerida: number;
  costoEstimado: number;
  motivo: string;
}
