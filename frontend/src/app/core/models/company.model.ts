export interface Company {
  id: string;
  name: string;
  taxId: string; // NIT o Identificación Tributaria
  commercialActivity: string; // Rubro / Giro comercial (ej. Distribuidora, Retail, Servicios)
  currency: 'BOB' | 'USD';
  address: string;
  phone: string;
  email: string;
  branchesCount: number;
  productCatalogCount: number;
  isDefault?: boolean;
  createdAt: string;
}

export interface CreateCompanyDto {
  name: string;
  taxId: string;
  commercialActivity: string;
  currency: 'BOB' | 'USD';
  address: string;
  phone: string;
  email: string;
}
