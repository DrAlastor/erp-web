import { Injectable, computed, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'erp_theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly theme = signal<Theme>(this.readStoredTheme());

  readonly isDark = computed(() => this.theme() === 'dark');

  constructor() {
    this.applyToDocument(this.theme());
  }

  toggle(): void {
    const next: Theme = this.theme() === 'dark' ? 'light' : 'dark';
    this.theme.set(next);
    this.applyToDocument(next);
    try {
      localStorage.setItem(STORAGE_KEY, next);
    } catch {
      // Almacenamiento no disponible; el tema sigue vivo en memoria para esta sesión.
    }
  }

  /**
   * Aplica el tema que impone la empresa (CU06) sin guardarlo como preferencia, para que
   * landing y login sigan con el tema que eligió la persona.
   */
  aplicarSinGuardar(theme: Theme): void {
    this.theme.set(theme);
    this.applyToDocument(theme);
  }

  /** Vuelve al tema guardado en el navegador; se usa al cerrar sesión. */
  restaurarPreferencia(): void {
    const stored = this.readStoredTheme();
    this.theme.set(stored);
    this.applyToDocument(stored);
  }

  private applyToDocument(theme: Theme): void {
    try {
      document.documentElement.classList.toggle('dark', theme === 'dark');
    } catch {
      // SSR: no hay `document` disponible durante el renderizado en servidor.
    }
  }

  private readStoredTheme(): Theme {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'dark' ? 'dark' : 'light';
    } catch {
      return 'light';
    }
  }
}
