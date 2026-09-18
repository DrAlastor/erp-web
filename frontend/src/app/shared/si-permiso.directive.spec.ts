import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { SiPermisoDirective } from './si-permiso.directive';
import { PermisosService } from '../core/permisos/permisos.service';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-anfitrion-prueba',
  imports: [SiPermisoDirective],
  template: `
    <button type="button" *siPermiso="'SEGURIDAD_MODIFICAR'" data-prueba="guardar">Guardar matriz</button>
    <span *siPermiso="'COMERCIAL_CONSULTAR'" data-prueba="ventas">Ventas</span>
  `,
})
class AnfitrionDePrueba {}

describe('SiPermisoDirective', () => {
  let http: HttpTestingController;
  let permisos: PermisosService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AnfitrionDePrueba],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    permisos = TestBed.inject(PermisosService);
  });

  afterEach(() => http.verify());

  function cargar(codigos: string[]): void {
    permisos.cargar().subscribe();
    http.expectOne(`${environment.apiUrl}/seguridad/mis-permisos`).flush({
      usuarioId: 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3',
      empresaId: 'f0f0f0f0-f0f0-f0f0-f0f0-f0f0f0f0f0f0',
      nombre: 'Usuario de prueba',
      permisos: codigos,
      roles: [],
    });
  }

  it('sin permisos no dibuja nada', async () => {
    const fixture = TestBed.createComponent(AnfitrionDePrueba);
    await fixture.whenStable();

    const plantilla = fixture.nativeElement as HTMLElement;
    expect(plantilla.querySelector('[data-prueba="guardar"]')).toBeNull();
    expect(plantilla.querySelector('[data-prueba="ventas"]')).toBeNull();
  });

  it('dibuja solo el contenido cuyo permiso tiene el usuario', async () => {
    cargar(['COMERCIAL_CONSULTAR']);

    const fixture = TestBed.createComponent(AnfitrionDePrueba);
    await fixture.whenStable();

    const plantilla = fixture.nativeElement as HTMLElement;
    expect(plantilla.querySelector('[data-prueba="ventas"]')).not.toBeNull();
    expect(plantilla.querySelector('[data-prueba="guardar"]')).toBeNull();
  });

  it('con el permiso de modificar aparece el botón de guardar', async () => {
    cargar(['SEGURIDAD_MODIFICAR', 'COMERCIAL_CONSULTAR']);

    const fixture = TestBed.createComponent(AnfitrionDePrueba);
    await fixture.whenStable();

    const plantilla = fixture.nativeElement as HTMLElement;
    expect(plantilla.querySelector('[data-prueba="guardar"]')?.textContent).toContain('Guardar matriz');
  });

  it('reacciona cuando los permisos cambian: al limpiarlos, el botón se va', async () => {
    cargar(['SEGURIDAD_MODIFICAR']);

    const fixture = TestBed.createComponent(AnfitrionDePrueba);
    await fixture.whenStable();
    const plantilla = fixture.nativeElement as HTMLElement;
    expect(plantilla.querySelector('[data-prueba="guardar"]')).not.toBeNull();

    permisos.limpiar();
    await fixture.whenStable();

    expect(plantilla.querySelector('[data-prueba="guardar"]')).toBeNull();
  });
});
