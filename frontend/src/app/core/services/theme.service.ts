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
