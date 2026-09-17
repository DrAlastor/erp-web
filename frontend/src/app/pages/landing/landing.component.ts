import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { CompanyService } from '../../core/services/company.service';
import { ThemeService } from '../../core/services/theme.service';
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
  readonly themeService = inject(ThemeService);

  private readonly router = inject(Router);

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
    this.router.navigate(['/login']);
  }

  scrollToSection(id: string): void {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  }
}
