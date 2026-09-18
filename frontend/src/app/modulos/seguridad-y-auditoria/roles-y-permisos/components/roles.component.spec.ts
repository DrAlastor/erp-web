import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RolesComponent } from './roles.component';
import { PermisosService } from '../services/permisos.service';
import { environment } from '../../../../../environments/environment';
import { Permiso, Rol } from '../models/roles.model';

const MODULOS = [
  ['SEGURIDAD', 'Seguridad y auditoría'],
  ['COMERCIAL', 'Comercial y ventas'],
  ['INVENTARIO', 'Inventarios y compras'],
  ['CONTABILIDAD', 'Contabilidad'],
  ['FACTURACION', 'Facturación'],
  ['REPORTES', 'Reportes'],
];

const ACCIONES = [
  ['CREAR', 'Crear'],
  ['CONSULTAR', 'Consultar'],
  ['MODIFICAR', 'Modificar'],
  ['ANULAR', 'Anular'],
];

/** Los 24 permisos, igual que los devuelve el backend. */
const CATALOGO: Permiso[] = MODULOS.flatMap(([modulo, etiquetaModulo]) =>
  ACCIONES.map(([accion, etiquetaAccion]) => ({
    codigo: `${modulo}_${accion}`,
    modulo,
    accion,
    etiquetaModulo,
    etiquetaAccion,
    descripcion: `${etiquetaAccion} en ${etiquetaModulo}`,
  })),
);

const CAJERO: Rol = {
  id: 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1',
  codigo: 'CAJERO',
  nombre: 'Cajero',
  descripcion: 'Registra ventas y facturación en el punto de venta',
  activo: true,
  esSistema: true,
  permisos: [
    'COMERCIAL_CREAR',
    'COMERCIAL_CONSULTAR',
    'INVENTARIO_CONSULTAR',
    'FACTURACION_CREAR',
    'FACTURACION_CONSULTAR',
    'FACTURACION_ANULAR',
  ],
};

describe('RolesComponent', () => {
  let http: HttpTestingController;
  let permisos: PermisosService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RolesComponent],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    permisos = TestBed.inject(PermisosService);
  });

  afterEach(() => http.verify());

  function conPermisosDelUsuario(codigos: string[]): void {
    permisos.cargar().subscribe();
    http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`).flush({
      usuarioId: 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3',
      empresaId: 'f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0',
      nombre: 'Administrador Demo',
      permisos: codigos,
      roles: ['ADMINISTRADOR'],
    });
  }

  function montar() {
    const fixture = TestBed.createComponent(RolesComponent);
    http.expectOne(`${environment.apiUrl}/seguridad/roles`).flush([CAJERO]);
    http.expectOne(`${environment.apiUrl}/seguridad/permisos`).flush(CATALOGO);
    fixture.detectChanges();
    return fixture;
  }

  it('dibuja la matriz con seis módulos y cuatro acciones', () => {
    conPermisosDelUsuario(['SEGURIDAD_CONSULTAR', 'SEGURIDAD_MODIFICAR']);
    const fixture = montar();

    expect(fixture.componentInstance.filas().length).toBe(6);
    expect(fixture.componentInstance.acciones().length).toBe(4);

    const casillas = (fixture.nativeElement as HTMLElement).querySelectorAll('input[type="checkbox"]');
    expect(casillas.length).toBe(24);
  });

  it('usa la notación C, L, M, A del documento en las columnas', () => {
    conPermisosDelUsuario(['SEGURIDAD_CONSULTAR']);
    const fixture = montar();

    expect(fixture.componentInstance.acciones().map((a) => a.letra)).toEqual(['C', 'L', 'M', 'A']);
  });

  it('el rol elegido llega con sus permisos marcados', () => {
    conPermisosDelUsuario(['SEGURIDAD_CONSULTAR']);
    const fixture = montar();

    expect(fixture.componentInstance.marcadosCuenta()).toBe(6);
    expect(fixture.componentInstance.estaMarcado('FACTURACION_ANULAR')).toBe(true);
    expect(fixture.componentInstance.estaMarcado('SEGURIDAD_MODIFICAR')).toBe(false);
    expect(fixture.componentInstance.hayCambios()).toBe(false);
  });

  it('al guardar manda exactamente los permisos marcados', () => {
    conPermisosDelUsuario(['SEGURIDAD_CONSULTAR', 'SEGURIDAD_MODIFICAR']);
    const fixture = montar();

    fixture.componentInstance.alternar('INVENTARIO_MODIFICAR');
    expect(fixture.componentInstance.hayCambios()).toBe(true);

    fixture.componentInstance.guardar();

    const peticion = http.expectOne(`${environment.apiUrl}/seguridad/roles/${CAJERO.id}/permisos`);
    expect(peticion.request.method).toBe('PUT');
    const enviados = (peticion.request.body as { permisos: string[] }).permisos;
    expect(enviados).toHaveLength(7);
    expect(enviados).toContain('INVENTARIO_MODIFICAR');

    peticion.flush({ ...CAJERO, permisos: enviados });
    // Al guardar se releen los permisos propios, que pueden haber cambiado.
    http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`).flush({
      usuarioId: 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3',
      empresaId: 'f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0',
      nombre: 'Administrador Demo',
      permisos: ['SEGURIDAD_CONSULTAR', 'SEGURIDAD_MODIFICAR'],
      roles: ['ADMINISTRADOR'],
    });

    expect(fixture.componentInstance.hayCambios()).toBe(false);
    expect(fixture.componentInstance.aviso()).toContain('7 permisos');
  });

  it('marcar un módulo habilita sus cuatro acciones de una vez', () => {
    conPermisosDelUsuario(['SEGURIDAD_CONSULTAR', 'SEGURIDAD_MODIFICAR']);
    const fixture = montar();
    const contabilidad = fixture.componentInstance.filas().find((f) => f.modulo === 'CONTABILIDAD')!;

    fixture.componentInstance.alternarModulo(contabilidad);

    expect(fixture.componentInstance.moduloCompleto(contabilidad)).toBe(true);
    expect(fixture.componentInstance.estaMarcado('CONTABILIDAD_ANULAR')).toBe(true);
  });

  it('sin el permiso de modificar la matriz queda en modo consulta', () => {
    conPermisosDelUsuario(['SEGURIDAD_CONSULTAR']);
    const fixture = montar();

    expect(fixture.componentInstance.puedeEditar()).toBe(false);

    fixture.componentInstance.alternar('CONTABILIDAD_CREAR');
    expect(fixture.componentInstance.estaMarcado('CONTABILIDAD_CREAR')).toBe(false);

    const plantilla = fixture.nativeElement as HTMLElement;
    const deshabilitadas = plantilla.querySelectorAll('input[type="checkbox"]:disabled');
    expect(deshabilitadas.length).toBe(24);
    expect(plantilla.textContent).toContain('modo consulta');
  });

  it('un 403 del backend se muestra como acceso denegado', () => {
    conPermisosDelUsuario(['SEGURIDAD_CONSULTAR', 'SEGURIDAD_MODIFICAR']);
    const fixture = montar();

    fixture.componentInstance.alternar('INVENTARIO_MODIFICAR');
    fixture.componentInstance.guardar();

    http.expectOne(`${environment.apiUrl}/seguridad/roles/${CAJERO.id}/permisos`).flush(
      { error: 'ACCESO_DENEGADO', mensaje: 'No tiene permiso para realizar esta operación' },
      { status: 403, statusText: 'Forbidden' },
    );

    expect(fixture.componentInstance.error()).toBe('No tiene permiso para realizar esta operación');
  });

  it('descartar vuelve a la matriz guardada', () => {
    conPermisosDelUsuario(['SEGURIDAD_CONSULTAR', 'SEGURIDAD_MODIFICAR']);
    const fixture = montar();

    fixture.componentInstance.alternar('CONTABILIDAD_CREAR');
    fixture.componentInstance.descartar();

    expect(fixture.componentInstance.hayCambios()).toBe(false);
    expect(fixture.componentInstance.marcadosCuenta()).toBe(6);
  });
});
