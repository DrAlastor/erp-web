import { Component, computed, inject } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { PermisosService } from '../../core/permisos/permisos.service';

/** Un permiso agrupado para mostrarlo: el módulo y las acciones que el usuario puede hacer. */
interface FilaDePermisos {
  modulo: string;
  acciones: string[];
}

const ETIQUETAS_DE_MODULO: Record<string, string> = {
  SEGURIDAD: 'Seguridad y auditoría',
  COMERCIAL: 'Comercial y ventas',
  INVENTARIO: 'Inventarios y compras',
  CONTABILIDAD: 'Contabilidad',
  FACTURACION: 'Facturación',
  REPORTES: 'Reportes',
};

const ETIQUETAS_DE_ACCION: Record<string, string> = {
  CREAR: 'Crear',
  CONSULTAR: 'Consultar',
  MODIFICAR: 'Modificar',
  ANULAR: 'Anular',
};

const ORDEN_DE_MODULOS = Object.keys(ETIQUETAS_DE_MODULO);
const ORDEN_DE_ACCIONES = Object.keys(ETIQUETAS_DE_ACCION);

/**
 * Primera pantalla del área logueada: le muestra al usuario exactamente qué puede hacer.
 *
 * Sirve para el día a día y también para la demostración de la HU-03: entrando con dos
 * usuarios distintos se ve, sin abrir el código, cómo cambia lo que el sistema le permite a
 * cada uno.
 */
@Component({
  selector: 'app-inicio',
  templateUrl: './inicio.component.html',
  styleUrl: './inicio.component.scss',
})
export class InicioComponent {
  private readonly auth = inject(AuthService);
  private readonly permisosService = inject(PermisosService);

  readonly nombre = this.auth.nombre;
  readonly roles = this.permisosService.roles;
  readonly cantidad = this.permisosService.cantidad;

  /** Los permisos del usuario, agrupados por módulo y en el orden de la matriz. */
  readonly filas = computed<FilaDePermisos[]>(() => {
    const permisos = this.permisosService.permisos();
    const porModulo = new Map<string, string[]>();

    for (const codigo of permisos) {
      const separador = codigo.lastIndexOf('_');
      const modulo = codigo.slice(0, separador);
      const accion = codigo.slice(separador + 1);
      porModulo.set(modulo, [...(porModulo.get(modulo) ?? []), accion]);
    }

    return ORDEN_DE_MODULOS.filter((modulo) => porModulo.has(modulo)).map((modulo) => ({
      modulo: ETIQUETAS_DE_MODULO[modulo] ?? modulo,
      acciones: ORDEN_DE_ACCIONES.filter((accion) => porModulo.get(modulo)!.includes(accion)).map(
        (accion) => ETIQUETAS_DE_ACCION[accion] ?? accion,
      ),
    }));
  });
}
