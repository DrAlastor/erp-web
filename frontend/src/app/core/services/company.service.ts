import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { Company, CreateCompanyDto } from '../models/company.model';

const INITIAL_COMPANIES: Company[] = [
  {
    id: 'emp-001',
    name: 'Distribuidora Santa Cruz S.R.L.',
    taxId: '1029384019',
    commercialActivity: 'Comercializadora y Distribución Mayorista',
    currency: 'BOB',
    address: 'Av. Cristo Redentor 4to Anillo, Santa Cruz',
    phone: '+591 3 3456789',
    email: 'contacto@distribuidorasc.com.bo',
    branchesCount: 3,
    productCatalogCount: 420,
    isDefault: true,
    createdAt: '2026-01-15'
  },
  {
    id: 'emp-002',
    name: 'Comercial & Contable Los Andes S.A.',
    taxId: '2049581023',
    commercialActivity: 'Importación y Venta de Equipamiento',
    currency: 'BOB',
    address: 'Calle 21 de Calacoto #850, La Paz',
    phone: '+591 2 2789123',
    email: 'administracion@losandes.bo',
    branchesCount: 2,
    productCatalogCount: 185,
    createdAt: '2026-02-10'
  },
  {
    id: 'emp-003',
    name: 'ElectroTech Soluciones Globales',
    taxId: '3058472910',
    commercialActivity: 'Tecnología, Suministros y Servicios',
    currency: 'USD',
    address: 'Parque Industrial PI-27, Cochabamba',
    phone: '+591 4 4123456',
    email: 'ventas@electrotech.com',
    branchesCount: 1,
    productCatalogCount: 95,
    createdAt: '2026-03-01'
  }
];

const STORAGE_KEY_COMPANIES = 'nexo_erp_companies';
const STORAGE_KEY_ACTIVE = 'nexo_erp_active_company_id';

@Injectable({
  providedIn: 'root'
})
export class CompanyService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly isBrowser = isPlatformBrowser(this.platformId);

  private readonly _companies = signal<Company[]>(this.loadInitialCompanies());
  private readonly _activeCompanyId = signal<string>(this.loadInitialActiveId());

  readonly companies = this._companies.asReadonly();
  
  readonly activeCompany = computed(() => {
    const list = this._companies();
    const activeId = this._activeCompanyId();
    return list.find(c => c.id === activeId) ?? list[0] ?? null;
  });

  private loadInitialCompanies(): Company[] {
    if (!this.isBrowser) {
      return INITIAL_COMPANIES;
    }
    try {
      const stored = localStorage.getItem(STORAGE_KEY_COMPANIES);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      }
    } catch (e) {
      console.warn('Error reading companies from localStorage', e);
    }
    return INITIAL_COMPANIES;
  }

  private loadInitialActiveId(): string {
    if (!this.isBrowser) {
      return INITIAL_COMPANIES[0].id;
    }
    try {
      const storedId = localStorage.getItem(STORAGE_KEY_ACTIVE);
      if (storedId) {
        return storedId;
      }
    } catch (e) {
      console.warn('Error reading active company from localStorage', e);
    }
    return INITIAL_COMPANIES[0].id;
  }

  selectCompany(id: string): void {
    const exists = this._companies().some(c => c.id === id);
    if (exists) {
      this._activeCompanyId.set(id);
      if (this.isBrowser) {
        localStorage.setItem(STORAGE_KEY_ACTIVE, id);
      }
    }
  }

  createCompany(dto: CreateCompanyDto): Company {
    const newCompany: Company = {
      id: `emp-${Date.now().toString().slice(-4)}`,
      name: dto.name.trim(),
      taxId: dto.taxId.trim(),
      commercialActivity: dto.commercialActivity.trim(),
      currency: dto.currency || 'BOB',
      address: dto.address.trim() || 'Central',
      phone: dto.phone.trim() || 'S/N',
      email: dto.email.trim() || 'admin@empresa.com',
      branchesCount: 1,
      productCatalogCount: 0,
      createdAt: new Date().toISOString().split('T')[0]
    };

    const updated = [newCompany, ...this._companies()];
    this._companies.set(updated);
    this.selectCompany(newCompany.id);

    if (this.isBrowser) {
      try {
        localStorage.setItem(STORAGE_KEY_COMPANIES, JSON.stringify(updated));
      } catch (e) {
        console.error('Error saving new company in localStorage', e);
      }
    }

    return newCompany;
  }
}
