import { Component, EventEmitter, Input, Output, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CompanyService } from '../../core/services/company.service';
import { CreateCompanyDto } from '../../core/models/company.model';

@Component({
  selector: 'app-company-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    @if (isOpen) {
      <div class="modal-backdrop" (click)="onBackdropClick($event)">
        <div class="modal-card glass-card" (click)="$event.stopPropagation()">
          
          <!-- Header -->
          <div class="modal-header">
            <div class="header-icon">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect width="16" height="20" x="4" y="2" rx="2" ry="2"/>
                <path d="M9 22v-4h6v4"/>
                <path d="M8 6h.01"/>
                <path d="M16 6h.01"/>
                <path d="M12 6h.01"/>
                <path d="M12 10h.01"/>
                <path d="M12 14h.01"/>
                <path d="M16 10h.01"/>
                <path d="M16 14h.01"/>
                <path d="M8 10h.01"/>
                <path d="M8 14h.01"/>
              </svg>
            </div>
            <div>
              <h3 class="modal-title">Registrar Nueva Empresa</h3>
              <p class="modal-subtitle">Configura la entidad para gestionar sus catálogos, productos y contabilidad</p>
            </div>
            <button class="close-btn" (click)="closeModal()" aria-label="Cerrar modal">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
          </div>

          <!-- Form Body -->
          <form (ngSubmit)="onSubmit()" class="modal-form">
            <div class="form-grid">
              
              <!-- Razón Social -->
              <div class="form-group full-width">
                <label for="companyName">Razón Social o Nombre Comercial *</label>
                <input
                  id="companyName"
                  name="name"
                  type="text"
                  required
                  [(ngModel)]="formData.name"
                  placeholder="Ej. Comercializadora del Oriente S.R.L."
                  class="form-input"
                />
              </div>

              <!-- NIT / Identificación -->
              <div class="form-group">
                <label for="taxId">NIT / Identificación Tributaria *</label>
                <input
                  id="taxId"
                  name="taxId"
                  type="text"
                  required
                  [(ngModel)]="formData.taxId"
                  placeholder="Ej. 1029482015"
                  class="form-input font-mono"
                />
              </div>

              <!-- Rubro Comercial -->
              <div class="form-group">
                <label for="commercialActivity">Rubro o Giro Comercial *</label>
                <select
                  id="commercialActivity"
                  name="commercialActivity"
                  [(ngModel)]="formData.commercialActivity"
                  class="form-input form-select"
                >
                  <option value="Distribuidora y Venta Mayorista">Distribuidora y Venta Mayorista</option>
                  <option value="Comercio Minorista / Retail">Comercio Minorista / Retail</option>
                  <option value="Supermercado / Abarrotes">Supermercado / Abarrotes</option>
                  <option value="Ferretería y Construcción">Ferretería y Construcción</option>
                  <option value="Farmacia y Cosmética">Farmacia y Cosmética</option>
                  <option value="Servicios Comerciales y Consultoría">Servicios Comerciales y Consultoría</option>
                </select>
              </div>

              <!-- Moneda Principal -->
              <div class="form-group">
                <label>Moneda Principal Contable</label>
                <div class="currency-toggle">
                  <button
                    type="button"
                    class="curr-btn"
                    [class.active]="formData.currency === 'BOB'"
                    (click)="formData.currency = 'BOB'"
                  >
                    <span>🇧🇴 Bolivianos (BOB)</span>
                  </button>
                  <button
                    type="button"
                    class="curr-btn"
                    [class.active]="formData.currency === 'USD'"
                    (click)="formData.currency = 'USD'"
                  >
                    <span>💵 Dólares (USD)</span>
                  </button>
                </div>
              </div>

              <!-- Teléfono -->
              <div class="form-group">
                <label for="phone">Teléfono / Celular de Contacto</label>
                <input
                  id="phone"
                  name="phone"
                  type="text"
                  [(ngModel)]="formData.phone"
                  placeholder="Ej. +591 76012345"
                  class="form-input"
                />
              </div>

              <!-- Email -->
              <div class="form-group">
                <label for="email">Correo Corporativo</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  [(ngModel)]="formData.email"
                  placeholder="contacto@empresa.com"
                  class="form-input"
                />
              </div>

              <!-- Dirección -->
              <div class="form-group">
                <label for="address">Dirección / Sede Principal</label>
                <input
                  id="address"
                  name="address"
                  type="text"
                  [(ngModel)]="formData.address"
                  placeholder="Ej. Av. Banzer Km 5, Santa Cruz"
                  class="form-input"
                />
              </div>

            </div>

            <!-- Notice box -->
            <div class="accounting-notice">
              <div class="notice-icon">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 2v20M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
                </svg>
              </div>
              <div class="notice-text">
                <strong>Integración Contable Automática:</strong> Al crear esta empresa se creará automáticamente su estructura de Plan de Cuentas base y libro diario vinculado a los futuros productos y ventas.
              </div>
            </div>

            @if (errorMessage()) {
              <div class="error-banner">
                {{ errorMessage() }}
              </div>
            }

            <!-- Actions -->
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" (click)="closeModal()">
                Cancelar
              </button>
              <button type="submit" class="btn btn-primary" [disabled]="isSubmitting()">
                @if (isSubmitting()) {
                  <span>Registrando...</span>
                } @else {
                  <span>Confirmar y Crear Empresa</span>
                }
              </button>
            </div>
          </form>

        </div>
      </div>
    }
  `,
  styles: [`
    .modal-backdrop {
      position: fixed;
      inset: 0;
      background: rgba(4, 7, 15, 0.85);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 1rem;
      animation: fadeIn 0.2s ease-out;
    }

    .modal-card {
      width: 100%;
      max-width: 640px;
      max-height: 90vh;
      overflow-y: auto;
      background: #0d1322;
      border: 1px solid rgba(99, 102, 241, 0.25);
      border-radius: 20px;
      box-shadow: 0 20px 40px -15px rgba(0, 0, 0, 0.7), 0 0 35px rgba(99, 102, 241, 0.18);
      padding: 1.75rem;
    }

    .modal-header {
      display: flex;
      align-items: flex-start;
      gap: 1rem;
      margin-bottom: 1.5rem;
      position: relative;
    }

    .header-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      background: rgba(99, 102, 241, 0.15);
      color: #818cf8;
      border: 1px solid rgba(99, 102, 241, 0.3);
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .modal-title {
      font-size: 1.25rem;
      font-weight: 700;
      color: #f8fafc;
      line-height: 1.2;
    }

    .modal-subtitle {
      font-size: 0.85rem;
      color: #94a3b8;
      margin-top: 0.25rem;
    }

    .close-btn {
      margin-left: auto;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.1);
      color: #94a3b8;
      width: 34px;
      height: 34px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        color: #ffffff;
      }
    }

    .form-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 1rem;

      @media (max-width: 600px) {
        grid-template-columns: 1fr;
      }
    }

    .full-width {
      grid-column: 1 / -1;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;

      label {
        font-size: 0.82rem;
        font-weight: 600;
        color: #cbd5e1;
      }
    }

    .form-input {
      background: rgba(15, 23, 42, 0.8);
      border: 1px solid rgba(255, 255, 255, 0.12);
      border-radius: 10px;
      padding: 0.7rem 0.9rem;
      color: #f8fafc;
      font-size: 0.9rem;
      font-family: inherit;
      outline: none;
      transition: border-color 0.2s, box-shadow 0.2s;

      &:focus {
        border-color: #6366f1;
        box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.2);
      }

      &::placeholder {
        color: #64748b;
      }
    }

    .form-select {
      cursor: pointer;
      appearance: none;
      background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E");
      background-repeat: no-repeat;
      background-position: right 0.75rem center;
      padding-right: 2.2rem;
    }

    .currency-toggle {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.5rem;
    }

    .curr-btn {
      background: rgba(15, 23, 42, 0.8);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 8px;
      padding: 0.6rem;
      font-size: 0.82rem;
      color: #94a3b8;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;

      &.active {
        background: rgba(99, 102, 241, 0.2);
        border-color: #6366f1;
        color: #c7d2fe;
        font-weight: 600;
      }
    }

    .accounting-notice {
      margin-top: 1.25rem;
      background: rgba(16, 185, 129, 0.08);
      border: 1px solid rgba(16, 185, 129, 0.25);
      border-radius: 12px;
      padding: 0.85rem 1rem;
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;

      .notice-icon {
        color: #10b981;
        margin-top: 2px;
      }

      .notice-text {
        font-size: 0.8rem;
        color: #a7f3d0;
        line-height: 1.4;

        strong {
          color: #34d399;
        }
      }
    }

    .error-banner {
      margin-top: 1rem;
      background: rgba(244, 63, 94, 0.12);
      border: 1px solid rgba(244, 63, 94, 0.3);
      border-radius: 8px;
      padding: 0.6rem 0.9rem;
      color: #fca5a5;
      font-size: 0.85rem;
    }

    .modal-footer {
      margin-top: 1.5rem;
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: scale(0.98); }
      to { opacity: 1; transform: scale(1); }
    }
  `]
})
export class CompanyModalComponent {
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();
  @Output() companyCreated = new EventEmitter<void>();

  private companyService = inject(CompanyService);

  formData: CreateCompanyDto = {
    name: '',
    taxId: '',
    commercialActivity: 'Distribuidora y Venta Mayorista',
    currency: 'BOB',
    address: '',
    phone: '',
    email: ''
  };

  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);

  onBackdropClick(e: MouseEvent): void {
    this.closeModal();
  }

  closeModal(): void {
    this.errorMessage.set(null);
    this.close.emit();
  }

  onSubmit(): void {
    if (!this.formData.name.trim() || !this.formData.taxId.trim()) {
      this.errorMessage.set('Por favor completa la Razón Social y el NIT de la empresa.');
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set(null);

    try {
      this.companyService.createCompany(this.formData);
      this.isSubmitting.set(false);
      this.companyCreated.emit();
      this.closeModal();
      
      // Reset form
      this.formData = {
        name: '',
        taxId: '',
        commercialActivity: 'Distribuidora y Venta Mayorista',
        currency: 'BOB',
        address: '',
        phone: '',
        email: ''
      };
    } catch (err: any) {
      this.isSubmitting.set(false);
      this.errorMessage.set('Ocurrió un error al registrar la empresa. Intenta nuevamente.');
    }
  }
}
