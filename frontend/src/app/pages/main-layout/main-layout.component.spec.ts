import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { signal } from '@angular/core';
import { MainLayoutComponent } from './main-layout.component';
import { AuthService } from '../../modulos/seguridad-y-auditoria/acceso-al-sistema/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';
import { ERP_MODULES } from '../../core/models/erp-navigation';
import { environment } from '../../../environments/environment';

describe('Panel de control compartido', () => {
  const params = new BehaviorSubject(convertToParamMap({}));
  const user = signal({
    id: 1,
    username: 'empleado',
    email: 'empleado@test.local',
    fullname: 'Empleado ERP',
  });
  let logoutCalls = 0;
  beforeEach(async () => {
    params.next(convertToParamMap({}));
    logoutCalls = 0;
    await TestBed.configureTestingModule({
      imports: [MainLayoutComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { queryParamMap: params } },
        {
          provide: AuthService,
          useValue: {
            usuario: user,
            logout: () => logoutCalls++,
            hasPermission: () => false,
            isClient: () => false,
          },
        },
        { provide: ThemeService, useValue: { isDark: () => false, toggle: () => {} } },
      ],
    }).compileComponents();
  });
  it('muestra los seis módulos y las veinte funciones sin requerir permisos de administrador', () => {
    const fixture = TestBed.createComponent(MainLayoutComponent);
    fixture.detectChanges();
    const nav = fixture.nativeElement.querySelector('nav') as HTMLElement;
    expect(nav.querySelectorAll('.module-toggle').length).toBe(6);
    expect(nav.querySelectorAll('.function-list a').length).toBe(21);
    for (const module of ERP_MODULES) {
      expect(nav.textContent).toContain(module.name);
      for (const fn of module.functions) expect(nav.textContent).toContain(fn.name);
    }
    expect(nav.textContent).not.toMatch(/HU-?\d/);
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Dashboard');
  });
  it('permite navegar entre funciones y volver al dashboard sin perder el panel', () => {
    const fixture = TestBed.createComponent(MainLayoutComponent);
    fixture.detectChanges();
    params.next(
      convertToParamMap({ modulo: 'comercial-y-preventa', funcion: 'gestion-de-clientes' }),
    );
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Gestión de Clientes');
    expect(fixture.nativeElement.querySelector('main').textContent).toContain(
      'Directorio de clientes',
    );
    expect(fixture.nativeElement.querySelector('nav a[aria-current="page"]').textContent).toBe(
      'Gestión de Clientes',
    );
    params.next(convertToParamMap({}));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Dashboard');
  });
  it('muestra la sesión del CU01 y conserva el cierre de sesión', () => {
    params.next(
      convertToParamMap({ modulo: 'seguridad-y-auditoria', funcion: 'acceso-al-sistema' }),
    );
    const fixture = TestBed.createComponent(MainLayoutComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('main').textContent).toContain(
      'empleado@test.local',
    );
    fixture.nativeElement.querySelector('main button').click();
    expect(logoutCalls).toBe(1);
  });
  it('renderiza CU02 dentro del dashboard en lugar del marcador pendiente', () => {
    params.next(
      convertToParamMap({ modulo: 'seguridad-y-auditoria', funcion: 'gestion-de-usuarios' }),
    );
    const fixture = TestBed.createComponent(MainLayoutComponent);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${environment.apiUrl}/usuarios/roles`).flush([]);
    http
      .expectOne((req) => req.url === `${environment.apiUrl}/usuarios`)
      .flush({
        content: [
          {
            id: 1,
            username: 'admin',
            fullname: 'Administrador General',
            email: 'admin@erp.com',
            enable: true,
            roles: [],
            createdAt: null,
            updatedAt: null,
          },
        ],
        totalElements: 1,
        totalPages: 1,
      });
    fixture.detectChanges();
    const main = fixture.nativeElement.querySelector('main') as HTMLElement;
    expect(main.querySelector('app-usuarios table')).not.toBeNull();
    expect(main.textContent).toContain('admin@erp.com');
    expect(main.textContent).not.toContain('En desarrollo');
    http.verify();
  });
  it('rechaza selecciones que no pertenecen al catálogo', () => {
    params.next(convertToParamMap({ modulo: 'caja-y-arqueo', funcion: 'gestion-de-usuarios' }));
    const fixture = TestBed.createComponent(MainLayoutComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1').textContent).toBe('Función no encontrada');
  });
});
