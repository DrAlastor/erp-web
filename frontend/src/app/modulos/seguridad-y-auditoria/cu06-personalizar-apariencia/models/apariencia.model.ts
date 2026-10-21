export type TemaApariencia = 'CLARO' | 'OSCURO';
export type TamanoFuente = 'PEQUENO' | 'NORMAL' | 'GRANDE';
export type Tipografia = 'INTER' | 'ROBOTO' | 'POPPINS' | 'OPEN_SANS';
export type EstiloBordes = 'RECTOS' | 'SUAVES' | 'REDONDEADOS';

/** Apariencia de la empresa, tal como la devuelve `GET /api/seguridad/apariencia`. */
export interface Apariencia {
  tema: TemaApariencia;
  colorPrimario: string;
  colorSecundario: string;
  colorAcento: string;
  tamanoFuente: TamanoFuente;
  tipografia: Tipografia;
  bordes: EstiloBordes;
  tieneLogo: boolean;
  logoVersion: number;
  encabezadoFactura: string | null;
  pieFactura: string | null;
  fechaModificacion: string | null;
  modificadoPor: string | null;
}

/** Cuerpo de `PUT /api/seguridad/apariencia`. El logo va por su propio endpoint. */
export type ActualizarApariencia = Pick<
  Apariencia,
  | 'tema'
  | 'colorPrimario'
  | 'colorSecundario'
  | 'colorAcento'
  | 'tamanoFuente'
  | 'tipografia'
  | 'bordes'
  | 'encabezadoFactura'
  | 'pieFactura'
>;

/** Marca NexoERP: lo que ve una empresa que todavía no personalizó nada. */
export const APARIENCIA_POR_DEFECTO: Apariencia = {
  tema: 'CLARO',
  colorPrimario: '#007AFF',
  colorSecundario: '#1D1D1F',
  colorAcento: '#34C759',
  tamanoFuente: 'NORMAL',
  tipografia: 'INTER',
  bordes: 'SUAVES',
  tieneLogo: false,
  logoVersion: 0,
  encabezadoFactura: null,
  pieFactura: null,
  fechaModificacion: null,
  modificadoPor: null,
};

export interface Paleta {
  nombre: string;
  primario: string;
  secundario: string;
  acento: string;
}

export const PALETAS_PREDEFINIDAS: readonly Paleta[] = [
  { nombre: 'NexoERP', primario: '#007AFF', secundario: '#1D1D1F', acento: '#34C759' },
  { nombre: 'Esmeralda', primario: '#059669', secundario: '#064E3B', acento: '#F59E0B' },
  { nombre: 'Carmesí', primario: '#E11D48', secundario: '#1F2937', acento: '#F97316' },
  { nombre: 'Índigo', primario: '#4F46E5', secundario: '#1E1B4B', acento: '#06B6D4' },
  { nombre: 'Ámbar', primario: '#D97706', secundario: '#292524', acento: '#2563EB' },
  { nombre: 'Grafito', primario: '#475569', secundario: '#0F172A', acento: '#0EA5E9' },
];

export const TAMANOS_FUENTE: readonly { valor: TamanoFuente; etiqueta: string; escala: number }[] = [
  { valor: 'PEQUENO', etiqueta: 'Pequeño', escala: 0.92 },
  { valor: 'NORMAL', etiqueta: 'Normal', escala: 1 },
  { valor: 'GRANDE', etiqueta: 'Grande', escala: 1.1 },
];

export const TIPOGRAFIAS: readonly { valor: Tipografia; etiqueta: string; familia: string; google: string | null }[] = [
  { valor: 'INTER', etiqueta: 'Inter', familia: "'Inter'", google: null },
  { valor: 'ROBOTO', etiqueta: 'Roboto', familia: "'Roboto'", google: 'Roboto:wght@300;400;500;700' },
  { valor: 'POPPINS', etiqueta: 'Poppins', familia: "'Poppins'", google: 'Poppins:wght@300;400;500;600;700' },
  { valor: 'OPEN_SANS', etiqueta: 'Open Sans', familia: "'Open Sans'", google: 'Open+Sans:wght@300;400;500;600;700' },
];

export const ESTILOS_BORDES: readonly { valor: EstiloBordes; etiqueta: string; chico: number; medio: number; grande: number }[] = [
  { valor: 'RECTOS', etiqueta: 'Rectos', chico: 2, medio: 4, grande: 6 },
  { valor: 'SUAVES', etiqueta: 'Suaves', chico: 8, medio: 10, grande: 16 },
  { valor: 'REDONDEADOS', etiqueta: 'Redondeados', chico: 12, medio: 18, grande: 24 },
];

/** Error que devuelve el `GlobalExceptionHandler` del backend. */
export interface ErrorRespuesta {
  status: number;
  error: string;
  message: string;
}

export const TIPOS_LOGO = ['image/png', 'image/jpeg', 'image/webp', 'image/svg+xml'] as const;
export const TAMANO_MAXIMO_LOGO = 512 * 1024;
