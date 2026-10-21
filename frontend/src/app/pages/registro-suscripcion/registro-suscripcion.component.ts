import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ThemeService } from '../../core/services/theme.service';
import { environment } from '../../../environments/environment';

export interface PlanInfo {
  codigo: string;
  nombre: string;
  tagline: string;
  precioMensual: number;
  precioAnual: number;
  limiteUsuarios: number;
  limiteSucursales: number;
  limiteAlmacenes: number;
  caracteristicas: string[];
  destacado?: boolean;
}

interface MonedaCatalogo {
  codigo: string;
  nombre: string;
  simbolo: string;
}

interface PaisCatalogo {
  codigoIso2: string;
  codigoIso3: string;
  nombre: string;
  monedas: MonedaCatalogo[];
}

@Component({
  selector: 'app-registro-suscripcion',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './registro-suscripcion.component.html',
  styleUrl: './registro-suscripcion.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegistroSuscripcionComponent {
  readonly themeService = inject(ThemeService);
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly http = inject(HttpClient);

  // Pasos del Asistente (1: Plan, 2: Empresa, 3: Administrador, 4: Éxito)
  readonly currentStep = signal<number>(1);
  readonly cicloFacturacion = signal<'mensual' | 'anual'>('mensual');
  readonly planSeleccionado = signal<string>('PRUEBA');
  readonly loading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal<boolean>(false);
  readonly paises = signal<PaisCatalogo[]>([]);
  readonly monedas = signal<MonedaCatalogo[]>([]);
  readonly provisionedResult = signal<{
    empresaNombre: string;
    nit: string;
    dbName: string;
    adminEmail: string;
    codigoEmpresa: string;
    planNombre: string;
  } | null>(null);

  readonly planes: PlanInfo[] = [
    {
      codigo: 'PRUEBA',
      nombre: 'Prueba gratuita',
      tagline: 'Siete días sin necesidad de realizar un pago.',
      precioMensual: 0,
      precioAnual: 0,
      limiteUsuarios: 3,
      limiteSucursales: 1,
      limiteAlmacenes: 1,
      caracteristicas: ['Sin tarjeta', 'Base PostgreSQL dedicada', 'Hasta 100 productos']
    },
    {
      codigo: 'BASICO',
      nombre: 'Plan Emprendedor Start',
      tagline: 'Ideal para microempresas y comercios en fase inicial.',
      precioMensual: 29.90,
      precioAnual: 299,
      limiteUsuarios: 10,
      limiteSucursales: 1,
      limiteAlmacenes: 1,
      caracteristicas: [
        'Hasta 20 usuarios concurrentes',
        '1 sucursal y 1 almacén',
        'Facturación electrónica y cotizaciones',
        'Catálogo de artículos y kardex básico',
        'Base de datos PostgreSQL dedicada'
      ]
    },
    {
      codigo: 'PREMIUM',
      nombre: 'Plan Profesional PyME',
      tagline: 'Para empresas en expansión con múltiples puntos de venta.',
      precioMensual: 69.90,
      precioAnual: 699,
      limiteUsuarios: 30,
      limiteSucursales: 3,
      limiteAlmacenes: 5,
      destacado: true,
      caracteristicas: [
        'Hasta 100 usuarios concurrentes',
        '3 sucursales y 5 almacenes',
        'Multialmacén y transferencias de stock',
        'Control de caja POS y arqueo de turnos',
        'Compras, cuentas por pagar y proveedores',
        'Base de datos PostgreSQL dedicada'
      ]
    },
    {
      codigo: 'CORPORATIVO',
      nombre: 'Plan Corporativo Enterprise',
      tagline: 'Capacidad total para empresas comerciales y contables consolidadas.',
      precioMensual: 149.90,
      precioAnual: 1499,
      limiteUsuarios: 100,
      limiteSucursales: 10,
      limiteAlmacenes: 20,
      caracteristicas: [
        'Hasta 200 usuarios concurrentes',
        '10 sucursales y 20 almacenes',
        'Contabilidad automática integrada (Libro Diario)',
        'Reportes ejecutivos y balance de comprobación',
        'Historial de auditoría inmutable (Bitácora)',
        'Soporte prioritario y copias de seguridad diarias'
      ]
    },
    {
      codigo: 'EMPRESARIAL',
      nombre: 'Plan Empresarial',
      tagline: 'Capacidad ampliada para grandes organizaciones.',
      precioMensual: 299.90,
      precioAnual: 2999,
      limiteUsuarios: 500,
      limiteSucursales: 50,
      limiteAlmacenes: 100,
      caracteristicas: ['Hasta 100.000 productos', 'Acceso completo', 'Analítica avanzada']
    }
  ];

  // Formulario reactivo de Empresa y Administrador
  readonly onboardingForm = this.fb.nonNullable.group({
    // Datos de la Empresa
    razonSocial: ['', [Validators.required, Validators.maxLength(200)]],
    nombreComercial: ['', [Validators.maxLength(200)]],
    nit: ['', [Validators.required, Validators.pattern(/^[0-9]{6,15}$/)]],
    paisCodigo: ['BO', [Validators.required]],
    ciudad: ['', [Validators.required]],
    direccion: ['', [Validators.maxLength(255)]],
    telefonoEmpresa: ['', [Validators.maxLength(30)]],
    monedaPrincipal: ['BOB', [Validators.required]],
    
    // Datos del Administrador de la Empresa
    adminNombre: ['', [Validators.required, Validators.maxLength(150)]],
    adminEmail: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    adminPassword: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    adminTelefono: ['', [Validators.maxLength(30)]],
    terminosAceptados: [true, [Validators.requiredTrue]]
  });

  constructor() {
    this.cargarCatalogos();
    this.route.queryParams.subscribe(params => {
      if (params['pago'] === 'exito' && params['session_id']) {
        this.confirmarPago(params['session_id']);
      } else if (params['pago'] === 'cancelado') {
        this.errorMessage.set('El pago fue cancelado. La empresa permanecerá pendiente de activación.');
      }
      if (params['plan']) {
        const found = this.planes.find(p => p.codigo === params['plan']);
        if (found) {
          this.planSeleccionado.set(found.codigo);
        }
      }
      if (params['ciclo'] === 'anual' || params['ciclo'] === 'mensual') {
        this.cicloFacturacion.set(params['ciclo']);
      }
    });
  }

  get planActual(): PlanInfo {
    return this.planes.find(p => p.codigo === this.planSeleccionado()) ?? this.planes[1];
  }

  get precioCalculado(): number {
    return this.cicloFacturacion() === 'anual' ? this.planActual.precioAnual : this.planActual.precioMensual;
  }

  seleccionarPlan(codigo: string): void {
    this.planSeleccionado.set(codigo);
  }

  setPeriodo(ciclo: 'mensual' | 'anual'): void {
    this.cicloFacturacion.set(ciclo);
  }

  togglePassword(): void {
    this.showPassword.update(v => !v);
  }

  irAlPaso(paso: number): void {
    this.errorMessage.set(null);
    if (paso === 2) {
      this.currentStep.set(2);
    } else if (paso === 3) {
      // Validar datos de empresa antes de avanzar
      const { razonSocial, nit, ciudad } = this.onboardingForm.controls;
      razonSocial.markAsTouched();
      nit.markAsTouched();
      ciudad.markAsTouched();
      if (razonSocial.invalid || nit.invalid || ciudad.invalid) {
        this.errorMessage.set('Por favor completa los datos obligatorios de la empresa (Razón Social, NIT y Ciudad).');
        return;
      }
      this.currentStep.set(3);
    } else if (paso === 1) {
      this.currentStep.set(1);
    }
  }

  finalizarOnboarding(): void {
    if (this.onboardingForm.invalid) {
      this.onboardingForm.markAllAsTouched();
      this.errorMessage.set('Por favor completa todos los campos requeridos correctamente.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const formValues = this.onboardingForm.getRawValue();
    const payload = {
      planCodigo: this.planSeleccionado(),
      cicloFacturacion: this.cicloFacturacion().toUpperCase(),
      razonSocial: formValues.razonSocial,
      nombreComercial: formValues.nombreComercial || formValues.razonSocial,
      nit: formValues.nit,
      ciudad: formValues.ciudad,
      direccion: formValues.direccion,
      telefonoEmpresa: formValues.telefonoEmpresa,
      monedaPrincipal: formValues.monedaPrincipal,
      paisCodigo: formValues.paisCodigo,
      adminNombre: formValues.adminNombre,
      adminEmail: formValues.adminEmail,
      adminPassword: formValues.adminPassword,
      adminTelefono: formValues.adminTelefono
    };

    this.http.post<any>(`${environment.apiUrl}/auth/onboarding-empresa`, payload).subscribe({
      next: (res) => {
        this.loading.set(false);
        const result = {
          empresaNombre: res.empresaNombre || formValues.razonSocial,
          nit: res.nit || formValues.nit,
          dbName: res.dbName,
          adminEmail: res.adminEmail || formValues.adminEmail,
          codigoEmpresa: res.codigoEmpresa,
          planNombre: this.planActual.nombre
        };
        if (res.checkoutUrl) {
          sessionStorage.setItem('erp_pending_onboarding', JSON.stringify(result));
          window.location.assign(res.checkoutUrl);
          return;
        }
        this.provisionedResult.set(result);
        this.currentStep.set(4);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'No se pudo crear la empresa. Inténtalo nuevamente.');
      }
    });
  }

  irAlLogin(): void {
    this.router.navigate(['/login']);
  }

  seleccionarPais(codigoIso2: string): void {
    const country = this.paises().find(item => item.codigoIso2 === codigoIso2);
    const currencies = country?.monedas ?? [];
    this.monedas.set(currencies);
    if (currencies.length > 0) {
      this.onboardingForm.controls.monedaPrincipal.setValue(currencies[0].codigo);
    }
  }

  private cargarCatalogos(): void {
    this.http.get<PaisCatalogo[]>(`${environment.apiUrl}/public/catalogos/paises`).subscribe({
      next: countries => {
        this.paises.set(countries);
        this.seleccionarPais(this.onboardingForm.controls.paisCodigo.value);
      },
      error: () => this.errorMessage.set('No se pudo cargar el catálogo mundial de países y monedas.')
    });
  }

  private confirmarPago(sessionId: string): void {
    this.loading.set(true);
    this.http.post<{ estado: string; codigoEmpresa: string }>(
      `${environment.apiUrl}/auth/onboarding-confirmar`,
      null,
      { params: { sessionId } }
    ).subscribe({
      next: response => {
        const stored = sessionStorage.getItem('erp_pending_onboarding');
        const pending = stored ? JSON.parse(stored) : null;
        sessionStorage.removeItem('erp_pending_onboarding');
        this.provisionedResult.set({
          empresaNombre: pending?.empresaNombre ?? 'Empresa registrada',
          nit: pending?.nit ?? '',
          dbName: pending?.dbName ?? '',
          adminEmail: pending?.adminEmail ?? '',
          codigoEmpresa: response.codigoEmpresa,
          planNombre: pending?.planNombre ?? 'Suscripción activa'
        });
        this.loading.set(false);
        this.currentStep.set(4);
      },
      error: err => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Stripe todavía no confirmó el pago.');
      }
    });
  }
}
