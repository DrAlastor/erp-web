import { Component, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CompanyService } from '../../core/services/company.service';
import { ThemeService } from '../../core/services/theme.service';
import { CompanyModalComponent } from '../../components/company-modal/company-modal.component';
import { environment } from '../../../environments/environment';

export interface MonedaCatalogoLanding {
  codigo: string;
  nombre: string;
  simbolo: string;
}

export interface PaisCatalogoLanding {
  codigoIso2: string;
  codigoIso3: string;
  nombre: string;
  monedas: MonedaCatalogoLanding[];
}

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, FormsModule, CompanyModalComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent implements OnInit {
  @ViewChild('planesScroll') planesScrollRef?: ElementRef<HTMLDivElement>;

  readonly companyService = inject(CompanyService);
  readonly themeService = inject(ThemeService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  // Global catalogs
  readonly paises = signal<PaisCatalogoLanding[]>([]);
  readonly catalogosLoading = signal<boolean>(true);

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
      codigo: 'PRUEBA',
      nombre: 'Prueba Gratuita',
      tagline: 'Acceso completo durante 7 días sin costo ni tarjeta.',
      precioMensual: 0,
      precioAnual: 0,
      precioUsdMensual: 0,
      precioUsdAnual: 0,
      limiteUsuarios: 3,
      limiteSucursales: 1,
      limiteAlmacenes: 1,
      esPrueba: true,
      caracteristicas: [
        'Hasta 3 usuarios concurrentes',
        '1 sucursal y 1 almacén',
        'Facturación, cotizaciones y ventas',
        'Base de datos PostgreSQL dedicada',
        'Sin compromiso de permanencia'
      ]
    },
    {
      codigo: 'BASICO',
      nombre: 'Plan Emprendedor Start',
      tagline: 'Diseñado para microempresas y comercios en crecimiento.',
      precioMensual: 299,
      precioAnual: 2990,
      precioUsdMensual: 29.90,
      precioUsdAnual: 299,
      limiteUsuarios: 20,
      limiteSucursales: 1,
      limiteAlmacenes: 1,
      esPrueba: false,
      caracteristicas: [
        'Hasta 20 usuarios incluidos',
        '1 sucursal y 1 almacén',
        'Facturación electrónica y cotizaciones',
        'Catálogo de artículos y kardex esencial',
        'Base de datos PostgreSQL dedicada'
      ]
    },
    {
      codigo: 'PREMIUM',
      nombre: 'Plan Profesional PyME',
      tagline: 'Para empresas en expansión que operan múltiples puntos de venta.',
      precioMensual: 699,
      precioAnual: 6990,
      precioUsdMensual: 69.90,
      precioUsdAnual: 699,
      limiteUsuarios: 100,
      limiteSucursales: 3,
      limiteAlmacenes: 5,
      destacado: true,
      esPrueba: false,
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
      codigo: 'CORPORATIVO',
      nombre: 'Plan Corporativo Enterprise',
      tagline: 'Capacidad total con contabilidad analítica en tiempo real.',
      precioMensual: 1499,
      precioAnual: 14990,
      precioUsdMensual: 149.90,
      precioUsdAnual: 1499,
      limiteUsuarios: 200,
      limiteSucursales: 10,
      limiteAlmacenes: 20,
      esPrueba: false,
      caracteristicas: [
        'Hasta 200 usuarios incluidos',
        '10 sucursales y 20 almacenes',
        'Contabilidad automática integrada (Libro Diario)',
        'Reportes ejecutivos, estados financieros y QBE',
        'Bitácora inmutable de auditoría',
        'Soporte prioritario y copias de seguridad diarias'
      ]
    },
    {
      codigo: 'EMPRESARIAL',
      nombre: 'Plan Empresarial Multi-Holding',
      tagline: 'Máxima infraestructura y soporte para grandes organizaciones.',
      precioMensual: 2999,
      precioAnual: 29990,
      precioUsdMensual: 299.90,
      precioUsdAnual: 2999,
      limiteUsuarios: 500,
      limiteSucursales: 50,
      limiteAlmacenes: 100,
      esPrueba: false,
      caracteristicas: [
        'Hasta 500 usuarios incluidos',
        '50 sucursales y 100 almacenes',
        'Multi-holding y consolidación contable',
        'Hasta 200.000 productos y transacciones ilimitadas',
        'Auditoría total y backups automatizados a S3',
        'SLA garantizado y soporte prioritario 24/7'
      ]
    }
  ];

  setPeriodo(ciclo: 'mensual' | 'anual'): void {
    this.billingCycle.set(ciclo);
  }

  scrollPlanes(direction: 'left' | 'right'): void {
    const el = this.planesScrollRef?.nativeElement;
    if (!el) return;
    const scrollAmount = 360;
    el.scrollBy({
      left: direction === 'left' ? -scrollAmount : scrollAmount,
      behavior: 'smooth'
    });
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

  ngOnInit(): void {
    this.cargarCatalogos();
  }

  private cargarCatalogos(): void {
    this.http.get<PaisCatalogoLanding[]>(`${environment.apiUrl}/public/catalogos/paises`).subscribe({
      next: (data) => {
        this.paises.set(data);
        this.catalogosLoading.set(false);
      },
      error: () => {
        this.catalogosLoading.set(false);
      }
    });
  }

  scrollToSection(id: string): void {
    const el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' });
    }
  }
}
