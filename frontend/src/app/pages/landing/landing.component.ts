import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CompanyService } from '../../core/services/company.service';
import { CompanyModalComponent } from '../../components/company-modal/company-modal.component';
import { Company } from '../../core/models/company.model';

export interface TeamMember {
  id: string;
  name: string;
  role: string;
  description: string;
  initials: string;
  avatarGradient: string;
  tags: string[];
  githubUrl: string;
  linkedinUrl: string;
  portfolioUrl: string;
  instagramUrl: string;
  whatsappUrl: string;
}

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

  // Equipo de Desarrollo (6 integrantes del equipo de SI2 - UAGRM)
  readonly teamMembers = signal<TeamMember[]>([
    {
      id: 'dev-1',
      name: 'Arteaga Silva Geimbert Santiago',
      role: 'Desarrollador Full Stack',
      description: 'Ingeniería y desarrollo integral del ERP: componentes frontend Angular, microservicios backend Spring Boot y persistencia.',
      initials: 'GS',
      avatarGradient: 'linear-gradient(135deg, #5b4bf6 0%, #7c3aed 100%)',
      tags: ['Full Stack', 'Angular', 'Spring Boot', 'PostgreSQL', 'Docker'],
      githubUrl: 'https://github.com',
      linkedinUrl: 'https://linkedin.com',
      portfolioUrl: 'https://portfolio.dev',
      instagramUrl: 'https://instagram.com',
      whatsappUrl: 'https://wa.me'
    },
    {
      id: 'dev-2',
      name: 'Mopy Cabezas Leonardo',
      role: 'Desarrollador Full Stack',
      description: 'Ingeniería y desarrollo integral del ERP: arquitectura frontend, servicios backend Spring Boot y seguridad multi-tenant.',
      initials: 'LM',
      avatarGradient: 'linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%)',
      tags: ['Full Stack', 'Angular', 'Spring Boot', 'PostgreSQL', 'Docker'],
      githubUrl: 'https://github.com',
      linkedinUrl: 'https://linkedin.com',
      portfolioUrl: 'https://portfolio.dev',
      instagramUrl: 'https://instagram.com',
      whatsappUrl: 'https://wa.me'
    },
    {
      id: 'dev-3',
      name: 'Perez Leon Luis Enrique',
      role: 'Desarrollador Full Stack',
      description: 'Ingeniería y desarrollo integral del ERP: interfaces web interactivas, endpoints RESTful y base de datos relacional.',
      initials: 'LP',
      avatarGradient: 'linear-gradient(135deg, #0891b2 0%, #06b6d4 100%)',
      tags: ['Full Stack', 'Angular', 'Spring Boot', 'PostgreSQL', 'Docker'],
      githubUrl: 'https://github.com',
      linkedinUrl: 'https://linkedin.com',
      portfolioUrl: 'https://portfolio.dev',
      instagramUrl: 'https://instagram.com',
      whatsappUrl: 'https://wa.me'
    },
    {
      id: 'dev-4',
      name: 'Quispe Mamani Javier',
      role: 'Desarrollador Full Stack',
      description: 'Ingeniería y desarrollo integral del ERP: lógica comercial, flujos de datos transaccionales e integración de servicios.',
      initials: 'JQ',
      avatarGradient: 'linear-gradient(135deg, #ea580c 0%, #f97316 100%)',
      tags: ['Full Stack', 'Angular', 'Spring Boot', 'PostgreSQL', 'Docker'],
      githubUrl: 'https://github.com',
      linkedinUrl: 'https://linkedin.com',
      portfolioUrl: 'https://portfolio.dev',
      instagramUrl: 'https://instagram.com',
      whatsappUrl: 'https://wa.me'
    },
    {
      id: 'dev-5',
      name: 'Verduguez Teran Nicolas Junior',
      role: 'Desarrollador Full Stack',
      description: 'Ingeniería y desarrollo integral del ERP: automatización contable, modelado de bases de datos y arquitectura full stack.',
      initials: 'NV',
      avatarGradient: 'linear-gradient(135deg, #059669 0%, #10b981 100%)',
      tags: ['Full Stack', 'Angular', 'Spring Boot', 'PostgreSQL', 'Docker'],
      githubUrl: 'https://github.com',
      linkedinUrl: 'https://linkedin.com',
      portfolioUrl: 'https://portfolio.dev',
      instagramUrl: 'https://instagram.com',
      whatsappUrl: 'https://wa.me'
    },
    {
      id: 'dev-6',
      name: 'Yevara Ponce Alessandro',
      role: 'Desarrollador Full Stack',
      description: 'Ingeniería y desarrollo integral del ERP: integración frontend-backend, aplicaciones móviles y despliegue en la nube.',
      initials: 'AY',
      avatarGradient: 'linear-gradient(135deg, #9333ea 0%, #c084fc 100%)',
      tags: ['Full Stack', 'Angular', 'Spring Boot', 'PostgreSQL', 'Flutter'],
      githubUrl: 'https://github.com',
      linkedinUrl: 'https://linkedin.com',
      portfolioUrl: 'https://portfolio.dev',
      instagramUrl: 'https://instagram.com',
      whatsappUrl: 'https://wa.me'
    }
  ]);

  openCreateModal(): void {
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  onCompanyCreated(): void {
    this.showNotification('¡Empresa creada con éxito! Su espacio de datos está activo.');
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
