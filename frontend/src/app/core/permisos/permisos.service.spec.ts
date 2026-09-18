import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PermisosService } from './permisos.service';
import { environment } from '../../../environments/environment';

describe('PermisosService', () => {
  let servicio: PermisosService;
  let http: HttpTestingController;

  const respuesta = {
    usuarioId: 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3',
    empresaId: 'f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0',
    nombre: 'Cajero Demo',
    permisos: ['COMERCIAL_CREAR', 'COMERCIAL_CONSULTAR', 'FACTURACION_CREAR'],
    roles: ['CAJERO'],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servicio = TestBed.inject(PermisosService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('antes de cargar no concede ningún permiso', () => {
    expect(servicio.cargado()).toBe(false);
    expect(servicio.tiene('COMERCIAL_CREAR')).toBe(false);
    expect(servicio.cantidad()).toBe(0);
  });

  it('cargar pide los permisos efectivos al backend', () => {
    servicio.cargar().subscribe();

    const peticion = http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`);
    expect(peticion.request.method).toBe('GET');
    peticion.flush(respuesta);

    expect(servicio.cargado()).toBe(true);
    expect(servicio.cantidad()).toBe(3);
    expect(servicio.tiene('COMERCIAL_CREAR')).toBe(true);
    expect(servicio.tiene('SEGURIDAD_MODIFICAR')).toBe(false);
    expect(servicio.roles()).toEqual(['CAJERO']);
  });

  it('tieneAlguno sirve para los ítems de menú que aceptan varios permisos', () => {
    servicio.cargar().subscribe();
    http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`).flush(respuesta);

    expect(servicio.tieneAlguno(['SEGURIDAD_CONSULTAR', 'COMERCIAL_CONSULTAR'])).toBe(true);
    expect(servicio.tieneAlguno(['SEGURIDAD_CONSULTAR', 'CONTABILIDAD_CONSULTAR'])).toBe(false);
    expect(servicio.tieneAlguno([])).toBe(false);
  });

  it('asegurarCargados no vuelve a pedir si ya están cargados', () => {
    servicio.cargar().subscribe();
    http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`).flush(respuesta);

    let listo = false;
    servicio.asegurarCargados().subscribe((valor) => (listo = valor));

    expect(listo).toBe(true);
    http.expectNone(`${environment.apiUrl}/seguridad/mis-permisos`);
  });

  it('limpiar deja al usuario sin permisos', () => {
    servicio.cargar().subscribe();
    http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`).flush(respuesta);

    servicio.limpiar();

    expect(servicio.cargado()).toBe(false);
    expect(servicio.cantidad()).toBe(0);
    expect(servicio.tiene('COMERCIAL_CREAR')).toBe(false);
  });
});
