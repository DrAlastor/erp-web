import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UsuariosComponent } from './usuarios.component';
import { environment } from '../../../../../environments/environment';
const url = `${environment.apiUrl}/usuarios`;
const account = {
  id: 2,
  username: 'test',
  fullname: 'Test User',
  email: 'test@test.local',
  enable: true,
  roles: [],
  createdAt: null,
  updatedAt: null,
};
describe('Gestion de usuarios', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [UsuariosComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function start() {
    const fixture = TestBed.createComponent(UsuariosComponent);
    http.expectOne(`${url}/roles`).flush([{ id: 1, nombre: 'ADMIN' }]);
    http
      .expectOne((req) => req.url === url)
      .flush({ content: [account], totalElements: 1, totalPages: 1 });
    fixture.detectChanges();
    return fixture;
  }
  it('carga cuentas y envia los filtros al servidor', () => {
    const fixture = start();
    expect(fixture.nativeElement.textContent).toContain('Test User');
    fixture.componentInstance.search = 'test';
    fixture.componentInstance.role = '1';
    fixture.componentInstance.enable = 'false';
    fixture.componentInstance.load(true);
    const req = http.expectOne((req) => req.url === url);
    expect(req.request.params.get('search')).toBe('test');
    expect(req.request.params.get('role')).toBe('1');
    expect(req.request.params.get('enable')).toBe('false');
    req.flush({ content: [], totalElements: 0, totalPages: 0 });
  });
  it('solo envia campos administrativos al guardar', () => {
    const fixture = start();
    fixture.componentInstance.open(account);
    http.expectOne(`${url}/2`).flush(account);
    fixture.componentInstance.fullname = 'Changed';
    fixture.componentInstance.save();
    const req = http.expectOne(`${url}/2`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ fullname: 'Changed', email: 'test@test.local' });
    req.flush({ ...account, fullname: 'Changed' });
    http
      .expectOne((req) => req.url === url)
      .flush({ content: [], totalElements: 0, totalPages: 0 });
    expect(fixture.componentInstance.success()).toBeTruthy();
  });
  it('no cambia el estado hasta confirmar y maneja denegacion de permisos', () => {
    const fixture = start();
    fixture.componentInstance.confirmation.set(account);
    fixture.detectChanges();
    http.expectNone(`${url}/2/status`);
    expect(fixture.nativeElement.querySelector('[role="dialog"]')).toBeTruthy();
    fixture.componentInstance.changeStatus();
    const req = http.expectOne(`${url}/2/status`);
    expect(req.request.body).toEqual({ enable: false });
    req.flush({ message: 'Denied' }, { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();
    expect(fixture.componentInstance.forbidden()).toBe(true);
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
  });
});
