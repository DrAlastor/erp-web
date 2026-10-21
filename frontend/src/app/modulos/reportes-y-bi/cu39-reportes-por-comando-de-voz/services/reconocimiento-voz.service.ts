import { DestroyRef, Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/** Lo mínimo de la Web Speech API que usa la pantalla (no está en los tipos de TypeScript). */
interface ResultadoVoz {
  readonly isFinal: boolean;
  readonly 0: { transcript: string; confidence: number };
}

interface EventoResultadoVoz {
  readonly resultIndex: number;
  readonly results: ArrayLike<ResultadoVoz>;
}

interface ReconocedorVoz {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  maxAlternatives: number;
  onstart: (() => void) | null;
  onresult: ((evento: EventoResultadoVoz) => void) | null;
  onerror: ((evento: { error: string }) => void) | null;
  onend: (() => void) | null;
  start(): void;
  stop(): void;
  abort(): void;
}

type ConstructorReconocedor = new () => ReconocedorVoz;

const CONFIANZA_MINIMA = 0.5;

/**
 * HU39.1 - Captura y transcripción de audio. Usa el servicio Speech-to-Text del navegador
 * (Web Speech API), que envía el audio al motor de reconocimiento y devuelve el texto en
 * español. Se provee por componente para que la grabación se corte al salir de la pantalla.
 */
@Injectable()
export class ReconocimientoVozService {
  private readonly esNavegador = isPlatformBrowser(inject(PLATFORM_ID));
  private reconocedor: ReconocedorVoz | null = null;
  private textoFinal = '';
  private huboError = false;

  /** El navegador ofrece reconocimiento de voz (Chrome, Edge, Safari). */
  readonly soportado = signal(this.esNavegador && !!this.constructorReconocedor());
  readonly grabando = signal(false);
  /** Texto reconocido hasta el momento, incluido lo que aún no es definitivo. */
  readonly transcripcion = signal('');
  readonly aviso = signal('');

  constructor() {
    inject(DestroyRef).onDestroy(() => this.reconocedor?.abort());
  }

  iniciar(): void {
    const Reconocedor = this.constructorReconocedor();
    if (!Reconocedor) {
      this.aviso.set('Tu navegador no permite dictar por voz. Usa Chrome o Edge, o escribe la consulta.');
      return;
    }
    this.reconocedor?.abort();
    this.textoFinal = '';
    this.huboError = false;
    this.transcripcion.set('');
    this.aviso.set('');

    const r = new Reconocedor();
    r.lang = 'es-BO';
    r.continuous = false;
    r.interimResults = true;
    r.maxAlternatives = 1;
    r.onstart = () => this.grabando.set(true);
    r.onresult = (evento) => this.alRecibir(evento);
    r.onerror = (evento) => this.alFallar(evento.error);
    r.onend = () => {
      this.grabando.set(false);
      this.transcripcion.set(this.textoFinal.trim());
      if (!this.huboError && !this.textoFinal.trim()) {
        this.aviso.set('No se detectó voz. Acércate al micrófono y vuelve a intentarlo.');
      }
    };
    this.reconocedor = r;
    try {
      r.start();
    } catch {
      this.grabando.set(false);
      this.aviso.set('No se pudo iniciar el micrófono. Vuelve a intentarlo.');
    }
  }

  /** Termina la grabación; el texto definitivo llega en el evento de fin. */
  detener(): void {
    this.reconocedor?.stop();
  }

  private alRecibir(evento: EventoResultadoVoz): void {
    let provisional = '';
    for (let i = evento.resultIndex; i < evento.results.length; i++) {
      const resultado = evento.results[i];
      const alternativa = resultado[0];
      if (resultado.isFinal) {
        this.textoFinal += alternativa.transcript + ' ';
        // Algunos navegadores informan confianza 0 cuando no la calculan: solo se avisa si la dan.
        if (alternativa.confidence > 0 && alternativa.confidence < CONFIANZA_MINIMA) {
          this.aviso.set('La calidad del audio fue baja. Revisa la transcripción antes de generar el reporte.');
        }
      } else {
        provisional += alternativa.transcript;
      }
    }
    this.transcripcion.set((this.textoFinal + provisional).trim());
  }

  private alFallar(codigo: string): void {
    if (codigo === 'aborted') return;
    this.huboError = true;
    const mensajes: Record<string, string> = {
      'no-speech': 'No se detectó voz. Acércate al micrófono y vuelve a intentarlo.',
      'audio-capture': 'No se encontró un micrófono disponible en este equipo.',
      'not-allowed': 'Permite el acceso al micrófono en el navegador para dictar la consulta.',
      'service-not-allowed': 'Permite el acceso al micrófono en el navegador para dictar la consulta.',
      network: 'No se pudo conectar con el servicio de transcripción. Revisa tu conexión a internet.',
      'language-not-supported': 'El servicio de transcripción no admite español en este navegador.'
    };
    this.aviso.set(mensajes[codigo] ?? 'No se pudo transcribir el audio. Vuelve a intentarlo.');
  }

  private constructorReconocedor(): ConstructorReconocedor | null {
    if (!this.esNavegador) return null;
    const w = window as unknown as {
      SpeechRecognition?: ConstructorReconocedor;
      webkitSpeechRecognition?: ConstructorReconocedor;
    };
    return w.SpeechRecognition ?? w.webkitSpeechRecognition ?? null;
  }
}
