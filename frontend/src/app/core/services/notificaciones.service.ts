import { Injectable, signal, computed } from '@angular/core';

export interface NotificacionERP {
  id: string;
  titulo: string;
  mensaje: string;
  categoria: 'ventas' | 'inventario' | 'seguridad' | 'sistema';
  tipo: 'info' | 'alerta' | 'exito' | 'urgente';
  fecha: Date;
  leida: boolean;
  moduloId?: string;
  funcionId?: string;
}

const STORAGE_LEIDAS_KEY = 'erp_notificaciones_leidas_ids';
const STORAGE_ELIMINADAS_KEY = 'erp_notificaciones_eliminadas_ids';

function obtenerLeidasStorage(): Set<string> {
  try {
    const raw = localStorage.getItem(STORAGE_LEIDAS_KEY);
    return raw ? new Set<string>(JSON.parse(raw)) : new Set<string>();
  } catch {
    return new Set<string>();
  }
}

function guardarLeidasStorage(ids: Set<string>): void {
  try {
    localStorage.setItem(STORAGE_LEIDAS_KEY, JSON.stringify(Array.from(ids)));
  } catch { /* noop */ }
}

function obtenerEliminadasStorage(): Set<string> {
  try {
    const raw = localStorage.getItem(STORAGE_ELIMINADAS_KEY);
    return raw ? new Set<string>(JSON.parse(raw)) : new Set<string>();
  } catch {
    return new Set<string>();
  }
}

function guardarEliminadasStorage(ids: Set<string>): void {
  try {
    localStorage.setItem(STORAGE_ELIMINADAS_KEY, JSON.stringify(Array.from(ids)));
  } catch { /* noop */ }
}

@Injectable({
  providedIn: 'root'
})
export class NotificacionesService {
  private readonly leidasSet = obtenerLeidasStorage();
  private readonly eliminadasSet = obtenerEliminadasStorage();

  private readonly _notificaciones = signal<NotificacionERP[]>(this.inicializarNotificaciones());

  readonly notificaciones = this._notificaciones.asReadonly();

  readonly conteoNoLeidas = computed(() =>
    this._notificaciones().filter(n => !n.leida).length
  );

  private inicializarNotificaciones(): NotificacionERP[] {
    const iniciales: NotificacionERP[] = [
      {
        id: 'n-1',
        titulo: 'Stock Crítico en Almacén Central',
        mensaje: 'El artículo "Laptop ASUS Vivobook 15" ha alcanzado el nivel de reserva mínimo (2 unidades disponibles).',
        categoria: 'inventario',
        tipo: 'urgente',
        fecha: new Date(Date.now() - 1000 * 60 * 15),
        leida: false,
        moduloId: 'catalogo-e-inventario',
        funcionId: 'cu10-catalogo-de-articulos'
      },
      {
        id: 'n-2',
        titulo: 'Nueva Cotización Registrada',
        mensaje: 'Se ha emitido la cotización comercial #COT-1004 para el cliente Importadora Boliviana S.R.L.',
        categoria: 'ventas',
        tipo: 'info',
        fecha: new Date(Date.now() - 1000 * 60 * 45),
        leida: false,
        moduloId: 'ventas-y-facturacion',
        funcionId: 'cu15-gestionar-cotizaciones'
      },
      {
        id: 'n-3',
        titulo: 'Lista de Precios Actualizada',
        mensaje: 'La lista "Tarifa Mayorista 2026" fue guardada con protección de margen de rentabilidad aprobada.',
        categoria: 'ventas',
        tipo: 'exito',
        fecha: new Date(Date.now() - 1000 * 60 * 120),
        leida: false,
        moduloId: 'ventas-y-facturacion',
        funcionId: 'cu14-precios-y-listas-precio'
      },
      {
        id: 'n-4',
        titulo: 'Despacho Pendiente de Entrega',
        mensaje: 'Guía de despacho #DESP-0082 lista para revisión física y salida de almacén.',
        categoria: 'inventario',
        tipo: 'alerta',
        fecha: new Date(Date.now() - 1000 * 60 * 240),
        leida: true,
        moduloId: 'ventas-y-facturacion',
        funcionId: 'cu18-gestionar-despacho-de-venta'
      },
      {
        id: 'n-5',
        titulo: 'Inicio de Sesión Seguro',
        mensaje: 'Se verificó autenticación exitosa mediante credenciales con token JWT activo.',
        categoria: 'seguridad',
        tipo: 'info',
        fecha: new Date(Date.now() - 1000 * 60 * 360),
        leida: true,
        moduloId: 'seguridad-y-auditoria',
        funcionId: 'cu08-consultar-bitacora'
      }
    ];

    return iniciales
      .filter(item => !this.eliminadasSet.has(item.id))
      .map(item => ({
        ...item,
        leida: item.leida || this.leidasSet.has(item.id)
      }));
  }

  marcarComoLeida(id: string): void {
    this.leidasSet.add(id);
    guardarLeidasStorage(this.leidasSet);
    this._notificaciones.update(items =>
      items.map(item => item.id === id ? { ...item, leida: true } : item)
    );
  }

  marcarTodasComoLeidas(): void {
    const items = this._notificaciones();
    items.forEach(item => this.leidasSet.add(item.id));
    guardarLeidasStorage(this.leidasSet);
    this._notificaciones.update(curr =>
      curr.map(item => ({ ...item, leida: true }))
    );
  }

  eliminarNotificacion(id: string): void {
    this.eliminadasSet.add(id);
    guardarEliminadasStorage(this.eliminadasSet);
    this._notificaciones.update(items =>
      items.filter(item => item.id !== id)
    );
  }

  agregarNotificacion(notif: Omit<NotificacionERP, 'id' | 'fecha' | 'leida'>): void {
    const id = 'n-' + Date.now();
    const nueva: NotificacionERP = {
      ...notif,
      id,
      fecha: new Date(),
      leida: this.leidasSet.has(id)
    };
    this._notificaciones.update(items => [nueva, ...items]);
  }
}
