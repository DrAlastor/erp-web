import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ReportesPredefinidosComponent } from './reportes-predefinidos.component';
import { ReportesService } from '../services/reportes.service';
import { DefinicionReporte, ResultadoReporte } from '../models/reportes.model';

const VENTAS: DefinicionReporte = {
  codigo: 'VENTAS_POR_PERIODO', titulo: 'Ventas por período', descripcion: 'd', categoria: 'VENTAS',
  categoriaEtiqueta: 'Ventas', filtros: ['DESDE', 'HASTA', 'SUCURSAL'],
  columnas: [
    { clave: 'dia', etiqueta: 'Día', tipo: 'FECHA', totalizar: false },
    { clave: 'total', etiqueta: 'Total', tipo: 'MONEDA', totalizar: true },
  ],
};
const EXISTENCIAS: DefinicionReporte = {
  codigo: 'EXISTENCIAS_POR_SUCURSAL', titulo: 'Existencias por sucursal', descripcion: 'd', categoria: 'INVENTARIO',
  categoriaEtiqueta: 'Inventario', filtros: ['SUCURSAL', 'CATEGORIA'],
  columnas: [{ clave: 'producto', etiqueta: 'Producto', tipo: 'TEXTO', totalizar: false }],
};
const RESULTADO: ResultadoReporte = {
  codigo: 'VENTAS_POR_PERIODO', titulo: 'Ventas por período', categoria: 'Ventas', generadoEn: '2026-01-31T10:00:00',
  generadoPor: 'admin', filtros: { Desde: '01/01/2026' }, columnas: VENTAS.columnas,
  filas: [{ dia: '2026-01-17', total: 33850 }], totales: { total: 33850 },
};

describe('CU37 Reportes predefinidos', () => {
  let servicio: {
    catalogo: ReturnType<typeof vi.fn>; opciones: ReturnType<typeof vi.fn>; generar: ReturnType<typeof vi.fn>;
    descargarCsv: ReturnType<typeof vi.fn>; imprimir: ReturnType<typeof vi.fn>;
  };

  function crear(catalogo: DefinicionReporte[]) {
    servicio.catalogo.mockReturnValue(of(catalogo));
    const fixture = TestBed.createComponent(ReportesPredefinidosComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    servicio = {
      catalogo: vi.fn(),
      opciones: vi.fn(() => of({ sucursales: [{ id: 1, nombre: 'Central' }], categorias: [] })),
      generar: vi.fn(() => of(RESULTADO)),
      descargarCsv: vi.fn(() => of(undefined)),
      imprimir: vi.fn(),
    };
    await TestBed.configureTestingModule({
      imports: [ReportesPredefinidosComponent],
      providers: [{ provide: ReportesService, useValue: servicio }],
    }).compileComponents();
  });

  it('agrupa por categoría solo los reportes que devuelve el backend', () => {
    const el = crear([VENTAS, EXISTENCIAS]).nativeElement as HTMLElement;

    expect([...el.querySelectorAll('.grupo')].map((g) => g.textContent?.trim())).toEqual(['Ventas', 'Inventario']);
    expect(el.querySelectorAll('.reporte').length).toBe(2);
  });

  it('sin reportes permitidos muestra un aviso', () => {
    const el = crear([]).nativeElement as HTMLElement;

    expect(el.textContent).toContain('no tiene acceso');
  });

  it('muestra solo los filtros del reporte elegido', () => {
    const fixture = crear([VENTAS, EXISTENCIAS]);
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('input[type="date"]').length).toBe(2);

    fixture.componentInstance.elegir(EXISTENCIAS);
    fixture.detectChanges();

    expect(el.querySelectorAll('input[type="date"]').length).toBe(0);
    expect(el.querySelectorAll('select').length).toBe(2);
  });

  it('genera con los filtros del reporte y muestra filas y totales', () => {
    const fixture = crear([VENTAS]);
    const c = fixture.componentInstance;
    c.formulario.patchValue({ desde: '2026-01-01', hasta: '2026-01-31', sucursalId: 1, categoriaId: 9, limite: 5 });

    c.generar();
    fixture.detectChanges();

    expect(servicio.generar).toHaveBeenCalledWith('VENTAS_POR_PERIODO', {
      desde: '2026-01-01', hasta: '2026-01-31', sucursalId: 1, categoriaId: null, limite: null,
    });
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('tbody')?.textContent).toContain('17/01/2026');
    expect(el.querySelector('tfoot')?.textContent).toContain('Totales');
  });

  it('no envía un rango de fechas invertido', () => {
    const c = crear([VENTAS]).componentInstance;
    c.formulario.patchValue({ desde: '2026-02-01', hasta: '2026-01-01' });

    c.generar();

    expect(servicio.generar).not.toHaveBeenCalled();
    expect(c.error()).toContain('posterior');
  });

  it('un 403 del backend muestra un mensaje de permiso', () => {
    servicio.generar.mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    const c = crear([VENTAS]).componentInstance;

    c.generar();

    expect(c.error()).toContain('permiso');
  });

  it('descarga el CSV e imprime el reporte generado', () => {
    const c = crear([VENTAS]).componentInstance;
    c.generar();

    c.descargarCsv();
    c.imprimir();

    expect(servicio.descargarCsv).toHaveBeenCalledWith('VENTAS_POR_PERIODO', expect.any(Object));
    expect(servicio.imprimir).toHaveBeenCalledWith(RESULTADO);
  });
});
