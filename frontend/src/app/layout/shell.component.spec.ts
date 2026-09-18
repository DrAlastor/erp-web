import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ShellComponent } from './shell.component';
import { PermisosService } from '../core/permisos/permisos.service';
import { environment } from '../../environments/environment';

/**
 * El menú filtrado por permisos es la tarea 7 de la HU-03 y uno de los criterios de
 * aprobación: "las opciones de menú que el usuario no tenga permitido utilizar deben estar
 * restringidas".
 */
describe('ShellComponent', () => {
  let http: HttpTestingController;
  let permisos: PermisosService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    permisos = TestBed.inject(PermisosService);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  function cargar(codigos: string[], roles: string[] = []): void {
    permisos.cargar().subscribe();
    http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`).flush({
      usuarioId: 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3',
      empresaId: 'f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0',
      nombre: 'Usuario de prueba',
      permisos: codigos,
      roles,
    });
  }

  function etiquetas(): string[] {
    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
    return fixture.componentInstance.items().map((item) => item.etiqueta);
  }

  it('el cajero no ve la sección de roles y permisos', () => {
    // Los seis permisos exactos de la fila del Cajero en la matriz del documento.
    cargar(
      [
        'COMERCIAL_CREAR',
        'COMERCIAL_CONSULTAR',
        'INVENTARIO_CONSULTAR',
        'FACTURACION_CREAR',
        'FACTURACION_CONSULTAR',
        'FACTURACION_ANULAR',
      ],
      ['CAJERO'],
    );

    const visibles = etiquetas();

    expect(visibles).not.toContain('Roles y permisos');
    expect(visibles).toContain('Ventas');
    expect(visibles).toContain('Inventario');
    expect(visibles).toContain('Facturación');
    expect(visibles).not.toContain('Contabilidad');
  });

  it('el administrador ve todos los módulos', () => {
    cargar(
      [
        'SEGURIDAD_CONSULTAR',
        'COMERCIAL_CONSULTAR',
        'INVENTARIO_CONSULTAR',
        'CONTABILIDAD_CONSULTAR',
        'FACTURACION_CONSULTAR',
        'REPORTES_CONSULTAR',
      ],
      ['ADMINISTRADOR'],
    );

    const visibles = etiquetas();

    expect(visibles).toContain('Roles y permisos');
    expect(visibles).toContain('Contabilidad');
    expect(visibles).toContain('Reportes');
  });

  it('sin ningún permiso solo queda Inicio, que no exige permisos', () => {
    cargar([], []);

    expect(etiquetas()).toEqual(['Inicio']);
  });

  it('el menú se dibuja en la plantilla con los ítems permitidos', () => {
    cargar(['SEGURIDAD_CONSULTAR'], ['ADMINISTRADOR']);

    const fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();

    const texto = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texto).toContain('Roles y permisos');
    expect(texto).not.toContain('Contabilidad');
  });
});
