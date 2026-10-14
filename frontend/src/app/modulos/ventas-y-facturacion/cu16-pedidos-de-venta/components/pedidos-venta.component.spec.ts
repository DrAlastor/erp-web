import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PedidosVentaComponent } from './pedidos-venta.component';
import { PedidoFormComponent } from './pedido-form.component';
import { environment } from '../../../../../environments/environment';
import { validarPedido } from '../utils/pedido.utils';
import { LineaPedido } from '../models/pedido.model';

const url = `${environment.apiUrl}/pedidos`;
const resumen = {
  id: 5, numeroPedido: 'PED-000005', fecha: '2026-10-14', clienteId: 2, clienteRazonSocial: 'Cliente de prueba',
  clienteNitCi: '123456', sucursalId: 1, sucursalNombre: 'Central', vendedorId: 3, vendedorNombre: 'Ana',
  subtotal: 100, descuento: 0, total: 100, estado: 'PENDIENTE' as const, cantidadItems: 1,
};
const page = (content: unknown[]) => ({ content, totalElements: content.length, totalPages: 1, number: 0, size: 15 });
const linea = (over: Partial<LineaPedido> = {}): LineaPedido => ({
  productoId: 1, codigo: 'P-1', nombre: 'Producto', stock: 10, cantidad: 2, precioUnitario: 50, descuento: 0, ...over,
});

describe('Gestion de pedidos de venta', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [PedidosVentaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('carga pedidos y envia los filtros al servidor', () => {
    const fixture = TestBed.createComponent(PedidosVentaComponent);
    http.expectOne((r) => r.url === url).flush(page([resumen]));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('PED-000005');
    expect(fixture.nativeElement.textContent).toContain('Bs. 100.00');
    fixture.componentInstance.search = 'PED';
    fixture.componentInstance.estado = 'APROBADO';
    fixture.componentInstance.load(true);
    const request = http.expectOne((r) => r.url === url);
    expect(request.request.params.get('search')).toBe('PED');
    expect(request.request.params.get('estado')).toBe('APROBADO');
    expect(request.request.params.get('size')).toBe('15');
    request.flush(page([]));
  });

  it('muestra el mensaje de error del backend', () => {
    const fixture = TestBed.createComponent(PedidosVentaComponent);
    http.expectOne((r) => r.url === url).flush({ message: 'Estado invalido' }, { status: 422, statusText: 'x' });
    fixture.detectChanges();
    expect(fixture.componentInstance.error()).toBe('Estado invalido');
  });

  it('el formulario valida antes de enviar y luego crea el pedido', () => {
    const fixture = TestBed.createComponent(PedidoFormComponent);
    http.expectOne(`${url}/opciones/sucursales`).flush([{ id: 1, nombre: 'Central' }]);
    const form = fixture.componentInstance;
    form.save();
    http.expectNone(url);
    expect(form.validacion()).toBe('Selecciona un cliente.');
    form.cliente.set({ id: 2, razonSocial: 'Cliente', nitCi: '1' });
    form.lineas.set([linea({ descuento: 10 })]);
    form.descuento.set(5);
    expect(form.total()).toBe(85);
    form.save();
    const request = http.expectOne((r) => r.url === url && r.method === 'POST');
    expect(request.request.body.sucursalId).toBe(1);
    expect(request.request.body.detalles).toEqual([{ productoId: 1, cantidad: 2, precioUnitario: 50, descuento: 10 }]);
    request.flush({ ...resumen, observaciones: null, fechaCreacion: null, detalles: [] });
  });

  it('aplica las reglas de negocio de las lineas', () => {
    const base = { clienteId: 1, sucursalId: 1, descuento: 0 };
    expect(validarPedido(base, [])).toContain('al menos un producto');
    expect(validarPedido(base, [linea({ cantidad: 0 })])).toContain('mayor a 0');
    expect(validarPedido(base, [linea(), linea()])).toContain('repetido');
    expect(validarPedido(base, [linea({ descuento: 101 })])).toContain('supera el subtotal');
    expect(validarPedido({ ...base, descuento: 500 }, [linea()])).toContain('descuento global');
    expect(validarPedido(base, [linea()])).toBe('');
  });
});
