import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { JsonPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AgenteService } from '../services/agente.service';
import {
  ConversacionResumen, EstadoAgente, GraficoDatos, GraficoGuardado, HerramientaUsada, MensajeChat,
} from '../models/agente.model';
import { graficosDeRespuesta } from '../utils/graficos';
import { TextoAsistenteComponent } from './texto-asistente.component';
import { GraficoBiComponent } from './grafico-bi.component';

/**
 * CU41 — Asistente de IA (chat) de Inteligencia de Negocio. Envío optimista con reintento,
 * historial de conversaciones, gráficos por respuesta (máx. 2), "Cómo se calculó" y gráficos guardados.
 */
@Component({
  selector: 'app-asistente-ia',
  standalone: true,
  imports: [FormsModule, JsonPipe, TextoAsistenteComponent, GraficoBiComponent],
  templateUrl: './asistente-ia.component.html',
  styleUrl: './asistente-ia.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AsistenteIaComponent {
  private readonly api = inject(AgenteService);
  private readonly destroy = inject(DestroyRef);
  private readonly fin = viewChild<ElementRef<HTMLElement>>('fin');

  readonly estado = signal<EstadoAgente | null>(null);
  readonly conversaciones = signal<ConversacionResumen[]>([]);
  readonly graficos = signal<GraficoGuardado[]>([]);
  readonly idConversacion = signal<number | null>(null);
  readonly mensajes = signal<MensajeChat[]>([]);
  readonly enviando = signal(false);
  readonly error = signal('');
  readonly graficoAbierto = signal<GraficoDatos | null>(null);
  readonly cargandoGrafico = signal(false);
  readonly guardados = signal(new Set<string>());

  pregunta = '';

  readonly sugerencias = [
    '¿Cuánto vendí este mes y cómo va frente al mes pasado?',
    '¿Por qué bajaron las ventas en octubre frente a septiembre?',
    '¿Qué productos se venden menos y cuáles debería liquidar?',
    '¿Cuánto voy a vender los próximos 6 meses?',
    '¿Qué clientes están en riesgo de dejar de comprar?',
    '¿Qué productos se compran juntos para armar combos?',
  ];

  readonly graficosDe = graficosDeRespuesta;

  constructor() {
    this.api.estado().pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (e) => this.estado.set(e),
      error: (e: HttpErrorResponse) => this.error.set(this.mensajeError(e)),
    });
    this.cargarListas();
  }

  cargarListas(): void {
    this.api.conversaciones().pipe(takeUntilDestroyed(this.destroy)).subscribe({ next: (c) => this.conversaciones.set(c), error: () => {} });
    this.api.graficos().pipe(takeUntilDestroyed(this.destroy)).subscribe({ next: (g) => this.graficos.set(g), error: () => {} });
  }

  // ------------------------------------------------------------------ chat

  enviar(texto?: string): void {
    const pregunta = (texto ?? this.pregunta).trim();
    if (!pregunta || this.enviando()) return;
    this.error.set('');
    this.pregunta = '';
    this.enviando.set(true);
    // Envío optimista: la pregunta aparece de inmediato.
    this.mensajes.update((m) => [...m,
      { rol: 'USUARIO', texto: pregunta, herramientas: [] },
      { rol: 'ASISTENTE', texto: '', herramientas: [], pendiente: true }]);
    this.bajar();
    this.api.preguntar(pregunta, this.idConversacion()).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (r) => {
        this.idConversacion.set(r.idConversacion);
        this.mensajes.update((m) => [...m.slice(0, -1),
          { rol: 'ASISTENTE', texto: r.texto, herramientas: r.herramientas, tokens: r.tokens }]);
        this.enviando.set(false);
        this.estado.update((e) => (e ? { ...e, preguntasHoy: e.preguntasHoy + 1 } : e));
        this.cargarListas();
        this.bajar();
      },
      error: (e: HttpErrorResponse) => {
        // Si falla, se quita del chat y vuelve al cuadro de texto para reintentar.
        this.mensajes.update((m) => m.slice(0, -2));
        this.pregunta = pregunta;
        this.enviando.set(false);
        this.error.set(this.mensajeError(e));
      },
    });
  }

  alPresionar(ev: KeyboardEvent): void {
    if (ev.key === 'Enter' && !ev.shiftKey) {
      ev.preventDefault();
      this.enviar();
    }
  }

  nueva(): void {
    this.idConversacion.set(null);
    this.mensajes.set([]);
    this.error.set('');
    this.graficoAbierto.set(null);
  }

  abrir(c: ConversacionResumen): void {
    this.graficoAbierto.set(null);
    this.api.conversacion(c.id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (d) => {
        this.idConversacion.set(d.id);
        this.mensajes.set(d.mensajes.map((m) => ({ rol: m.rol, texto: m.contenido, herramientas: m.herramientas ?? [] })));
        this.error.set('');
        this.bajar();
      },
      error: (e: HttpErrorResponse) => this.error.set(this.mensajeError(e)),
    });
  }

  borrar(c: ConversacionResumen, ev: Event): void {
    ev.stopPropagation();
    if (!confirm(`¿Borrar la conversación "${c.titulo}"?`)) return;
    this.api.borrarConversacion(c.id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: () => {
        if (this.idConversacion() === c.id) this.nueva();
        this.conversaciones.update((l) => l.filter((x) => x.id !== c.id));
      },
      error: (e: HttpErrorResponse) => this.error.set(this.mensajeError(e)),
    });
  }

  // ------------------------------------------------------------------ gráficos

  claveGrafico(i: number, h: HerramientaUsada): string {
    return `${i}:${h.nombre}:${JSON.stringify(h.parametros)}`;
  }

  guardarGrafico(i: number, h: HerramientaUsada): void {
    this.api.guardarGrafico(h.nombre, h.parametros, h.grafico?.titulo ?? null).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (g) => {
        this.guardados.update((s) => new Set(s).add(this.claveGrafico(i, h)));
        this.graficos.update((l) => [g, ...l]);
      },
      error: (e: HttpErrorResponse) => this.error.set(this.mensajeError(e)),
    });
  }

  abrirGrafico(g: GraficoGuardado): void {
    this.cargandoGrafico.set(true);
    this.api.datosGrafico(g.id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: (d) => {
        this.graficoAbierto.set(d);
        this.cargandoGrafico.set(false);
      },
      error: (e: HttpErrorResponse) => {
        this.cargandoGrafico.set(false);
        this.error.set(this.mensajeError(e));
      },
    });
  }

  borrarGrafico(g: GraficoGuardado, ev: Event): void {
    ev.stopPropagation();
    if (!confirm(`¿Borrar el gráfico "${g.titulo}"?`)) return;
    this.api.borrarGrafico(g.id).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: () => {
        this.graficos.update((l) => l.filter((x) => x.id !== g.id));
        if (this.graficoAbierto()?.grafico.id === g.id) this.graficoAbierto.set(null);
      },
      error: (e: HttpErrorResponse) => this.error.set(this.mensajeError(e)),
    });
  }

  // ------------------------------------------------------------------ util

  parametrosTexto(p: Record<string, unknown>): string {
    const pares = Object.entries(p ?? {});
    return pares.length ? pares.map(([k, v]) => `${k}: ${v}`).join(' · ') : 'sin parámetros';
  }

  fecha(iso: string): string {
    return new Date(iso).toLocaleDateString('es-BO', { day: '2-digit', month: 'short' });
  }

  private bajar(): void {
    setTimeout(() => this.fin()?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'end' }), 30);
  }

  mensajeError(e: HttpErrorResponse): string {
    if (e.status === 0) return 'No se pudo conectar con el servidor.';
    if (e.status === 403) return 'No tienes permiso para usar el asistente (REPORTES / CONSULTAR).';
    const m = (e.error as { message?: string } | null)?.message;
    return m || `Error ${e.status} del asistente.`;
  }
}
