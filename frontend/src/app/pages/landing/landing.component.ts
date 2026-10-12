import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CompanyService } from '../../core/services/company.service';
import { ThemeService } from '../../core/services/theme.service';
import { CompanyModalComponent } from '../../components/company-modal/company-modal.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, FormsModule, CompanyModalComponent],
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

  // Specialist Demo Form State
  demoData = {
    fullName: '',
    workEmail: '',
    phone: '',
    employees: '10 a 50 empleados',
    goal: 'Automatizar conciliación bancaria y pólizas contables'
  };
  demoLoading = signal(false);

  // Billing cycle state
  billingCycle = signal<'mensual' | 'anual'>('mensual');

  // Subscription plans from ERP_Master
  readonly planes = [
    {
      codigo: 'PLAN-BASICO',
      nombre: 'Plan Emprendedor Start',
      tagline: 'Diseñado para microempresas y comercios en crecimiento.',
      precioMensual: 299,
      precioAnual: 2990,
      limiteUsuarios: 20,
      limiteSucursales: 1,
      limiteAlmacenes: 1,
      caracteristicas: [
        'Hasta 20 usuarios incluidos',
        '1 sucursal y 1 almacén',
        'Facturación electrónica y cotizaciones',
        'Catálogo de artículos y kardex esencial',
        'Base de datos PostgreSQL dedicada'
      ]
    },
    {
      codigo: 'PLAN-PRO',
      nombre: 'Plan Profesional PyME',
      tagline: 'Para empresas en expansión que operan múltiples puntos de venta.',
      precioMensual: 699,
      precioAnual: 6990,
      limiteUsuarios: 100,
      limiteSucursales: 3,
      limiteAlmacenes: 5,
      destacado: true,
      caracteristicas: [
        'Hasta 100 usuarios incluidos',
        '3 sucursales y 5 almacenes',
        'Multialmacén y transferencias de stock',
        'Control de turnos, arqueo de caja y POS',
        'Compras, cuentas por pagar y proveedores',
        'Base de datos PostgreSQL dedicada'
      ]
    },
    {
      codigo: 'PLAN-CORP',
      nombre: 'Plan Corporativo Enterprise',
      tagline: 'Capacidad total con contabilidad analítica en tiempo real.',
      precioMensual: 1499,
      precioAnual: 14990,
      limiteUsuarios: 200,
      limiteSucursales: 10,
      limiteAlmacenes: 20,
      caracteristicas: [
        'Hasta 200 usuarios incluidos',
        '10 sucursales y 20 almacenes',
        'Contabilidad automática integrada (Libro Diario)',
        'Reportes ejecutivos, estados financieros y QBE',
        'Bitácora inmutable de auditoría',
        'Soporte prioritario y copias de seguridad diarias'
      ]
    }
  ];

  setPeriodo(ciclo: 'mensual' | 'anual'): void {
    this.billingCycle.set(ciclo);
  }

  seleccionarPlan(codigo: string): void {
    this.router.navigate(['/registro-suscripcion'], {
      queryParams: {
        plan: codigo,
        ciclo: this.billingCycle()
      }
    });
  }

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

  submitDemo(event?: Event): void {
    if (event) {
      event.preventDefault();
    }
    if (!this.demoData.fullName || !this.demoData.workEmail) {
      this.showNotification('Por favor completa tu nombre y correo corporativo.');
      return;
    }
    this.demoLoading.set(true);
    setTimeout(() => {
      this.demoLoading.set(false);
      this.showNotification('¡Demostración confirmada! Un especialista comercial y contable te contactará.');
      this.demoData = {
        fullName: '',
        workEmail: '',
        phone: '',
        employees: '10 a 50 empleados',
        goal: 'Automatizar conciliación bancaria y pólizas contables'
      };
    }, 700);
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
