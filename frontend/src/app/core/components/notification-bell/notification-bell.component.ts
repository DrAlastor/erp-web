import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificacionesService, NotificacionERP } from '../../services/notificaciones.service';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-bell.component.html',
  styleUrl: './notification-bell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NotificationBellComponent {
  private readonly notificacionesService = inject(NotificacionesService);
  private readonly router = inject(Router);
  private readonly elementRef = inject(ElementRef);

  readonly isOpen = signal(false);
  readonly filtroActivo = signal<'todas' | 'no-leidas' | 'ventas' | 'inventario'>('todas');

  readonly notificaciones = this.notificacionesService.notificaciones;
  readonly conteoNoLeidas = this.notificacionesService.conteoNoLeidas;

  readonly notificacionesFiltradas = computed(() => {
    const lista = this.notificaciones();
    const filtro = this.filtroActivo();

    if (filtro === 'no-leidas') {
      return lista.filter(n => !n.leida);
    }
    if (filtro === 'ventas') {
      return lista.filter(n => n.categoria === 'ventas');
    }
    if (filtro === 'inventario') {
      return lista.filter(n => n.categoria === 'inventario');
    }
    return lista;
  });

  toggleOpen(): void {
    const next = !this.isOpen();
    this.isOpen.set(next);
    if (next) {
      this.notificacionesService.cargarNotificaciones(true);
    }
  }

  setFiltro(filtro: 'todas' | 'no-leidas' | 'ventas' | 'inventario'): void {
    this.filtroActivo.set(filtro);
  }

  marcarTodas(): void {
    this.notificacionesService.marcarTodasComoLeidas();
  }

  clickNotificacion(notif: NotificacionERP): void {
    this.notificacionesService.marcarComoLeida(notif.id);
    this.isOpen.set(false);

    if (notif.moduloId && notif.funcionId) {
      this.router.navigate(['/app'], {
        queryParams: {
          modulo: notif.moduloId,
          funcion: notif.funcionId
        }
      });
    }
  }

  descartar(event: MouseEvent, id: string): void {
    event.stopPropagation();
    this.notificacionesService.eliminarNotificacion(id);
  }

  formatearTiempo(fecha: Date): string {
    const ahora = Date.now();
    const difMs = ahora - new Date(fecha).getTime();
    const difMin = Math.floor(difMs / (1000 * 60));
    if (difMin < 1) return 'Hace un momento';
    if (difMin < 60) return `Hace ${difMin} min`;
    const difHoras = Math.floor(difMin / 60);
    if (difHoras < 24) return `Hace ${difHoras} h`;
    const difDias = Math.floor(difHoras / 24);
    return `Hace ${difDias} d`;
  }

  getIconoCategoria(categoria: string): string {
    switch (categoria) {
      case 'ventas': return '↗';
      case 'inventario': return '▦';
      case 'seguridad': return '◎';
      default: return '✦';
    }
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: MouseEvent): void {
    if (!this.elementRef.nativeElement.contains(event.target)) {
      this.isOpen.set(false);
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.isOpen.set(false);
  }
}
