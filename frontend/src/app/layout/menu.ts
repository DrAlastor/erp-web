/**
 * Mapa de módulos del ERP y el permiso que cada uno exige.
 *
 * El menú se arma filtrando esta lista contra los permisos efectivos del usuario: es la
 * tarea 7 de la HU-03. Los módulos que todavía no están construidos se muestran
 * deshabilitados con el sprint en el que llegan, para que el mapa del sistema se lea
 * completo sin ofrecer enlaces que no llevan a ninguna parte.
 */
export interface ItemDeMenu {
  /** Texto que ve el usuario. */
  etiqueta: string;
  /** Ruta dentro del área logueada, o null si el módulo todavía no existe. */
  ruta: string | null;
  /** Permiso que hay que tener para ver el ítem. null = visible para cualquiera. */
  permiso: string | null;
  /** Sprint en el que se construye, para los módulos que faltan. */
  sprint?: number;
}

export const ITEMS_DE_MENU: readonly ItemDeMenu[] = [
  { etiqueta: 'Inicio', ruta: '', permiso: null },
  { etiqueta: 'Roles y permisos', ruta: 'seguridad/roles', permiso: 'SEGURIDAD_CONSULTAR' },
  { etiqueta: 'Ventas', ruta: null, permiso: 'COMERCIAL_CONSULTAR', sprint: 2 },
  { etiqueta: 'Inventario', ruta: null, permiso: 'INVENTARIO_CONSULTAR', sprint: 1 },
  { etiqueta: 'Facturación', ruta: null, permiso: 'FACTURACION_CONSULTAR', sprint: 2 },
  { etiqueta: 'Contabilidad', ruta: null, permiso: 'CONTABILIDAD_CONSULTAR', sprint: 3 },
  { etiqueta: 'Reportes', ruta: null, permiso: 'REPORTES_CONSULTAR', sprint: 3 },
];
