import { DestroyRef, Injectable, PLATFORM_ID, inject, signal } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

/**
 * Lee en voz alta las respuestas del asistente con la síntesis de voz del navegador
 * (speechSynthesis): no necesita servicios externos ni API keys. Se provee por componente
 * para que la lectura se corte al salir de la pantalla.
 */
@Injectable()
export class SintesisVozService {
  private readonly esNavegador = isPlatformBrowser(inject(PLATFORM_ID));

  readonly soportado = signal(this.esNavegador && 'speechSynthesis' in window);
  readonly hablando = signal(false);

  constructor() {
    inject(DestroyRef).onDestroy(() => this.detener());
  }

  hablar(texto: string): void {
    if (!this.soportado() || !texto.trim()) return;
    const sintesis = window.speechSynthesis;
    sintesis.cancel();
    const frase = new SpeechSynthesisUtterance(texto);
    frase.lang = 'es-BO';
    const voz = this.vozEspanol(sintesis.getVoices());
    if (voz) frase.voice = voz;
    frase.rate = 1.05;
    frase.onstart = () => this.hablando.set(true);
    frase.onend = () => this.hablando.set(false);
    frase.onerror = () => this.hablando.set(false);
    sintesis.speak(frase);
  }

  detener(): void {
    if (!this.soportado()) return;
    window.speechSynthesis.cancel();
    this.hablando.set(false);
  }

  /** Prefiere una voz de Latinoamérica; si no hay, cualquier voz en español. */
  private vozEspanol(voces: SpeechSynthesisVoice[]): SpeechSynthesisVoice | null {
    const espanol = voces.filter(v => v.lang.toLowerCase().startsWith('es'));
    return espanol.find(v => /es-(bo|419|mx|us|ar|co|pe|cl)/i.test(v.lang))
      ?? espanol.find(v => v.lang.toLowerCase() === 'es-es')
      ?? espanol[0]
      ?? null;
  }
}
