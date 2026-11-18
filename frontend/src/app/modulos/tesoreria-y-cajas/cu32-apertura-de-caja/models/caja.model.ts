export interface CajaSucursal {
  id: number;
  nombre: string;
}

export interface CajaTurno {
  id: number;
  sucursalId: number;
  sucursalNombre: string;
  cajeroId: number;
  montoApertura: number;
  fechaApertura: string;
  estado: string;
  observaciones: string | null;
}

export interface ResumenCaja {
  montoApertura: number;
  ventasContado: number;
  cobrosEfectivo: number;
  otrosCobros: number;
  ingresos: number;
  egresos: number;
  montoEsperado: number;
  fechaCorte: string;
}

export interface EstadoCaja {
  turno: CajaTurno | null;
  sucursales: CajaSucursal[];
  formasPago: { id: number; codigo: string; nombre: string }[];
  resumen: ResumenCaja | null;
  /** Hay dosificación vigente para emitir factura. */
  facturaDisponible: boolean;
}

export interface CierreCaja {
  turnoId: number;
  estado: string;
  montoApertura: number;
  montoEsperado: number;
  montoCierre: number;
  diferencia: number;
  fechaCierre: string;
}
