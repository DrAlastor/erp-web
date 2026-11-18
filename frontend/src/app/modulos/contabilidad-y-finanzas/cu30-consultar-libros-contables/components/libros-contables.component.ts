import { CommonModule, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  CuentaContableSimple,
  FiltroDiario,
  FiltroMayor,
  LibroDiarioResponse,
  LibroMayorResponse,
  PeriodoContableSimple
} from '../models/libros-contables.model';
import { LibrosContablesService } from '../services/libros-contables.service';

@Component({
  selector: 'app-libros-contables',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './libros-contables.component.html',
  styleUrls: ['./libros-contables.component.scss']
})
export class LibrosContablesComponent implements OnInit {
  private readonly librosService = inject(LibrosContablesService);

  // Pestaña activa ('diario' | 'mayor')
  readonly pestanaActiva = signal<'diario' | 'mayor'>('diario');

  // Estados de carga
  readonly cargando = signal<boolean>(false);
  readonly errorMsg = signal<string | null>(null);

  // Listados de apoyo
  readonly periodos = signal<PeriodoContableSimple[]>([]);
  readonly cuentas = signal<CuentaContableSimple[]>([]);
  readonly tiposAsiento = signal<string[]>([]);

  // Filtros del Libro Diario
  filtroDiario: FiltroDiario = {
    periodoId: null,
    fechaInicio: null,
    fechaFin: null,
    tipoAsiento: 'TODOS',
    estado: 'MAYORIZADO',
    busqueda: ''
  };

  // Filtros del Libro Mayor
  filtroMayor: FiltroMayor = {
    cuentaId: null,
    periodoId: null,
    fechaInicio: null,
    fechaFin: null
  };

  // Datos cargados
  readonly datosDiario = signal<LibroDiarioResponse | null>(null);
  readonly datosMayor = signal<LibroMayorResponse | null>(null);

  ngOnInit(): void {
    this.cargarCatalogos();
  }

  cargarCatalogos(): void {
    this.librosService.listarPeriodos().subscribe({
      next: (data) => {
        this.periodos.set(data);
        if (data.length > 0) {
          // Seleccionar por defecto el periodo abierto más reciente
          const abierto = data.find(p => p.estado.toUpperCase() === 'ABIERTO') || data[0];
          this.filtroDiario.periodoId = abierto.id;
          this.filtroMayor.periodoId = abierto.id;
          this.filtroDiario.fechaInicio = abierto.fechaInicio;
          this.filtroDiario.fechaFin = abierto.fechaFin;
          this.filtroMayor.fechaInicio = abierto.fechaInicio;
          this.filtroMayor.fechaFin = abierto.fechaFin;
        }
        this.consultarDiario();
      },
      error: () => this.errorMsg.set('No se pudieron cargar los periodos contables.')
    });

    this.librosService.listarCuentas().subscribe({
      next: (data) => this.cuentas.set(data),
      error: () => console.error('Error al cargar cuentas contables')
    });

    this.librosService.listarTiposAsiento().subscribe({
      next: (data) => this.tiposAsiento.set(data),
      error: () => console.error('Error al cargar tipos de asiento')
    });
  }

  cambiarPestana(pestana: 'diario' | 'mayor'): void {
    this.pestanaActiva.set(pestana);
    this.errorMsg.set(null);
    if (pestana === 'diario' && !this.datosDiario()) {
      this.consultarDiario();
    } else if (pestana === 'mayor' && !this.datosMayor()) {
      this.consultarMayor();
    }
  }

  onPeriodoDiarioChange(): void {
    const p = this.periodos().find(x => x.id === Number(this.filtroDiario.periodoId));
    if (p) {
      this.filtroDiario.fechaInicio = p.fechaInicio;
      this.filtroDiario.fechaFin = p.fechaFin;
    } else {
      this.filtroDiario.fechaInicio = null;
      this.filtroDiario.fechaFin = null;
    }
  }

  onPeriodoMayorChange(): void {
    const p = this.periodos().find(x => x.id === Number(this.filtroMayor.periodoId));
    if (p) {
      this.filtroMayor.fechaInicio = p.fechaInicio;
      this.filtroMayor.fechaFin = p.fechaFin;
    } else {
      this.filtroMayor.fechaInicio = null;
      this.filtroMayor.fechaFin = null;
    }
  }

  verTodoElAnoDiario(): void {
    this.filtroDiario.periodoId = null;
    this.filtroDiario.fechaInicio = '2026-01-01';
    this.filtroDiario.fechaFin = '2026-12-31';
    this.consultarDiario();
  }

  verTodoElAnoMayor(): void {
    this.filtroMayor.periodoId = null;
    this.filtroMayor.fechaInicio = '2026-01-01';
    this.filtroMayor.fechaFin = '2026-12-31';
    this.consultarMayor();
  }

  consultarDiario(): void {
    this.cargando.set(true);
    this.errorMsg.set(null);

    this.librosService.consultarLibroDiario(this.filtroDiario).subscribe({
      next: (res) => {
        this.datosDiario.set(res);
        this.cargando.set(false);
      },
      error: (err) => {
        this.cargando.set(false);
        this.errorMsg.set(err?.error?.message || 'Error al consultar el Libro Diario.');
      }
    });
  }

  consultarMayor(): void {
    this.cargando.set(true);
    this.errorMsg.set(null);

    this.librosService.consultarLibroMayor(this.filtroMayor).subscribe({
      next: (res) => {
        this.datosMayor.set(res);
        this.cargando.set(false);
      },
      error: (err) => {
        this.cargando.set(false);
        this.errorMsg.set(err?.error?.message || 'Error al consultar el Libro Mayor.');
      }
    });
  }

  limpiarFiltrosDiario(): void {
    this.filtroDiario = {
      periodoId: null,
      fechaInicio: null,
      fechaFin: null,
      tipoAsiento: 'TODOS',
      estado: 'TODOS',
      busqueda: ''
    };
    this.consultarDiario();
  }

  limpiarFiltrosMayor(): void {
    this.filtroMayor = {
      cuentaId: null,
      periodoId: null,
      fechaInicio: null,
      fechaFin: null
    };
    this.consultarMayor();
  }

  exportarDiario(): void {
    this.librosService.exportarDiarioCsv(this.filtroDiario).subscribe({
      next: (blob) => this.descargarArchivo(blob, 'Libro_Diario.csv'),
      error: () => alert('Error al exportar el Libro Diario')
    });
  }

  exportarMayor(): void {
    this.librosService.exportarMayorCsv(this.filtroMayor).subscribe({
      next: (blob) => this.descargarArchivo(blob, 'Libro_Mayor.csv'),
      error: () => alert('Error al exportar el Libro Mayor')
    });
  }

  imprimir(): void {
    window.print();
  }

  private descargarArchivo(blob: Blob, nombre: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombre;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
