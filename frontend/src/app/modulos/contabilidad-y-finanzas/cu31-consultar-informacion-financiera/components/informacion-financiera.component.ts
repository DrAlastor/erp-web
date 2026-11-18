import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InformacionFinancieraService } from '../services/informacion-financiera.service';
import {
  BalanceGeneralResponse,
  EstadoResultadosResponse,
  FiltroBalance,
  FiltroResultados,
  NodoFinanciero
} from '../models/informacion-financiera.model';

@Component({
  selector: 'app-informacion-financiera',
  standalone: true,
  imports: [CommonModule, FormsModule, DecimalPipe],
  templateUrl: './informacion-financiera.component.html',
  styleUrls: ['./informacion-financiera.component.scss']
})
export class InformacionFinancieraComponent implements OnInit {
  private readonly financieraService = inject(InformacionFinancieraService);

  tabActiva: 'balance' | 'resultados' = 'balance';

  gestiones: number[] = [];
  meses = [
    { valor: 1, nombre: 'Enero' },
    { valor: 2, nombre: 'Febrero' },
    { valor: 3, nombre: 'Marzo' },
    { valor: 4, nombre: 'Abril' },
    { valor: 5, nombre: 'Mayo' },
    { valor: 6, nombre: 'Junio' },
    { valor: 7, nombre: 'Julio' },
    { valor: 8, nombre: 'Agosto' },
    { valor: 9, nombre: 'Septiembre' },
    { valor: 10, nombre: 'Octubre' },
    { valor: 11, nombre: 'Noviembre' },
    { valor: 12, nombre: 'Diciembre' }
  ];

  filtroBalance: FiltroBalance = {
    gestion: null,
    mesCorte: null,
    fechaCorte: null,
    nivel: 4
  };

  filtroResultados: FiltroResultados = {
    gestion: null,
    fechaInicio: null,
    fechaFin: null,
    nivel: 4
  };

  cargando = false;
  errorMensaje: string | null = null;

  balanceData: BalanceGeneralResponse | null = null;
  resultadosData: EstadoResultadosResponse | null = null;

  ngOnInit(): void {
    this.cargarGestiones();
  }

  cargarGestiones(): void {
    this.financieraService.listarGestiones().subscribe({
      next: (gestiones) => {
        this.gestiones = gestiones;
        const anioActual = new Date().getFullYear();
        const seleccion = gestiones.includes(anioActual) ? anioActual : gestiones[0] || anioActual;

        this.filtroBalance.gestion = seleccion;
        this.filtroResultados.gestion = seleccion;
        this.consultarBalance();
      },
      error: (err) => {
        console.error('Error al listar gestiones:', err);
        this.consultarBalance();
      }
    });
  }

  cambiarTab(tab: 'balance' | 'resultados'): void {
    this.tabActiva = tab;
    this.errorMensaje = null;
    if (tab === 'balance' && !this.balanceData) {
      this.consultarBalance();
    } else if (tab === 'resultados' && !this.resultadosData) {
      this.consultarResultados();
    }
  }

  consultarBalance(): void {
    this.cargando = true;
    this.errorMensaje = null;

    this.financieraService.consultarBalanceGeneral(this.filtroBalance).subscribe({
      next: (data) => {
        this.balanceData = data;
        this.cargando = false;
      },
      error: (err) => {
        this.cargando = false;
        this.errorMensaje = err.error?.message || 'No fue posible generar el Balance General.';
      }
    });
  }

  consultarResultados(): void {
    this.cargando = true;
    this.errorMensaje = null;

    this.financieraService.consultarEstadoResultados(this.filtroResultados).subscribe({
      next: (data) => {
        this.resultadosData = data;
        this.cargando = false;
      },
      error: (err) => {
        this.cargando = false;
        this.errorMensaje = err.error?.message || 'No fue posible generar el Estado de Resultados.';
      }
    });
  }

  exportarBalanceCsv(): void {
    this.financieraService.exportarBalanceCsv(this.filtroBalance).subscribe({
      next: (blob) => this.descargarBlob(blob, `Balance_General_${this.filtroBalance.gestion || 'Actual'}.csv`),
      error: (err) => alert('Error al exportar Balance General en CSV.')
    });
  }

  exportarResultadosCsv(): void {
    this.financieraService.exportarEstadoResultadosCsv(this.filtroResultados).subscribe({
      next: (blob) => this.descargarBlob(blob, `Estado_Resultados_${this.filtroResultados.gestion || 'Actual'}.csv`),
      error: (err) => alert('Error al exportar Estado de Resultados en CSV.')
    });
  }

  imprimir(): void {
    window.print();
  }

  private descargarBlob(blob: Blob, nombreArchivo: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombreArchivo;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
