import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom, isObservable, of } from 'rxjs';
import { permisoGuard } from './permiso.guard';
import { authGuard } from './auth.guard';
import { PermisosService } from '../permisos/permisos.service';
import { AuthService } from '../auth/auth.service';
import { environment } from '../../../environments/environment';

/** Ejecuta un guard dentro del contexto de inyección y normaliza el resultado. */
async function correr(guard: ReturnType<typeof permisoGuard>, url = '/app/seguridad/roles') {
  const resultado = TestBed.runInInjectionContext(() =>
    guard({} as never, { url } as never),
  );
  const valor = isObservable(resultado) ? await firstValueFrom(resultado) : await resultado;
  return valor;
}

describe('permisoGuard', () => {
  let http: HttpTestingController;
  let permisos: PermisosService;

  const misPermisos = {
    usuarioId: 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3',
    empresaId: 'f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0',
    nombre: 'Cajero Demo',
    permisos: ['COMERCIAL_CONSULTAR'],
    roles: ['CAJERO'],
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    permisos = TestBed.inject(PermisosService);
  });

  afterEach(() => http.verify());

  function cargarPermisos(): void {
    permisos.cargar().subscribe();
    http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`).flush(misPermisos);
  }

  it('con el permiso deja pasar', async () => {
    cargarPermisos();

    await expect(correr(permisoGuard('COMERCIAL_CONSULTAR'))).resolves.toBe(true);
  });

  it('sin el permiso manda a la pantalla de sin acceso', async () => {
    cargarPermisos();

    const resultado = await correr(permisoGuard('SEGURIDAD_MODIFICAR'));

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(String(resultado)).toBe('/app/sin-acceso');
  });

  it('si los permisos no estaban cargados, los pide antes de decidir', async () => {
    const promesa = correr(permisoGuard('COMERCIAL_CONSULTAR'));

    http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`).flush(misPermisos);

    await expect(promesa).resolves.toBe(true);
  });
});

describe('authGuard', () => {
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    http.verify();
    localStorage.clear();
  });

  it('sin sesión redirige al login guardando a dónde quería ir', async () => {
    const resultado = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/app/seguridad/roles' } as never),
    );

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(String(resultado)).toContain('/login');
    expect(String(resultado)).toContain('volverA=%2Fapp%2Fseguridad%2Froles');
  });

  it('con sesión válida deja pasar después de cargar los permisos', async () => {
    auth.login('admin@demo.bo', 'Admin123*').subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush({
      token: 'un-token',
      nombre: 'Administrador Demo',
      empresaId: 'f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0',
      minutosDeVigencia: 480,
    });

    const resultado = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/app' } as never),
    );
    const observable = isObservable(resultado) ? resultado : of(resultado);
    const promesa = firstValueFrom(observable);

    http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`).flush({
      usuarioId: 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3',
      empresaId: 'f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0',
      nombre: 'Administrador Demo',
      permisos: ['SEGURIDAD_CONSULTAR'],
      roles: ['ADMINISTRADOR'],
    });

    await expect(promesa).resolves.toBe(true);
  });

  it('si el token está vencido cierra la sesión y manda al login', async () => {
    auth.login('admin@demo.bo', 'Admin123*').subscribe();
    http.expectOne(`${environment.apiUrl}/auth/login`).flush({
      token: 'un-token-vencido',
      nombre: 'Administrador Demo',
      empresaId: 'f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0',
      minutosDeVigencia: 480,
    });

    const resultado = TestBed.runInInjectionContext(() =>
      authGuard({} as never, { url: '/app' } as never),
    );
    const observable = isObservable(resultado) ? resultado : of(resultado);
    const promesa = firstValueFrom(observable);

    http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`)
      .flush({ error: 'NO_AUTENTICADO' }, { status: 401, statusText: 'Unauthorized' });

    const destino = await promesa;
    expect(destino).toBeInstanceOf(UrlTree);
    expect(String(destino)).toContain('/login');
    expect(auth.estaAutenticado()).toBe(false);
  });
});
