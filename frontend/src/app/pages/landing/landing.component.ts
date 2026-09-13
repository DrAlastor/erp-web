import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CompanyService } from '../../core/services/company.service';
import { CompanyModalComponent } from '../../components/company-modal/company-modal.component';
import { Company } from '../../core/models/company.model';

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

  // Active Tab in Hero Simulation: 'comercial' | 'contable'
  activeSimulationTab = signal<'comercial' | 'contable'>('comercial');

  // Success Notification state
  notificationMessage = signal<string | null>(null);

  openCreateModal(): void {
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  onCompanyCreated(): void {
    this.showNotification('¡Empresa creada con éxito! Ahora está seleccionada como activa para gestionar sus productos.');
  }

  selectCompany(company: Company): void {
    this.companyService.selectCompany(company.id);
    this.showNotification(`Has seleccionado: ${company.name} como empresa activa.`);
  }

  showNotification(msg: string): void {
    this.notificationMessage.set(msg);
    setTimeout(() => {
      this.notificationMessage.set(null);
    }, 4500);
  }

  scrollToSection(id: string): void {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  }
}
