import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('se crea', () => {
    const fixture = TestBed.createComponent(App);

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('monta el router donde se dibujan las páginas', async () => {
    // Antes esta prueba buscaba un <h1>Hello, frontend</h1> que ya no existe en la
    // plantilla: quedó desactualizada cuando la app pasó a enrutar la landing.
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();

    const plantilla = fixture.nativeElement as HTMLElement;
    expect(plantilla.querySelector('router-outlet')).not.toBeNull();
  });
});
