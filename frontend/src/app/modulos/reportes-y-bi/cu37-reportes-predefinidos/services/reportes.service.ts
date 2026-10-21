import { Injectable, PLATFORM_ID, inject } from '@angular/core';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { AparienciaService } from '../../../seguridad-y-auditoria/cu06-personalizar-apariencia/services/apariencia.service';
import {
  DefinicionReporte,
  OpcionesReporte,
  ParametrosReporte,
  ResultadoReporte,
} from '../models/reportes.model';
import { documentoImprimible, fechaIso } from '../utils/formato';

/**
 * CU37: reportes predefinidos. El backend decide qué reportes ve cada usuario según los
 * permisos de su rol; el catálogo que devuelve ya viene filtrado.
 */
@Injectable({ providedIn: 'root' })
export class ReportesService {
  private readonly http = inject(HttpClient);
  private readonly apariencia = inject(AparienciaService);
  private readonly documento = inject(DOCUMENT);
  private readonly enNavegador = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly url = `${environment.apiUrl}/reportes/predefinidos`;

  catalogo(): Observable<DefinicionReporte[]> {
    return this.http.get<DefinicionReporte[]>(this.url);
  }

  opciones(): Observable<OpcionesReporte> {
    return this.http.get<OpcionesReporte>(`${this.url}/opciones`);
  }

  generar(codigo: string, parametros: ParametrosReporte): Observable<ResultadoReporte> {
    return this.http.get<ResultadoReporte>(`${this.url}/${codigo}`, { params: aParams(parametros) });
  }

  /**
   * Descarga el CSV. Se pide con `HttpClient` (y no con un enlace directo) para que el
   * interceptor agregue el token; luego se guarda con un enlace temporal.
   */
  descargarCsv(codigo: string, parametros: ParametrosReporte): Observable<void> {
    return this.http
      .get(`${this.url}/${codigo}/csv`, { params: aParams(parametros), responseType: 'blob', observe: 'response' })
      .pipe(
        map((respuesta) => {
          // En desarrollo (otro origen) CORS no expone Content-Disposition: se arma el mismo nombre.
          const nombre = nombreDeArchivo(respuesta.headers.get('Content-Disposition'))
            ?? `${codigo.toLowerCase().replace(/_/g, '-')}-${fechaIso(new Date())}.csv`;
          if (respuesta.body) this.guardar(respuesta.body, nombre);
        }),
      );
  }

  /**
   * Abre el diálogo de impresión con el reporte, desde donde también se guarda como PDF. Se
   * imprime un documento propio en un iframe oculto para no arrastrar el menú del panel.
   */
  imprimir(resultado: ResultadoReporte): void {
    if (!this.enNavegador) return;
    const a = this.apariencia.apariencia();
    const html = documentoImprimible(resultado, {
      logo: this.apariencia.logoUrl() ?? new URL('erp-logo.png', this.documento.baseURI).href,
      colorPrimario: a.colorPrimario,
      empresa: a.encabezadoFactura,
    });

    const marco = this.documento.createElement('iframe');
    marco.setAttribute('aria-hidden', 'true');
    marco.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0;visibility:hidden';
    this.documento.body.appendChild(marco);
    const ventana = marco.contentWindow;
    if (!ventana) {
      marco.remove();
      return;
    }
    ventana.document.open();
    ventana.document.write(html);
    ventana.document.close();
    const quitar = () => setTimeout(() => marco.remove(), 1000);
    ventana.onafterprint = quitar;
    // Espera a que cargue el logo para que salga en la impresión.
    setTimeout(() => {
      ventana.focus();
      ventana.print();
      quitar();
    }, 300);
  }

  private guardar(contenido: Blob, nombre: string): void {
    if (!this.enNavegador) return;
    const enlace = this.documento.createElement('a');
    const url = URL.createObjectURL(contenido);
    enlace.href = url;
    enlace.download = nombre;
    this.documento.body.appendChild(enlace);
    enlace.click();
    enlace.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  }
}

function aParams(p: ParametrosReporte): HttpParams {
  let params = new HttpParams();
  for (const [clave, valor] of Object.entries(p)) {
    if (valor !== null && valor !== undefined && valor !== '') params = params.set(clave, String(valor));
  }
  return params;
}

/** Nombre del archivo desde `Content-Disposition`, prefiriendo `filename*` (UTF-8). */
export function nombreDeArchivo(cabecera: string | null): string | null {
  if (!cabecera) return null;
  const extendido = /filename\*=UTF-8''([^;]+)/i.exec(cabecera);
  if (extendido) return decodeURIComponent(extendido[1]);
  const simple = /filename="?([^";]+)"?/i.exec(cabecera);
  return simple ? simple[1] : null;
}
