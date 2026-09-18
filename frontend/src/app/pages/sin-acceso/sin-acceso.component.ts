import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Pantalla a la que llega quien intenta entrar a un módulo para el que no tiene permiso. */
@Component({
  selector: 'app-sin-acceso',
  imports: [RouterLink],
  template: `
    <section class="aviso">
      <h1>No tenés acceso a esta sección</h1>
      <p>
        Tu rol no incluye los permisos que este módulo requiere. Si necesitás entrar, pedile al
        administrador del sistema que te asigne el rol correspondiente.
      </p>
      <a routerLink="/app">Volver al inicio</a>
    </section>
  `,
  styles: `
    .aviso {
      max-width: 34rem;
      padding: 2rem;
      background: var(--bg-surface);
      border: 1px solid var(--bg-card-border);
      border-radius: 16px;
    }

    h1 {
      font-size: 1.35rem;
      font-weight: 700;
      letter-spacing: -0.01em;
      color: var(--text-main);
    }

    p {
      margin: 0.65rem 0 1.25rem;
      font-size: 0.95rem;
      line-height: 1.6;
      color: var(--text-secondary);
    }

    a {
      font-size: 0.9rem;
      font-weight: 600;
      color: var(--primary);
      text-decoration: none;
    }

    a:hover {
      text-decoration: underline;
    }
  `,
})
export class SinAccesoComponent {}
