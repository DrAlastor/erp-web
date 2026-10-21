import { Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, map, of, switchMap, tap } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { ThemeService } from '../../../../core/services/theme.service';
import {
  APARIENCIA_POR_DEFECTO,
  ActualizarApariencia,
  Apariencia,
  ESTILOS_BORDES,
  TAMANOS_FUENTE,
  TIPOGRAFIAS,
} from '../models/apariencia.model';
import { luminancia, tonosDe } from '../utils/colores';

const CLAVE_CACHE = 'erp_apariencia';
const ID_ESTILOS = 'erp-apariencia-estilos';
const ID_FUENTE = 'erp-apariencia-fuente';
const FUENTE_BASE = "-apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', sans-serif";
const VARIABLES = [
  '--primary', '--primary-hover', '--primary-active', '--primary-light', '--primary-glow',
  '--secondary', '--secondary-hover', '--secondary-active', '--accent', '--font-sans',
] as const;

interface Cache {
  apariencia: Apariencia;
  logo: string | null;
}

/**
 * Apariencia de la empresa (CU06): la carga del backend y la aplica a todo el panel.
 *
 * Solo cambia lo que la empresa personalizó; si un valor es el de la marca NexoERP se deja
 * la hoja de estilos original tal cual. Se guarda una copia en el navegador para no mostrar
 * la marca por defecto un instante al recargar, y se borra al cerrar sesión para que otra
 * empresa que entre en el mismo navegador no herede esta apariencia.
 */
@Injectable({ providedIn: 'root' })
export class AparienciaService {
  private readonly http = inject(HttpClient);
  private readonly tema = inject(ThemeService);
  private readonly documento = inject(DOCUMENT);
  private readonly enNavegador = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly url = `${environment.apiUrl}/seguridad/apariencia`;

  private readonly _apariencia = signal<Apariencia>(APARIENCIA_POR_DEFECTO);
  private readonly _logoUrl = signal<string | null>(null);
  private readonly _cargada = signal(false);

  /** La apariencia guardada (la vista previa no la modifica). */
  readonly apariencia = this._apariencia.asReadonly();
  /** El logo como data URL, o `null` si la empresa usa el de NexoERP. */
  readonly logoUrl = this._logoUrl.asReadonly();
  readonly cargada = this._cargada.asReadonly();

  constructor() {
    const cache = this.leerCache();
    if (cache) {
      this._apariencia.set(cache.apariencia);
      this._logoUrl.set(cache.logo);
    }
  }

  /** Pide la apariencia al backend y la aplica. Se llama al entrar al panel. */
  cargar(): Observable<Apariencia> {
    // Mientras responde el backend se muestra lo último conocido, sin parpadeo.
    if (this._cargada() || this.leerCache()) this.aplicar(this._apariencia());

    return this.http.get<Apariencia>(this.url).pipe(
      switchMap((apariencia) => this.resolverLogo(apariencia).pipe(map((logo) => ({ apariencia, logo })))),
      tap(({ apariencia, logo }) => this.establecer(apariencia, logo)),
      map(({ apariencia }) => apariencia),
    );
  }

  guardar(datos: ActualizarApariencia): Observable<Apariencia> {
    return this.http.put<Apariencia>(this.url, datos).pipe(
      tap((apariencia) => this.establecer(apariencia, this._logoUrl())),
    );
  }

  subirLogo(archivo: File): Observable<Apariencia> {
    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo);
    return this.http.post<Apariencia>(`${this.url}/logo`, cuerpo).pipe(
      switchMap((apariencia) => leerComoDataUrl(archivo).pipe(map((logo) => ({ apariencia, logo })))),
      tap(({ apariencia, logo }) => this.establecer(apariencia, logo)),
      map(({ apariencia }) => apariencia),
    );
  }

  eliminarLogo(): Observable<Apariencia> {
    return this.http.delete<Apariencia>(`${this.url}/logo`).pipe(
      tap((apariencia) => this.establecer(apariencia, null)),
    );
  }

  /** Vuelve a la marca NexoERP (colores, tema, preferencias y logo). Conserva la factura. */
  restablecer(): Observable<Apariencia> {
    return this.http.post<Apariencia>(`${this.url}/restablecer`, {}).pipe(
      tap((apariencia) => this.establecer(apariencia, null)),
    );
  }

  /** Aplica una apariencia sin guardarla, para la vista previa en vivo. */
  previsualizar(apariencia: Apariencia): void {
    this.aplicar(apariencia);
  }

  /** Descarta la vista previa y vuelve a lo guardado. */
  restaurar(): void {
    this.aplicar(this._apariencia());
  }

  /** Quita la apariencia de la empresa y vuelve a la marca NexoERP. Se llama al cerrar sesión. */
  limpiar(): void {
    this._apariencia.set(APARIENCIA_POR_DEFECTO);
    this._logoUrl.set(null);
    this._cargada.set(false);
    if (!this.enNavegador) return;
    try { localStorage.removeItem(CLAVE_CACHE); } catch { /* Almacenamiento no disponible. */ }
    const raiz = this.documento.documentElement;
    for (const variable of VARIABLES) raiz.style.removeProperty(variable);
    this.documento.getElementById(ID_ESTILOS)?.remove();
    this.documento.getElementById(ID_FUENTE)?.remove();
    this.tema.restaurarPreferencia();
  }

  /** Escribe la apariencia en el documento. Solo toca lo que difiere de la marca NexoERP. */
  aplicar(a: Apariencia): void {
    if (!this.enNavegador) return;
    const raiz = this.documento.documentElement;
    const oscuro = a.tema === 'OSCURO';
    const def = APARIENCIA_POR_DEFECTO;
    const fijar = (variable: string, valor: string | null) =>
      valor === null ? raiz.style.removeProperty(variable) : raiz.style.setProperty(variable, valor);

    this.tema.aplicarSinGuardar(oscuro ? 'dark' : 'light');

    const primario = igual(a.colorPrimario, def.colorPrimario) ? null : tonosDe(a.colorPrimario, oscuro);
    fijar('--primary', primario?.base ?? null);
    fijar('--primary-hover', primario?.hover ?? null);
    fijar('--primary-active', primario?.active ?? null);
    fijar('--primary-light', primario?.light ?? null);
    fijar('--primary-glow', primario?.glow ?? null);

    // En modo oscuro el secundario original se invierte a casi blanco; un secundario oscuro
    // no se vería sobre el fondo negro, así que en ese caso se deja el de la hoja de estilos.
    const usarSecundario = !igual(a.colorSecundario, def.colorSecundario)
      && !(oscuro && luminancia(a.colorSecundario) < 0.35);
    const secundario = usarSecundario ? tonosDe(a.colorSecundario, false) : null;
    fijar('--secondary', secundario?.base ?? null);
    fijar('--secondary-hover', secundario?.hover ?? null);
    fijar('--secondary-active', secundario?.active ?? null);

    fijar('--accent', a.colorAcento.toUpperCase());

    const tipografia = TIPOGRAFIAS.find((t) => t.valor === a.tipografia);
    fijar('--font-sans', tipografia && tipografia.valor !== def.tipografia ? `${tipografia.familia}, ${FUENTE_BASE}` : null);
    this.cargarFuente(tipografia?.google ?? null);

    this.escribirEstilos(a);
  }

  private establecer(apariencia: Apariencia, logo: string | null): void {
    this._apariencia.set(apariencia);
    this._logoUrl.set(apariencia.tieneLogo ? logo : null);
    this._cargada.set(true);
    this.aplicar(apariencia);
    this.guardarCache({ apariencia, logo: apariencia.tieneLogo ? logo : null });
  }

  /** Reutiliza el logo en caché si no cambió de versión; si cambió, lo descarga. */
  private resolverLogo(apariencia: Apariencia): Observable<string | null> {
    if (!apariencia.tieneLogo) return of(null);
    const cache = this.leerCache();
    if (cache?.logo && cache.apariencia.logoVersion === apariencia.logoVersion) return of(cache.logo);
    return this.http
      .get(`${this.url}/logo`, { responseType: 'blob', params: { v: apariencia.logoVersion } })
      .pipe(switchMap((blob) => leerComoDataUrl(blob)));
  }

  /** Bordes, tamaño de fuente y acento: reglas que dependen de la empresa, dentro del panel. */
  private escribirEstilos(a: Apariencia): void {
    const reglas: string[] = [];
    const escala = TAMANOS_FUENTE.find((t) => t.valor === a.tamanoFuente)?.escala ?? 1;
    if (escala !== 1) reglas.push(`.workspace main { zoom: ${escala}; }`);

    if (a.bordes !== APARIENCIA_POR_DEFECTO.bordes) {
      const r = ESTILOS_BORDES.find((b) => b.valor === a.bordes);
      if (r) {
        reglas.push(
          `.workspace .btn, .workspace .btn-icon-square, .workspace .apple-search, .workspace select, .workspace textarea,
           .workspace input:not([type="checkbox"]):not([type="radio"]) { border-radius: ${r.medio}px !important; }`,
          `.workspace .apple-card, .workspace .module-card { border-radius: ${r.grande}px !important; }`,
          `.workspace .badge-mono, .workspace .btn-label-mono { border-radius: ${r.chico}px !important; }`,
        );
      }
    }

    if (!igual(a.colorAcento, APARIENCIA_POR_DEFECTO.colorAcento)) {
      reglas.push('.workspace .brand .accent { color: var(--accent); }');
    }

    let estilo = this.documento.getElementById(ID_ESTILOS);
    if (reglas.length === 0) {
      estilo?.remove();
      return;
    }
    if (!estilo) {
      estilo = this.documento.createElement('style');
      estilo.id = ID_ESTILOS;
      this.documento.head.appendChild(estilo);
    }
    estilo.textContent = reglas.join('\n');
  }

  private cargarFuente(familiaGoogle: string | null): void {
    const actual = this.documento.getElementById(ID_FUENTE) as HTMLLinkElement | null;
    if (!familiaGoogle) {
      actual?.remove();
      return;
    }
    const href = `https://fonts.googleapis.com/css2?family=${familiaGoogle}&display=swap`;
    if (actual?.href === href) return;
    const enlace = actual ?? this.documento.createElement('link');
    enlace.id = ID_FUENTE;
    enlace.rel = 'stylesheet';
    enlace.href = href;
    if (!actual) this.documento.head.appendChild(enlace);
  }

  private leerCache(): Cache | null {
    if (!this.enNavegador) return null;
    try {
      const texto = localStorage.getItem(CLAVE_CACHE);
      const cache = texto ? (JSON.parse(texto) as Cache) : null;
      return cache?.apariencia?.colorPrimario ? cache : null;
    } catch {
      return null;
    }
  }

  private guardarCache(cache: Cache): void {
    if (!this.enNavegador) return;
    try {
      localStorage.setItem(CLAVE_CACHE, JSON.stringify(cache));
    } catch {
      // Sin espacio o sin almacenamiento: la apariencia sigue aplicada en esta sesión.
    }
  }
}

function igual(a: string, b: string): boolean {
  return a.toUpperCase() === b.toUpperCase();
}

function leerComoDataUrl(blob: Blob): Observable<string> {
  return new Observable<string>((observador) => {
    const lector = new FileReader();
    lector.onload = () => {
      observador.next(String(lector.result));
      observador.complete();
    };
    lector.onerror = () => observador.error(lector.error);
    lector.readAsDataURL(blob);
    return () => lector.abort();
  });
}
