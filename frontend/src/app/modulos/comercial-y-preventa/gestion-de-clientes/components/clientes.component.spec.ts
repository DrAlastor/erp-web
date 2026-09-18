import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ClientesComponent } from './clientes.component';
import { environment } from '../../../../../environments/environment';

const url = `${environment.apiUrl}/clientes`;
const cliente = {
  id: 2,
  razonSocial: 'Cliente de prueba',
  nitCi: '123456789',
  telefono: '70000000',
  direccion: 'Dirección de prueba',
  activo: true,
  fechaCreacion: null,
};

describe('Gestion de clientes', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ClientesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  function start() {
    const fixture = TestBed.createComponent(ClientesComponent);
    http
      .expectOne((request) => request.url === url)
      .flush({ content: [cliente], totalElements: 1, totalPages: 1 });
    fixture.detectChanges();
    return fixture;
  }

  it('carga clientes y envia los filtros al servidor', () => {
    const fixture = start();
    expect(fixture.nativeElement.textContent).toContain('Cliente de prueba');
    fixture.componentInstance.search = 'prueba';
    fixture.componentInstance.activo = 'true';
    fixture.componentInstance.load(true);
    const request = http.expectOne((item) => item.url === url);
    expect(request.request.params.get('search')).toBe('prueba');
    expect(request.request.params.get('activo')).toBe('true');
    request.flush({ content: [], totalElements: 0, totalPages: 0 });
  });

  it('crea un cliente y actualiza el listado', () => {
    const fixture = start();
    fixture.componentInstance.openNew();
    fixture.componentInstance.razonSocial = 'Nueva empresa';
    fixture.componentInstance.nitCi = '987654321';
    fixture.componentInstance.telefono = '71111111';
    fixture.componentInstance.direccion = 'Nueva dirección';
    fixture.componentInstance.save();
    const request = http.expectOne(`${url}`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      razonSocial: 'Nueva empresa',
      nitCi: '987654321',
      telefono: '71111111',
      direccion: 'Nueva dirección',
    });
    request.flush({ ...cliente, razonSocial: 'Nueva empresa' });
    http
      .expectOne((item) => item.url === url)
      .flush({ content: [], totalElements: 0, totalPages: 0 });
    expect(fixture.componentInstance.success()).toBeTruthy();
  });

  it('edita y cambia el estado solo después de confirmar', () => {
    const fixture = start();
    fixture.componentInstance.open(cliente);
    http.expectOne(`${url}/2`).flush(cliente);
    fixture.componentInstance.razonSocial = 'Cliente actualizado';
    fixture.componentInstance.save();
    const update = http.expectOne(`${url}/2`);
    expect(update.request.method).toBe('PUT');
    update.flush({ ...cliente, razonSocial: 'Cliente actualizado' });
    http
      .expectOne((item) => item.url === url)
      .flush({ content: [], totalElements: 0, totalPages: 0 });

    fixture.componentInstance.confirmation.set(cliente);
    fixture.detectChanges();
    http.expectNone(`${url}/2/status`);
    fixture.componentInstance.changeStatus();
    const status = http.expectOne(`${url}/2/status`);
    expect(status.request.body).toEqual({ activo: false });
    status.flush({ ...cliente, activo: false });
    http
      .expectOne((item) => item.url === url)
      .flush({ content: [], totalElements: 0, totalPages: 0 });
  });
});
