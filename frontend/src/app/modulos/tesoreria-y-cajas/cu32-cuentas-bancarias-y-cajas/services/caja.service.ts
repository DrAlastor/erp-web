import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../../environments/environment';
import {
  AbrirTurnoRequest, ArqueoResponse, CajaOpcionesResponse, MovimientoResponse, RegistrarMovimientoRequest, TurnoResponse,
} from '../models/caja.model';

/**
 * HU-34 / CU34 — cliente HTTP de `/api/caja` para apertura y movimientos (FACTURACION consultar / crear).
 * Cierre, historial y detalle viven en `cu35-arqueos-y-conciliaciones/services/arqueo-caja.service.ts`.
 */
@Injectable({ providedIn: 'root' })
export class CajaService {
  private readonly http = inject(HttpClient);
  readonly url = `${environment.apiUrl}/caja`;

  opciones() {
    return this.http.get<CajaOpcionesResponse>(`${this.url}/opciones`);
  }

  /** 200 con el turno ABIERTO del usuario autenticado, o 204 (cuerpo null) si no tiene. */
  turnoActual() {
    return this.http.get<TurnoResponse | null>(`${this.url}/turno-actual`);
  }

  abrirTurno(body: AbrirTurnoRequest) {
    return this.http.post<TurnoResponse>(`${this.url}/turnos`, body);
  }

  movimientos(turnoId: number) {
    return this.http.get<MovimientoResponse[]>(`${this.url}/turnos/${turnoId}/movimientos`);
  }

  registrarMovimiento(turnoId: number, body: RegistrarMovimientoRequest) {
    return this.http.post<MovimientoResponse>(`${this.url}/turnos/${turnoId}/movimientos`, body);
  }

  arqueo(turnoId: number) {
    return this.http.get<ArqueoResponse>(`${this.url}/turnos/${turnoId}/arqueo`);
  }
}
