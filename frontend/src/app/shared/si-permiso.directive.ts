import {
  Directive,
  TemplateRef,
  ViewContainerRef,
  effect,
  inject,
  input,
} from '@angular/core';
import { PermisosService } from '../core/permisos/permisos.service';

/**
 * Muestra su contenido solo si el usuario tiene el permiso indicado. Es la tarea 7 de la
 * HU-03: ocultar las funcionalidades que el usuario no tiene permitido utilizar.
 *
 * Igual que los guards, esto es presentación: el backend vuelve a verificar el permiso.
 *
 * @example
 * <button *siPermiso="'SEGURIDAD_MODIFICAR'">Guardar matriz</button>
 */
@Directive({
  selector: '[siPermiso]',
})
export class SiPermisoDirective {
  private readonly permisos = inject(PermisosService);
  private readonly plantilla = inject(TemplateRef<unknown>);
  private readonly contenedor = inject(ViewContainerRef);

  readonly siPermiso = input.required<string>();

  private dibujado = false;

  constructor() {
    effect(() => {
      const permitido = this.permisos.permisos().has(this.siPermiso());

      if (permitido && !this.dibujado) {
        this.contenedor.createEmbeddedView(this.plantilla);
        this.dibujado = true;
      } else if (!permitido && this.dibujado) {
        this.contenedor.clear();
        this.dibujado = false;
      }
    });
  }
}
