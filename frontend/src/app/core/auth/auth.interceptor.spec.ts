import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { environment } from '../../../environments/environment';

describe('authInterceptor', () => {
  let http: HttpTestingController;
  let cliente: HttpClient;
  let auth: AuthService;

  const respuestaLogin = {
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
    http = TestBed.inject(HttpTestingController);
    cliente = TestBed.inject(HttpClient);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  function iniciarSesion(): void {
    auth.login('admin@demo.bo', 'Admin123*').subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush(respuestaLogin);
  }

  it('sin sesión no agrega la cabecera', () => {
    cliente.get(`${environment.apiUrl}/seguridad/roles`).subscribe();

    const peticion = http.expectOne(`${environment.apiUrl}/seguridad/roles`);
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush([]);
  });

  it('con sesión agrega el token como Bearer', () => {
    iniciarSesion();

    cliente.get(`${environment.apiUrl}/seguridad/roles`).subscribe();

    const peticion = http.expectOne(`${environment.apiUrl}/seguridad/roles`);
    expect(peticion.request.headers.get('Authorization')).toBe('Bearer un-token-firmado');
    peticion.flush([]);
  });

  it('no manda la cabecera al propio login', () => {
    iniciarSesion();

    auth.login('otro@demo.bo', 'otra').subscribe();

    const peticion = http.expectOne(`${environment.apiUrl}/auth/login`);
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush(respuestaLogin);
  });
});
