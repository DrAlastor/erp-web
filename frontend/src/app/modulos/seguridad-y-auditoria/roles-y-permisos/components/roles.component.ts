import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';
import { RolesService } from '../services/roles.service';
import { Permiso, Rol } from '../models/roles.model';
import { SiPermisoDirective } from '../directives/si-permiso.directive';
import { PermisosService } from '../services/permisos.service';
import { AsignarRolComponent } from './asignar-rol.component';
import { ErrorApi } from '../models/permisos.model';

/**
 * Notación de la matriz del documento de la HU-03: C = crear, L = consultar,
 * M = modificar, A = anular.
 */
const LETRAS_DE_LA_MATRIZ: Record<string, string> = {
  CREAR: 'C',
  CONSULTAR: 'L',
  MODIFICAR: 'M',
  ANULAR: 'A',
};

/** Una fila de la matriz: un módulo con sus cuatro acciones. */
interface FilaDeMatriz {
  modulo: string;
  etiqueta: string;
  celdas: { codigo: string; accion: string }[];
}

/**
 * Pantalla de administración de Roles y Permisos (HU-03).
 *
 * La matriz se dibuja con la misma forma que la tabla del documento de la historia:
 * los módulos como filas y las cuatro acciones como columnas, con la notación C/L/M/A.
 * Así la configuración del sistema y el documento entregado se pueden comparar de un
 * vistazo.
 *
 * Los botones de guardar y de cambiar estado están detrás de `*siPermiso`, y el backend
 * vuelve a verificar el permiso en cada petición.
 */
@Component({
  selector: 'app-roles',
  imports: [SiPermisoDirective, AsignarRolComponent],
  templateUrl: './roles.component.html',
  styleUrl: './roles.component.scss',
})
export class RolesComponent {
  private readonly servicio = inject(RolesService);
  private readonly permisosDelUsuario = inject(PermisosService);

  readonly roles = signal<Rol[]>([]);
  readonly catalogo = signal<Permiso[]>([]);
  readonly seleccionado = signal<Rol | null>(null);
  readonly marcados = signal<ReadonlySet<string>>(new Set<string>());

  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly error = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);
  readonly asignando = signal(false);

  /** Solo puede editar quien tenga el permiso; si no, la matriz se ve pero no se toca. */
  readonly puedeEditar = computed(() => this.permisosDelUsuario.tiene('SEGURIDAD_MODIFICAR'));

  /**
   * Las cuatro acciones, en el orden de las columnas del documento, con la letra que la
   * matriz del documento usa para cada una.
   */
  readonly acciones = computed(() => {
    const vistas = new Map<string, string>();
    for (const permiso of this.catalogo()) {
      if (!vistas.has(permiso.accion)) {
        vistas.set(permiso.accion, permiso.etiquetaAccion);
      }
    }
    return [...vistas.entries()].map(([accion, etiqueta]) => ({
      accion,
      etiqueta,
      letra: LETRAS_DE_LA_MATRIZ[accion] ?? etiqueta.charAt(0),
    }));
  });

  /** Las seis filas de la matriz, una por módulo. */
  readonly filas = computed<FilaDeMatriz[]>(() => {
    const porModulo = new Map<string, FilaDeMatriz>();

    for (const permiso of this.catalogo()) {
      if (!porModulo.has(permiso.modulo)) {
        porModulo.set(permiso.modulo, {
          modulo: permiso.modulo,
          etiqueta: permiso.etiquetaModulo,
          celdas: [],
        });
      }
      porModulo.get(permiso.modulo)!.celdas.push({ codigo: permiso.codigo, accion: permiso.accion });
    }

    return [...porModulo.values()];
  });

  /** Cuántos permisos tiene marcados la matriz que se está editando. */
  readonly marcadosCuenta = computed(() => this.marcados().size);

  /** Si hay cambios sin guardar. */
  readonly hayCambios = computed(() => {
    const rol = this.seleccionado();
    if (!rol) {
      return false;
    }
    const actuales = this.marcados();
    return (
      actuales.size !== rol.permisos.length ||
      rol.permisos.some((codigo) => !actuales.has(codigo))
    );
  });

  constructor() {
    this.cargar();
  }

  private cargar(): void {
    this.cargando.set(true);
    forkJoin({
      roles: this.servicio.listarRoles(),
      catalogo: this.servicio.catalogoDePermisos(),
    }).subscribe({
      next: ({ roles, catalogo }) => {
        this.roles.set(roles);
        this.catalogo.set(catalogo);
        this.cargando.set(false);
        if (roles.length > 0) {
          this.seleccionar(roles[0]);
        }
      },
      error: (fallo: HttpErrorResponse) => {
        this.cargando.set(false);
        this.error.set(this.mensajeDe(fallo));
      },
    });
  }

  seleccionar(rol: Rol): void {
    this.seleccionado.set(rol);
    this.marcados.set(new Set(rol.permisos));
    this.aviso.set(null);
    this.error.set(null);
  }

  estaMarcado(codigo: string): boolean {
    return this.marcados().has(codigo);
  }

  alternar(codigo: string): void {
    if (!this.puedeEditar()) {
      return;
    }
    const copia = new Set(this.marcados());
    if (copia.has(codigo)) {
      copia.delete(codigo);
    } else {
      copia.add(codigo);
    }
    this.marcados.set(copia);
  }

  /** Marca o desmarca las cuatro acciones de un módulo de una vez. */
  alternarModulo(fila: FilaDeMatriz): void {
    if (!this.puedeEditar()) {
      return;
    }
    const copia = new Set(this.marcados());
    const completo = fila.celdas.every((celda) => copia.has(celda.codigo));
    for (const celda of fila.celdas) {
      if (completo) {
        copia.delete(celda.codigo);
      } else {
        copia.add(celda.codigo);
      }
    }
    this.marcados.set(copia);
  }

  moduloCompleto(fila: FilaDeMatriz): boolean {
    return fila.celdas.every((celda) => this.marcados().has(celda.codigo));
  }

  descartar(): void {
    const rol = this.seleccionado();
    if (rol) {
      this.marcados.set(new Set(rol.permisos));
      this.aviso.set(null);
      this.error.set(null);
    }
  }

  guardar(): void {
    const rol = this.seleccionado();
    if (!rol || this.guardando()) {
      return;
    }

    this.guardando.set(true);
    this.error.set(null);
    this.aviso.set(null);

    this.servicio.guardarMatriz(rol.id, [...this.marcados()]).subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        this.reemplazar(actualizado);
        this.aviso.set(
          `Matriz de ${actualizado.nombre} guardada con ${actualizado.permisos.length} permisos.`,
        );
        // Los permisos propios pueden haber cambiado con esta edición.
        this.permisosDelUsuario.cargar().subscribe({ error: () => undefined });
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando.set(false);
        this.error.set(this.mensajeDe(fallo));
      },
    });
  }

  cambiarEstado(): void {
    const rol = this.seleccionado();
    if (!rol || this.guardando()) {
      return;
    }

    this.guardando.set(true);
    this.error.set(null);
    this.aviso.set(null);

    this.servicio.cambiarEstado(rol.id, !rol.activo).subscribe({
      next: (actualizado) => {
        this.guardando.set(false);
        this.reemplazar(actualizado);
        this.aviso.set(
          actualizado.activo
            ? `${actualizado.nombre} quedó activo.`
            : `${actualizado.nombre} quedó desactivado: deja de otorgar permisos a quienes lo tengan.`,
        );
      },
      error: (fallo: HttpErrorResponse) => {
        this.guardando.set(false);
        this.error.set(this.mensajeDe(fallo));
      },
    });
  }

  abrirAsignacion(): void {
    this.asignando.set(true);
  }

  cerrarAsignacion(mensaje: string | null): void {
    this.asignando.set(false);
    if (mensaje) {
      this.aviso.set(mensaje);
    }
  }

  private reemplazar(actualizado: Rol): void {
    this.roles.update((lista) =>
      lista.map((rol) => (rol.id === actualizado.id ? actualizado : rol)),
    );
    this.seleccionado.set(actualizado);
    this.marcados.set(new Set(actualizado.permisos));
  }

  private mensajeDe(fallo: HttpErrorResponse): string {
    if (fallo.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
    const cuerpo = fallo.error as ErrorApi | null;
    return cuerpo?.mensaje ?? 'La operación no se pudo completar.';
  }
}
