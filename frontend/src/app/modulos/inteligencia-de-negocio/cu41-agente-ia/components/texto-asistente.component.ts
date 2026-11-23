import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { bloques } from '../utils/markdown';

/** Texto del asistente con Markdown mínimo, construido con elementos de Angular (nunca HTML crudo). */
@Component({
  selector: 'app-texto-asistente',
  standalone: true,
  imports: [NgTemplateOutlet],
  template: `
    <ng-template #linea let-partes>
      @for (f of partes; track $index) {
        @switch (f.tipo) {
          @case ('negrita') { <strong>{{ f.texto }}</strong> }
          @case ('cursiva') { <em>{{ f.texto }}</em> }
          @case ('codigo') { <code>{{ f.texto }}</code> }
          @default { {{ f.texto }} }
        }
      }
    </ng-template>
    @for (b of bloques(); track $index) {
      @switch (b.tipo) {
        @case ('titulo') { <h4><ng-container *ngTemplateOutlet="linea; context: { $implicit: $any(b).partes }" /></h4> }
        @case ('lista') {
          @if ($any(b).ordenada) {
            <ol>@for (it of $any(b).items; track $index) { <li><ng-container *ngTemplateOutlet="linea; context: { $implicit: it }" /></li> }</ol>
          } @else {
            <ul>@for (it of $any(b).items; track $index) { <li><ng-container *ngTemplateOutlet="linea; context: { $implicit: it }" /></li> }</ul>
          }
        }
        @case ('tabla') {
          <div class="tabla-md">
            <table>
              <thead><tr>@for (c of $any(b).encabezado; track $index) { <th><ng-container *ngTemplateOutlet="linea; context: { $implicit: c }" /></th> }</tr></thead>
              <tbody>
                @for (fila of $any(b).filas; track $index) {
                  <tr>@for (c of fila; track $index) { <td><ng-container *ngTemplateOutlet="linea; context: { $implicit: c }" /></td> }</tr>
                }
              </tbody>
            </table>
          </div>
        }
        @default { <p><ng-container *ngTemplateOutlet="linea; context: { $implicit: $any(b).partes }" /></p> }
      }
    }
  `,
  styles: [`
    :host { display: block; width: 100%; font-size: 14px; line-height: 1.55; color: var(--text-main); }
    p { margin: 0 0 8px; word-break: break-word; }
    p:last-child { margin-bottom: 0; }
    h4 { margin: 10px 0 6px; font-size: 14px; font-weight: 600; }
    ul, ol { margin: 0 0 8px; padding-left: 20px; }
    li { margin: 2px 0; }
    code { padding: 1px 5px; border-radius: 5px; font-family: var(--font-mono); font-size: 12px; background: var(--bg-subtle); }
    .tabla-md {
      margin: 8px 0 12px;
      width: 100%;
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      border-radius: 8px;
      border: 1px solid var(--bg-card-border);
      background: var(--bg-surface);
    }
    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 12px;
      text-align: left;
    }
    th, td {
      padding: 6px 12px;
      border: 1px solid var(--bg-card-border);
      text-align: left;
      vertical-align: top;
      white-space: normal;
      word-break: break-word;
    }
    th {
      background: var(--bg-subtle);
      font-weight: 600;
      white-space: nowrap;
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TextoAsistenteComponent {
  readonly texto = input('');
  readonly bloques = computed(() => bloques(this.texto()));
}
