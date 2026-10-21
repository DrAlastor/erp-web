import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, model, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, startWith, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CuentasPorPagarService } from '../services/cuentas-por-pagar.service';
import { CompraSinCuentaResponse } from '../models/cuenta-por-pagar.model';
import { mensajeError } from '../utils/cxp.utils';

/** Buscador de compras a crédito sin cuenta por pagar (máx. 20 resultados del backend). */
@Component({
  selector: 'app-compra-selector',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './compra-selector.component.html',
  styleUrl: './compra-selector.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompraSelectorComponent {
  private readonly api = inject(CuentasPorPagarService);
  private readonly busqueda$ = new Subject<string>();

  readonly compra = model<CompraSinCuentaResponse | null>(null);
  readonly disabled = input(false);
  readonly invalid = input(false);

  readonly resultados = signal<CompraSinCuentaResponse[]>([]);
  readonly buscando = signal(true);
  readonly error = signal('');

  constructor() {
    this.busqueda$
      .pipe(
        startWith(''),
        debounceTime(250),
        distinctUntilChanged(),
        switchMap((texto) => {
          this.buscando.set(true);
          this.error.set('');
          return this.api.comprasSinCuenta(texto).pipe(
            catchError((err: HttpErrorResponse) => {
              this.error.set(mensajeError(err));
              return of([] as CompraSinCuentaResponse[]);
            }),
          );
        }),
        takeUntilDestroyed(inject(DestroyRef)),
      )
      .subscribe((lista) => {
        this.resultados.set(lista);
        this.buscando.set(false);
      });
  }

  buscar(texto: string): void {
    this.busqueda$.next(texto.trim());
  }
}
