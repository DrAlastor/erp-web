import { Component, DestroyRef, OnInit, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { PermisosService } from '../../cu03-gestionar-roles-y-permisos/services/permisos.service';
import { AparienciaService } from '../services/apariencia.service';
import {
  ActualizarApariencia,
  Apariencia,
  ESTILOS_BORDES,
  EstiloBordes,
  ErrorRespuesta,
  PALETAS_PREDEFINIDAS,
  Paleta,
  TAMANOS_FUENTE,
  TAMANO_MAXIMO_LOGO,
  TIPOGRAFIAS,
  TIPOS_LOGO,
  TamanoFuente,
  TemaApariencia,
  Tipografia,
} from '../models/apariencia.model';
import { esHexValido, textoSobre } from '../utils/colores';

const HEX = /^#[0-9A-Fa-f]{6}$/;

@Component({
  selector: 'app-personalizar-apariencia',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './personalizar-apariencia.component.html',
  styleUrl: './personalizar-apariencia.component.scss',
})
export class PersonalizarAparienciaComponent implements OnInit {
  private readonly servicio = inject(AparienciaService);
  private readonly permisos = inject(PermisosService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly paletas = PALETAS_PREDEFINIDAS;
  readonly tamanos = TAMANOS_FUENTE;
  readonly tipografias = TIPOGRAFIAS;
  readonly estilosBordes = ESTILOS_BORDES;
  readonly tiposLogo = TIPOS_LOGO.join(',');

  readonly apariencia = this.servicio.apariencia;
  readonly logoUrl = this.servicio.logoUrl;
  readonly puedeEditar = computed(() => this.permisos.tiene('SEGURIDAD_MODIFICAR'));

  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly subiendoLogo = signal(false);
  readonly confirmandoRestablecer = signal(false);
  readonly error = signal<string | null>(null);
  readonly aviso = signal<string | null>(null);

  readonly formulario = this.fb.nonNullable.group({
    tema: this.fb.nonNullable.control<TemaApariencia>('CLARO'),
    colorPrimario: ['#007AFF', [Validators.required, Validators.pattern(HEX)]],
    colorSecundario: ['#1D1D1F', [Validators.required, Validators.pattern(HEX)]],
    colorAcento: ['#34C759', [Validators.required, Validators.pattern(HEX)]],
    tamanoFuente: this.fb.nonNullable.control<TamanoFuente>('NORMAL'),
    tipografia: this.fb.nonNullable.control<Tipografia>('INTER'),
    bordes: this.fb.nonNullable.control<EstiloBordes>('SUAVES'),
    encabezadoFactura: ['', Validators.maxLength(500)],
    pieFactura: ['', Validators.maxLength(500)],
  });

  /** Valores del formulario, para la vista previa de la tarjeta y de la factura. */
  readonly vista = signal(this.formulario.getRawValue());
  readonly hayCambios = signal(false);
  readonly textoBotonPrimario = computed(() =>
    esHexValido(this.vista().colorPrimario) ? textoSobre(this.vista().colorPrimario) : '#FFFFFF',
  );

  constructor() {
    effect(() => {
      if (this.puedeEditar()) this.formulario.enable({ emitEvent: false });
      else this.formulario.disable({ emitEvent: false });
    });

    this.formulario.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      const valores = this.formulario.getRawValue();
      this.vista.set(valores);
      this.hayCambios.set(this.difiere(valores, this.apariencia()));
      if (this.formulario.valid) this.servicio.previsualizar({ ...this.apariencia(), ...this.normalizar(valores) });
    });

    // Al salir de la pantalla sin guardar, la vista previa no debe quedar aplicada.
    this.destroyRef.onDestroy(() => this.servicio.restaurar());
  }

  ngOnInit(): void {
    this.servicio.cargar().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (apariencia) => {
        this.llenarFormulario(apariencia);
        this.cargando.set(false);
      },
      error: (err) => {
        this.llenarFormulario(this.apariencia());
        this.error.set(this.mensajeDe(err, 'No se pudo cargar la apariencia de la empresa.'));
        this.cargando.set(false);
      },
    });
  }

  aplicarPaleta(paleta: Paleta): void {
    this.formulario.patchValue({
      colorPrimario: paleta.primario,
      colorSecundario: paleta.secundario,
      colorAcento: paleta.acento,
    });
  }

  paletaActiva(paleta: Paleta): boolean {
    const v = this.vista();
    return [v.colorPrimario, v.colorSecundario, v.colorAcento].join().toUpperCase()
      === [paleta.primario, paleta.secundario, paleta.acento].join().toUpperCase();
  }

  guardar(): void {
    if (!this.puedeEditar() || this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }
    this.limpiarMensajes();
    this.guardando.set(true);
    this.servicio.guardar(this.normalizar(this.formulario.getRawValue())).subscribe({
      next: (apariencia) => {
        this.llenarFormulario(apariencia);
        this.guardando.set(false);
        this.aviso.set('La apariencia se guardó. Todos los usuarios de la empresa la verán al entrar.');
      },
      error: (err) => {
        this.guardando.set(false);
        this.error.set(this.mensajeDe(err, 'No se pudo guardar la apariencia.'));
      },
    });
  }

  cancelar(): void {
    this.limpiarMensajes();
    this.llenarFormulario(this.apariencia());
    this.servicio.restaurar();
  }

  restablecer(): void {
    if (!this.puedeEditar()) return;
    this.confirmandoRestablecer.set(false);
    this.limpiarMensajes();
    this.guardando.set(true);
    this.servicio.restablecer().subscribe({
      next: (apariencia) => {
        this.llenarFormulario(apariencia);
        this.guardando.set(false);
        this.aviso.set('Se restableció la marca NexoERP. El encabezado y el pie de factura se conservaron.');
      },
      error: (err) => {
        this.guardando.set(false);
        this.error.set(this.mensajeDe(err, 'No se pudo restablecer la apariencia.'));
      },
    });
  }

  elegirLogo(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    entrada.value = '';
    if (!archivo || !this.puedeEditar()) return;

    this.limpiarMensajes();
    const problema = this.validarLogo(archivo);
    if (problema) {
      this.error.set(problema);
      return;
    }
    this.subiendoLogo.set(true);
    this.servicio.subirLogo(archivo).subscribe({
      next: () => {
        this.subiendoLogo.set(false);
        this.aviso.set('Logo actualizado.');
      },
      error: (err) => {
        this.subiendoLogo.set(false);
        this.error.set(this.mensajeDe(err, 'No se pudo subir el logo.'));
      },
    });
  }

  quitarLogo(): void {
    if (!this.puedeEditar()) return;
    this.limpiarMensajes();
    this.subiendoLogo.set(true);
    this.servicio.eliminarLogo().subscribe({
      next: () => {
        this.subiendoLogo.set(false);
        this.aviso.set('Se quitó el logo; se muestra el de NexoERP.');
      },
      error: (err) => {
        this.subiendoLogo.set(false);
        this.error.set(this.mensajeDe(err, 'No se pudo quitar el logo.'));
      },
    });
  }

  /** Validación previa al envío; el backend vuelve a comprobar el contenido del archivo. */
  validarLogo(archivo: File): string | null {
    if (!(TIPOS_LOGO as readonly string[]).includes(archivo.type)) {
      return 'El logo debe ser una imagen PNG, JPG, WEBP o SVG.';
    }
    if (archivo.size > TAMANO_MAXIMO_LOGO) {
      return 'El logo no puede superar los 512 KB.';
    }
    return null;
  }

  invalido(campo: keyof typeof this.formulario.controls): boolean {
    const control = this.formulario.controls[campo];
    return control.invalid && (control.dirty || control.touched);
  }

  private llenarFormulario(a: Apariencia): void {
    this.formulario.reset({
      tema: a.tema,
      colorPrimario: a.colorPrimario,
      colorSecundario: a.colorSecundario,
      colorAcento: a.colorAcento,
      tamanoFuente: a.tamanoFuente,
      tipografia: a.tipografia,
      bordes: a.bordes,
      encabezadoFactura: a.encabezadoFactura ?? '',
      pieFactura: a.pieFactura ?? '',
    }, { emitEvent: false });
    this.vista.set(this.formulario.getRawValue());
    this.hayCambios.set(false);
  }

  private normalizar(v: ReturnType<typeof this.formulario.getRawValue>): ActualizarApariencia {
    return {
      ...v,
      colorPrimario: v.colorPrimario.toUpperCase(),
      colorSecundario: v.colorSecundario.toUpperCase(),
      colorAcento: v.colorAcento.toUpperCase(),
      encabezadoFactura: v.encabezadoFactura.trim() || null,
      pieFactura: v.pieFactura.trim() || null,
    };
  }

  private difiere(v: ReturnType<typeof this.formulario.getRawValue>, a: Apariencia): boolean {
    const n = this.normalizar(v);
    return (Object.keys(n) as (keyof ActualizarApariencia)[]).some((k) =>
      (n[k] ?? '').toString().toUpperCase() !== (a[k] ?? '').toString().toUpperCase());
  }

  private limpiarMensajes(): void {
    this.error.set(null);
    this.aviso.set(null);
  }

  private mensajeDe(err: unknown, porDefecto: string): string {
    if (err instanceof HttpErrorResponse) {
      if (err.status === 403) return 'No tienes permiso para modificar la apariencia de la empresa.';
      if (err.status === 413) return 'El logo no puede superar los 512 KB.';
      const cuerpo = err.error as Partial<ErrorRespuesta> | null;
      if (cuerpo?.message) return cuerpo.message;
    }
    return porDefecto;
  }
}
