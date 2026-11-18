/** HU-29 / CU29 — modelos de `/api/cuentas-por-cobrar`. */

export type EstadoCxc = 'PENDIENTE' | 'PARCIAL' | 'PAGADO' | 'ANULADA';
export type FiltroEstadoCxc = '' | EstadoCxc | 'VENCIDA';

export const FILTROS_ESTADO_CXC: { valor: FiltroEstadoCxc; nombre: string }[] = [
  { valor: '', nombre: 'Todas' },
  { valor: 'PENDIENTE', nombre: 'Pendientes' },
  { valor: 'PARCIAL', nombre: 'Con abonos' },
  { valor: 'VENCIDA', nombre: 'Vencidas' },
  { valor: 'PAGADO', nombre: 'Pagadas' },
  { valor: 'ANULADA', nombre: 'Anuladas' },
];

export interface CuotaCxc {
  id: number;
  ventaId: number;
  documento: string;
  clienteId: number;
  clienteRazonSocial: string;
  clienteNitCi: string;
  numeroCuota: number;
  montoTotal: number;
  montoCobrado: number;
  saldo: number;
  fechaVencimiento: string;
  estado: EstadoCxc;
  vencida: boolean;
  diasVencida: number;
  sucursal: string | null;
}

export interface Cobro {
  id: number;
  numeroRecibo: string;
  formaPagoId: number;
  formaPago: string;
  monto: number;
  fechaCobro: string;
  transaccionExterna: string | null;
  observaciones: string | null;
  turnoCajaId: number | null;
  usuario: string | null;
  fechaCreacion: string | null;
}

export interface CuentaPorCobrar {
  cuota: CuotaCxc;
  fechaVenta: string | null;
  totalVenta: number | null;
  clienteTelefono: string | null;
  clienteEmail: string | null;
  cobrable: boolean;
  cobros: Cobro[];
}

export interface RegistrarCobroRequest {
  formaPagoId: number;
  monto: number;
  fechaCobro: string | null;
  transaccionExterna: string | null;
  observaciones: string | null;
}

export interface RegistrarCobroResponse {
  cobro: Cobro;
  numeroAsiento: string;
  cuenta: CuentaPorCobrar;
}

export interface ResumenCxc {
  saldoTotal: number;
  vencido: number;
  porVencer7Dias: number;
  cobradoMes: number;
  cuotasConSaldo: number;
  cuotasVencidas: number;
  antiguedad: number[];
  topClientes: { clienteId: number; razonSocial: string; saldo: number }[];
}

export interface EstadoCuentaCliente {
  clienteId: number;
  razonSocial: string;
  nitCi: string;
  limiteCredito: number | null;
  totalFacturadoCredito: number;
  totalCobrado: number;
  saldo: number;
  vencido: number;
  cuotas: CuotaCxc[];
}

export interface Opcion {
  id: number;
  nombre: string;
  detalle: string;
}

export interface CuotaPage {
  content: CuotaCxc[];
  totalElements: number;
  totalPages: number;
}

export const TRAMOS_ANTIGUEDAD = ['1 a 30 días', '31 a 60 días', '61 a 90 días', 'Más de 90 días'];
