import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CompanyService } from '../../core/services/company.service';
import { CompanyModalComponent } from '../../components/company-modal/company-modal.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, CompanyModalComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent {
  readonly companyService = inject(CompanyService);

  // Modal state
  isCreateModalOpen = signal(false);

  // Success Notification state
  notificationMessage = signal<string | null>(null);

  openCreateModal(): void {
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  onCompanyCreated(): void {
    this.showNotification('¡Empresa registrada con éxito! Su espacio de datos está activo.');
  }

  showNotification(msg: string): void {
    this.notificationMessage.set(msg);
    setTimeout(() => {
      this.notificationMessage.set(null);
    }, 4500);
  }

  onLoginClick(): void {
    this.showNotification('Módulo de autenticación (CU-01): El inicio de sesión seguro estará disponible en el despliegue.');
  }

  scrollToSection(id: string): void {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  }
}
