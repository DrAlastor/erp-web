import { DOCUMENT } from '@angular/common';
import { DestroyRef, Directive, ElementRef, afterNextRender, inject, output } from '@angular/core';

const FOCUSABLE = 'button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [href], [tabindex]:not([tabindex="-1"])';

/**
 * Accesibilidad básica de los modales de CU34: lleva el foco al diálogo al abrir, lo mantiene
 * dentro con Tab / Shift+Tab, emite `cerrar` con Esc y devuelve el foco al elemento que lo abrió.
 */
@Directive({
  selector: '[appModalFoco]',
  host: { tabindex: '-1', '(keydown)': 'onKeydown($event)' },
})
export class ModalFocoDirective {
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly previo = inject(DOCUMENT).activeElement as HTMLElement | null;
  readonly cerrar = output<void>();

  constructor() {
    afterNextRender(() => {
      const primero = this.host.nativeElement.querySelector<HTMLElement>('[autofocus], ' + FOCUSABLE);
      (primero ?? this.host.nativeElement).focus();
    });
    inject(DestroyRef).onDestroy(() => this.previo?.focus?.());
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') {
      event.stopPropagation();
      this.cerrar.emit();
      return;
    }
    if (event.key !== 'Tab') return;
    const nodos = Array.from(this.host.nativeElement.querySelectorAll<HTMLElement>(FOCUSABLE))
      .filter((n) => !n.closest('[appModalFoco]') || n.closest('[appModalFoco]') === this.host.nativeElement);
    if (!nodos.length) return;
    const [primero, ultimo] = [nodos[0], nodos[nodos.length - 1]];
    const activo = this.host.nativeElement.ownerDocument.activeElement;
    if (event.shiftKey && activo === primero) {
      event.preventDefault();
      ultimo.focus();
    } else if (!event.shiftKey && activo === ultimo) {
      event.preventDefault();
      primero.focus();
    }
  }
}
