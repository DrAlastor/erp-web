import { Injectable, inject, signal, computed, DestroyRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

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

interface NotificacionBackendDto {
  id: number;
  usuarioId: number;
  titulo: string;
  mensaje: string;
  tipo: string;
  categoria: string;
  fecha: string;
  leida: boolean;
  moduloId?: string;
  funcionId?: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificacionesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/notificaciones`;

  private readonly _notificaciones = signal<NotificacionERP[]>([]);
  readonly notificaciones = this._notificaciones.asReadonly();

  readonly conteoNoLeidas = computed(() =>
    this._notificaciones().filter(n => !n.leida).length
  );

  private intervalId: any = null;

  constructor() {
    this.cargarNotificaciones();
    // Sondeo periódico cada 30 segundos para recibir alertas en tiempo real
    if (typeof window !== 'undefined') {
      this.intervalId = setInterval(() => {
        this.cargarNotificaciones(true);
      }, 30000);
    }
  }

  cargarNotificaciones(silencioso = false): void {
    this.http.get<NotificacionBackendDto[]>(this.apiUrl).subscribe({
      next: (dtos) => {
        if (Array.isArray(dtos)) {
          const list: NotificacionERP[] = dtos.map(d => ({
            id: String(d.id),
            titulo: d.titulo,
            mensaje: d.mensaje,
            categoria: this.normalizarCategoria(d.categoria),
            tipo: this.normalizarTipo(d.tipo),
            fecha: new Date(d.fecha),
            leida: Boolean(d.leida),
            moduloId: d.moduloId,
            funcionId: d.funcionId
          }));
          this._notificaciones.set(list);
        }
      },
      error: () => {
        // En caso de que no haya sesión o error de red, no interrumpir la experiencia
      }
    });
  }

  marcarComoLeida(id: string): void {
    // Actualización optimista local
    this._notificaciones.update(items =>
      items.map(item => item.id === id ? { ...item, leida: true } : item)
    );

    const numId = Number(id);
    if (!isNaN(numId) && numId > 0) {
      this.http.put(`${this.apiUrl}/${numId}/leer`, {}).subscribe({
        error: () => {}
      });
    }
  }

  marcarTodasComoLeidas(): void {
    // Actualización optimista local
    this._notificaciones.update(curr =>
      curr.map(item => ({ ...item, leida: true }))
    );

    this.http.put(`${this.apiUrl}/marcar-todas`, {}).subscribe({
      error: () => {}
    });
  }

  eliminarNotificacion(id: string): void {
    // Actualización optimista local
    this._notificaciones.update(items =>
      items.filter(item => item.id !== id)
    );

    const numId = Number(id);
    if (!isNaN(numId) && numId > 0) {
      this.http.delete(`${this.apiUrl}/${numId}`).subscribe({
        error: () => {}
      });
    }
  }

  notificarCambioPassword(email?: string): void {
    this.http.post(`${this.apiUrl}/evento-password`, { email }).subscribe({
      next: () => {
        this.cargarNotificaciones();
      },
      error: () => {}
    });
  }

  agregarNotificacion(notif: Omit<NotificacionERP, 'id' | 'fecha' | 'leida'>): void {
    const id = 'local-' + Date.now();
    const nueva: NotificacionERP = {
      ...notif,
      id,
      fecha: new Date(),
      leida: false
    };
    this._notificaciones.update(items => [nueva, ...items]);
  }

  private normalizarCategoria(cat?: string): 'ventas' | 'inventario' | 'seguridad' | 'sistema' {
    const lower = (cat || '').toLowerCase();
    if (lower === 'ventas') return 'ventas';
    if (lower === 'inventario') return 'inventario';
    if (lower === 'seguridad') return 'seguridad';
    return 'sistema';
  }

  private normalizarTipo(t?: string): 'info' | 'alerta' | 'exito' | 'urgente' {
    const lower = (t || '').toLowerCase();
    if (lower === 'alerta') return 'alerta';
    if (lower === 'exito') return 'exito';
    if (lower === 'urgente') return 'urgente';
    return 'info';
  }
}
