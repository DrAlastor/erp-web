import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../../../../environments/environment';
import { AparienciaService } from '../../../seguridad-y-auditoria/cu06-personalizar-apariencia/services/apariencia.service';
import { APARIENCIA_POR_DEFECTO } from '../../../seguridad-y-auditoria/cu06-personalizar-apariencia/models/apariencia.model';
import { ReportesService, nombreDeArchivo } from './reportes.service';

describe('CU37 ReportesService', () => {
  const url = `${environment.apiUrl}/reportes/predefinidos`;
  let servicio: ReportesService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AparienciaService, useValue: { apariencia: signal(APARIENCIA_POR_DEFECTO), logoUrl: signal(null) } },
      ],
    });
    servicio = TestBed.inject(ReportesService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pide el catálogo y las opciones de filtros', () => {
    servicio.catalogo().subscribe();
    servicio.opciones().subscribe();
    http.expectOne(url).flush([]);
    http.expectOne(`${url}/opciones`).flush({ sucursales: [], categorias: [] });
  });

  it('genera el reporte enviando solo los filtros con valor', () => {
    servicio.generar('VENTAS_POR_PERIODO', { desde: '2026-01-01', hasta: '2026-01-31', sucursalId: null, limite: undefined }).subscribe();

    const peticion = http.expectOne((r) => r.url === `${url}/VENTAS_POR_PERIODO`);
    expect(peticion.request.params.get('desde')).toBe('2026-01-01');
    expect(peticion.request.params.get('hasta')).toBe('2026-01-31');
    expect(peticion.request.params.has('sucursalId')).toBe(false);
    expect(peticion.request.params.has('limite')).toBe(false);
    peticion.flush({});
  });

  it('descarga el CSV como blob con el token del interceptor', () => {
    const crear = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:x');
    vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => {});
    const clic = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {});

    servicio.descargarCsv('EXISTENCIAS_POR_SUCURSAL', { sucursalId: 2 }).subscribe();
    const peticion = http.expectOne((r) => r.url === `${url}/EXISTENCIAS_POR_SUCURSAL/csv`);
    expect(peticion.request.responseType).toBe('blob');
    expect(peticion.request.params.get('sucursalId')).toBe('2');
    peticion.flush(new Blob(['a;b']));

    expect(crear).toHaveBeenCalled();
    expect(clic).toHaveBeenCalled();
    vi.restoreAllMocks();
  });

  it('lee el nombre del archivo de Content-Disposition, prefiriendo filename*', () => {
    expect(nombreDeArchivo(`attachment; filename="=?UTF-8?Q?x?="; filename*=UTF-8''ventas-2026.csv`)).toBe('ventas-2026.csv');
    expect(nombreDeArchivo('attachment; filename="kardex.csv"')).toBe('kardex.csv');
    expect(nombreDeArchivo(null)).toBeNull();
  });
});
