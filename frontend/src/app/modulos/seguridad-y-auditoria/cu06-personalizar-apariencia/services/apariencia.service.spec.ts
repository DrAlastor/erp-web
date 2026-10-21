import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../../../environments/environment';
import { ThemeService } from '../../../../core/services/theme.service';
import { AparienciaService } from './apariencia.service';
import { APARIENCIA_POR_DEFECTO, Apariencia } from '../models/apariencia.model';

describe('CU06 AparienciaService', () => {
  const url = `${environment.apiUrl}/seguridad/apariencia`;
  let servicio: AparienciaService;
  let http: HttpTestingController;
  const tema = { aplicarSinGuardar: vi.fn(), restaurarPreferencia: vi.fn() };
  const raiz = () => document.documentElement.style;

  const personalizada: Apariencia = {
    ...APARIENCIA_POR_DEFECTO,
    tema: 'OSCURO',
    colorPrimario: '#E11D48',
    tipografia: 'POPPINS',
    bordes: 'RECTOS',
    tamanoFuente: 'GRANDE',
  };

  /** Node 25 expone un localStorage sin métodos si no se le da archivo; se usa uno en memoria. */
  function almacenamientoEnMemoria(): Storage {
    const datos = new Map<string, string>();
    return {
      get length() { return datos.size; },
      clear: () => datos.clear(),
      getItem: (k) => datos.get(k) ?? null,
      key: (i) => [...datos.keys()][i] ?? null,
      removeItem: (k) => { datos.delete(k); },
      setItem: (k, v) => { datos.set(k, String(v)); },
    };
  }

  beforeEach(() => {
    vi.stubGlobal('localStorage', almacenamientoEnMemoria());
    tema.aplicarSinGuardar.mockClear();
    tema.restaurarPreferencia.mockClear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: ThemeService, useValue: tema }],
    });
    servicio = TestBed.inject(AparienciaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    servicio.limpiar();
    http.verify();
    vi.unstubAllGlobals();
  });

  it('carga la apariencia de la empresa y la aplica al documento', () => {
    servicio.cargar().subscribe();
    const peticion = http.expectOne(url);
    expect(peticion.request.method).toBe('GET');
    peticion.flush(personalizada);

    expect(servicio.apariencia().colorPrimario).toBe('#E11D48');
    expect(tema.aplicarSinGuardar).toHaveBeenCalledWith('dark');
    expect(raiz().getPropertyValue('--primary')).not.toBe('');
    expect(raiz().getPropertyValue('--font-sans')).toContain('Poppins');
    expect(document.getElementById('erp-apariencia-estilos')?.textContent).toContain('zoom: 1.1');
    expect(document.getElementById('erp-apariencia-fuente')).not.toBeNull();
  });

  it('con la marca NexoERP no sobrescribe la hoja de estilos original', () => {
    servicio.cargar().subscribe();
    http.expectOne(url).flush(APARIENCIA_POR_DEFECTO);

    expect(raiz().getPropertyValue('--primary')).toBe('');
    expect(raiz().getPropertyValue('--font-sans')).toBe('');
    expect(document.getElementById('erp-apariencia-estilos')).toBeNull();
    expect(tema.aplicarSinGuardar).toHaveBeenCalledWith('light');
  });

  it('descarga el logo con la versión cuando la empresa tiene uno', async () => {
    servicio.cargar().subscribe();
    http.expectOne(url).flush({ ...personalizada, tieneLogo: true, logoVersion: 3 });
    const logo = http.expectOne((r) => r.url === `${url}/logo`);
    expect(logo.request.params.get('v')).toBe('3');
    logo.flush(new Blob(['png'], { type: 'image/png' }));

    await vi.waitFor(() => expect(servicio.logoUrl()).toMatch(/^data:image\/png/));
  });

  it('guarda con PUT y sube el logo como multipart', () => {
    servicio.guardar({
      tema: 'CLARO', colorPrimario: '#059669', colorSecundario: '#064E3B', colorAcento: '#F59E0B',
      tamanoFuente: 'NORMAL', tipografia: 'INTER', bordes: 'SUAVES', encabezadoFactura: null, pieFactura: null,
    }).subscribe();
    const put = http.expectOne(url);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body.colorPrimario).toBe('#059669');
    put.flush({ ...APARIENCIA_POR_DEFECTO, colorPrimario: '#059669' });

    servicio.subirLogo(new File(['x'], 'logo.png', { type: 'image/png' })).subscribe();
    const post = http.expectOne(`${url}/logo`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body instanceof FormData).toBe(true);
    expect((post.request.body as FormData).get('archivo')).toBeTruthy();
    post.flush({ ...APARIENCIA_POR_DEFECTO, tieneLogo: true, logoVersion: 1 });
  });

  it('al cerrar sesión quita la apariencia y la copia del navegador', () => {
    servicio.cargar().subscribe();
    http.expectOne(url).flush(personalizada);
    expect(localStorage.getItem('erp_apariencia')).not.toBeNull();

    servicio.limpiar();

    expect(raiz().getPropertyValue('--primary')).toBe('');
    expect(document.getElementById('erp-apariencia-estilos')).toBeNull();
    expect(localStorage.getItem('erp_apariencia')).toBeNull();
    expect(servicio.apariencia()).toEqual(APARIENCIA_POR_DEFECTO);
    expect(tema.restaurarPreferencia).toHaveBeenCalled();
  });

  it('la vista previa no modifica lo guardado y restaurar la descarta', () => {
    servicio.previsualizar({ ...APARIENCIA_POR_DEFECTO, colorPrimario: '#4F46E5' });
    expect(raiz().getPropertyValue('--primary')).toBe('#4F46E5');
    expect(servicio.apariencia().colorPrimario).toBe('#007AFF');

    servicio.restaurar();
    expect(raiz().getPropertyValue('--primary')).toBe('');
  });
});
