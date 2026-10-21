import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../../environments/environment';

export interface BackupItem {
  id: number;
  empresaId: number;
  codigoEmpresa: string;
  razonSocial: string;
  nombreDatabase: string;
  nombreArchivo: string;
  rutaStorage: string;
  tamanoBytes: number;
  tamanoFormateado: string;
  estado: string; // COMPLETADO, EN_PROCESO, FALLIDO
  tipoBackup: string; // BAJO_DEMANDA, AUTOMATICO, LOGICO
  solicitadoPor: string;
  fechaInicio: string;
  fechaFin: string;
  checksumSha256: string;
  retencionHasta: string;
  jobId: string;
  estadoVerificacion: string; // VERIFICADO, PENDIENTE, CORRUPTO
  codigoError?: string;
  fechaCreacion: string;
}

export interface VerificacionIntegridad {
  backupId: number;
  nombreArchivo: string;
  checksumEsperado: string;
  checksumCalculado: string;
  integro: boolean;
  estadoVerificacion: string;
  mensaje: string;
}

export interface RestauracionItem {
  id: number;
  empresaId: number;
  backupId: number;
  tipoRestauracion: string; // COMPLETA, SELECTIVA
  tablasSeleccionadas: string;
  solicitadoPor: string;
  estado: string; // EN_PROCESO, COMPLETADO, FALLIDO
  fechaInicio: string;
  fechaFin: string;
  detalles: string;
  reversionDisponible: boolean;
}

export interface PoliticaBackup {
  empresaId: number;
  frecuencia: string; // DIARIO, SEMANAL, MENSUAL
  horaEjecucion: string; // HH:mm
  diasRetencion: number;
  activo: boolean;
  notificarEmail: string;
  ultimoBackupAutomatico?: string;
  fechaModificacion?: string;
}

@Injectable({ providedIn: 'root' })
export class BackupsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/backups`;

  listarBackups(): Observable<BackupItem[]> {
    return this.http.get<BackupItem[]>(this.baseUrl);
  }

  listarPuntosRestauracion(): Observable<BackupItem[]> {
    return this.http.get<BackupItem[]>(`${this.baseUrl}/puntos-restauracion`);
  }

  crearBackup(tipoBackup: string = 'BAJO_DEMANDA'): Observable<BackupItem> {
    return this.http.post<BackupItem>(this.baseUrl, { tipoBackup });
  }

  verificarIntegridad(id: number): Observable<VerificacionIntegridad> {
    return this.http.get<VerificacionIntegridad>(`${this.baseUrl}/${id}/verificar`);
  }

  descargarArchivo(id: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${id}/descargar`, { responseType: 'blob' });
  }

  restaurarCompleta(id: number, motivo: string): Observable<RestauracionItem> {
    return this.http.post<RestauracionItem>(`${this.baseUrl}/${id}/restaurar-completa`, {
      tipoRestauracion: 'COMPLETA',
      confirmacion: true,
      motivo
    });
  }

  restaurarSelectiva(id: number, tablasSeleccionadas: string[], motivo: string): Observable<RestauracionItem> {
    return this.http.post<RestauracionItem>(`${this.baseUrl}/${id}/restaurar-selectiva`, {
      tipoRestauracion: 'SELECTIVA',
      tablasSeleccionadas,
      confirmacion: true,
      motivo
    });
  }

  listarRestauraciones(): Observable<RestauracionItem[]> {
    return this.http.get<RestauracionItem[]>(`${this.baseUrl}/restauraciones`);
  }

  obtenerPolitica(): Observable<PoliticaBackup> {
    return this.http.get<PoliticaBackup>(`${this.baseUrl}/politica`);
  }

  guardarPolitica(dto: PoliticaBackup): Observable<PoliticaBackup> {
    return this.http.put<PoliticaBackup>(`${this.baseUrl}/politica`, dto);
  }

  obtenerTablasDisponibles(): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/tablas-disponibles`);
  }
}
