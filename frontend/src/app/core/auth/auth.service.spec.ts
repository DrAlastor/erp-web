import { TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { environment } from '../../../environments/environment';

describe('AuthService', () => {
  let servicio: AuthService;
  let http: HttpTestingController;

  const respuesta = {
    token: 'un-token-firmado',
    nombre: 'Administrador Demo',
    empresaId: 'f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0',
    minutosDeVigencia: 480,
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    servicio = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('sin sesión guardada no hay nadie autenticado', () => {
    expect(servicio.estaAutenticado()).toBe(false);
    expect(servicio.token()).toBeNull();
    expect(servicio.nombre()).toBeNull();
  });

  it('el login guarda la sesión y deja al usuario autenticado', () => {
    servicio.login('admin@demo.bo', 'Admin123*').subscribe();

    const peticion = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(peticion.request.method).toBe('POST');
    peticion.flush(respuesta);

    expect(servicio.estaAutenticado()).toBe(true);
    expect(servicio.token()).toBe('un-token-firmado');
    expect(servicio.nombre()).toBe('Administrador Demo');
  });

  it('la sesión sobrevive a recargar la página', () => {
    servicio.login('admin@demo.bo', 'Admin123*').subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush(respuesta);

    // Un servicio nuevo simula la recarga: lee lo que quedó guardado.
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    const recargado = TestBed.inject(AuthService);

    expect(recargado.estaAutenticado()).toBe(true);
    expect(recargado.token()).toBe('un-token-firmado');
    TestBed.inject(HttpTestingController).verify();
  });

  it('cerrar sesión borra el token', () => {
    servicio.login('admin@demo.bo', 'Admin123*').subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush(respuesta);

    servicio.logout();

    expect(servicio.estaAutenticado()).toBe(false);
    expect(servicio.token()).toBeNull();
    expect(localStorage.getItem('erp_sesion')).toBeNull();
  });

  it('el login no pide nada al backend antes de suscribirse', () => {
    servicio.login('admin@demo.bo', 'Admin123*');

    http.expectNone(`${environment.apiUrl}/auth/login`);
  });
});

describe('AuthService en el render del servidor', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PLATFORM_ID, useValue: 'server' },
      ],
    });
  });

  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('no toca localStorage y no hay sesión', () => {
    // Si el servicio leyera localStorage en el servidor, el render de SSR se rompería.
    const servicio = TestBed.inject(AuthService);

    expect(servicio.estaAutenticado()).toBe(false);
    expect(servicio.token()).toBeNull();
  });
});
