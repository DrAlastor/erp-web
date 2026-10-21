import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { PersonalizarAparienciaComponent } from './personalizar-apariencia.component';
import { AparienciaService } from '../services/apariencia.service';
import { PermisosService } from '../../cu03-gestionar-roles-y-permisos/services/permisos.service';
import { APARIENCIA_POR_DEFECTO } from '../models/apariencia.model';

describe('CU06 Personalizar Apariencia', () => {
  const permisos = signal<ReadonlySet<string>>(new Set());
  const servicio = {
    apariencia: signal(APARIENCIA_POR_DEFECTO),
    logoUrl: signal<string | null>(null),
    cargar: vi.fn(() => of(APARIENCIA_POR_DEFECTO)),
    guardar: vi.fn(() => of(APARIENCIA_POR_DEFECTO)),
    subirLogo: vi.fn(() => of(APARIENCIA_POR_DEFECTO)),
    eliminarLogo: vi.fn(),
    restablecer: vi.fn(() => of(APARIENCIA_POR_DEFECTO)),
    previsualizar: vi.fn(),
    restaurar: vi.fn(),
  };

  function crear(codigos: string[]) {
    permisos.set(new Set(codigos));
    const fixture = TestBed.createComponent(PersonalizarAparienciaComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    Object.values(servicio).forEach((v) => typeof v === 'function' && 'mockClear' in v && v.mockClear());
    await TestBed.configureTestingModule({
      imports: [PersonalizarAparienciaComponent],
      providers: [
        { provide: AparienciaService, useValue: servicio },
        { provide: PermisosService, useValue: { tiene: (c: string) => permisos().has(c) } },
      ],
    }).compileComponents();
  });

  it('sin SEGURIDAD_MODIFICAR la pantalla es de solo lectura', () => {
    const fixture = crear(['SEGURIDAD_CONSULTAR']);
    const el = fixture.nativeElement as HTMLElement;

    expect(el.textContent).toContain('solo lectura');
    expect(fixture.componentInstance.formulario.disabled).toBe(true);
    expect(el.querySelector('.acciones')).toBeNull();
  });

  it('con SEGURIDAD_MODIFICAR muestra la vista previa en vivo y guarda', () => {
    const fixture = crear(['SEGURIDAD_MODIFICAR']);
    const componente = fixture.componentInstance;
    expect(componente.formulario.enabled).toBe(true);

    componente.aplicarPaleta({ nombre: 'Esmeralda', primario: '#059669', secundario: '#064E3B', acento: '#F59E0B' });
    expect(servicio.previsualizar).toHaveBeenCalledWith(expect.objectContaining({ colorPrimario: '#059669' }));
    expect(componente.hayCambios()).toBe(true);

    componente.guardar();
    expect(servicio.guardar).toHaveBeenCalledWith(expect.objectContaining({
      colorPrimario: '#059669', colorAcento: '#F59E0B', encabezadoFactura: null,
    }));
  });

  it('no envía un color con formato inválido', () => {
    const componente = crear(['SEGURIDAD_MODIFICAR']).componentInstance;
    componente.formulario.controls.colorPrimario.setValue('rojo');

    componente.guardar();

    expect(servicio.guardar).not.toHaveBeenCalled();
  });

  it('rechaza logos de más de 512 KB o que no son imágenes antes de subirlos', () => {
    const componente = crear(['SEGURIDAD_MODIFICAR']).componentInstance;
    const grande = new File([new Uint8Array(512 * 1024 + 1)], 'logo.png', { type: 'image/png' });
    const pdf = new File(['%PDF'], 'logo.pdf', { type: 'application/pdf' });
    const valido = new File(['x'], 'logo.png', { type: 'image/png' });

    expect(componente.validarLogo(grande)).toContain('512 KB');
    expect(componente.validarLogo(pdf)).toContain('PNG');
    expect(componente.validarLogo(valido)).toBeNull();
  });

  it('cancelar descarta la vista previa', () => {
    const componente = crear(['SEGURIDAD_MODIFICAR']).componentInstance;
    componente.formulario.controls.tema.setValue('OSCURO');

    componente.cancelar();

    expect(servicio.restaurar).toHaveBeenCalled();
    expect(componente.formulario.controls.tema.value).toBe('CLARO');
    expect(componente.hayCambios()).toBe(false);
  });
});
