/**
 * HU-36 / CU39 — contrato de `GET /api/dashboard/ejecutivo?sucursalId=` (copiado de la spec).
 * El backend calcula cada bloque de forma independiente: si una consulta falla, ese bloque llega
 * con ceros o en null; por eso los bloques se tipan como anulables y la UI los trata uno a uno.
 */

export interface PuntoSerieDiaria {
  fecha: string;
  total: number;
}

export interface TopProductoVenta {
  productoId: number;
  nombre: string;
  cantidad: number;
  total: number;
}

export interface VentasBloque {
  hoy: number;
  mesActual: number;
  mesAnterior: number;
  variacionPct: number | null;
  cantidadMes: number;
  ticketPromedioMes: number;
  serieDiaria: PuntoSerieDiaria[];
  topProductos: TopProductoVenta[];
}

export interface PedidosPorEstado {
  estado: string;
  cantidad: number;
}

export interface ComercialBloque {
  pedidosPorEstado: PedidosPorEstado[];
  cotizacionesVigentes: number;
  clientesActivos: number;
}

export interface CobrarBloque {
  saldoTotal: number;
  vencido: number;
  cantidadVencidas: number;
}

export interface PagarBloque {
  saldoTotal: number;
  vencido: number;
  porVencer7Dias: number;
  cantidadVencidas: number;
}

export interface AlertaStock {
  productoId: number;
  codigo: string;
  nombre: string;
  stock: number;
  stockMinimo: number;
}

export interface InventarioBloque {
  productosActivos: number;
  bajoStockMinimo: number;
  sinStock: number;
  valorInventario: number;
  alertas: AlertaStock[];
}

export interface CajaBloque {
  turnosAbiertos: number;
  ingresosHoy: number;
  egresosHoy: number;
}

export type TipoAlertaDashboard =
  | 'CXP_VENCIDA' | 'CXC_VENCIDA' | 'STOCK_BAJO' | 'SIN_STOCK' | 'PEDIDOS_PENDIENTES' | 'VENTAS_A_LA_BAJA';

export type SeveridadAlerta = 'ALTA' | 'MEDIA' | 'INFO';

export interface AlertaDashboard {
  tipo: TipoAlertaDashboard;
  severidad: SeveridadAlerta;
  mensaje: string;
  /** Id de la función del menú lateral (`core/models/erp-navigation.ts`) a la que lleva la alerta. */
  funcionId: string;
}

export interface DashboardEjecutivoResponse {
  generadoEn: string;
  sucursalId: number | null;
  ventas: VentasBloque | null;
  comercial: ComercialBloque | null;
  cobrar: CobrarBloque | null;
  pagar: PagarBloque | null;
  inventario: InventarioBloque | null;
  caja: CajaBloque | null;
  alertas: AlertaDashboard[];
}

/** Opción del selector de sucursal (`/api/dashboard/ejecutivo/opciones/sucursales`, permiso REPORTES). */
export interface SucursalOpcion {
  id: number;
  nombre: string;
}
