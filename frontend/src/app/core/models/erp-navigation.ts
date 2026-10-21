export interface ErpFunction {
  id: string;
  name: string;
  slug: string;
  rolesPermitidos?: string[];
}

export interface ErpModule {
  id: string;
  name: string;
  icon: string;
  functions: ErpFunction[];
}

export const ERP_MODULES: ErpModule[] = [
  {
    id: 'seguridad-y-auditoria',
    name: 'Configuración y Seguridad',
    icon: '◎',
    functions: [
      { id: 'gestionar-usuarios', slug: 'gestionar-usuarios', name: 'Gestionar Usuarios', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'gestionar-roles-y-permisos', slug: 'gestionar-roles-y-permisos', name: 'Gestionar Roles y Permisos', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'gestionar-empleados', slug: 'gestionar-empleados', name: 'Gestionar Empleados', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'empresas-sucursales-configuracion', slug: 'empresas-sucursales-configuracion', name: 'Gestionar Empresas, Sucursales y Configuración', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'personalizar-apariencia', slug: 'personalizar-apariencia', name: 'Personalizar Apariencia', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'backups-y-restore', slug: 'backups-y-restore', name: 'Gestionar Backups y Restore', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'consultar-bitacora', slug: 'consultar-bitacora', name: 'Consultar Bitácora', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] }
    ]
  },
  {
    id: 'catalogo-e-inventario',
    name: 'Catálogo e Inventarios',
    icon: '▦',
    functions: [
      { id: 'datos-maestros-inventario', slug: 'datos-maestros-inventario', name: 'Gestionar Datos Maestros de Inventario', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] },
      { id: 'catalogo-de-articulos', slug: 'catalogo-de-articulos', name: 'Gestionar Catálogo de Artículos', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO', 'VENDEDOR'] },
      { id: 'movimientos-de-inventario', slug: 'movimientos-de-inventario', name: 'Gestionar Movimientos de Inventario', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] },
      { id: 'consultar-kardex', slug: 'consultar-kardex', name: 'Consultar Kardex', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO', 'VENDEDOR'] }
    ]
  },
  {
    id: 'ventas-y-facturacion',
    name: 'Ventas y Facturación',
    icon: '↗',
    functions: [
      { id: 'gestionar-clientes', slug: 'gestionar-clientes', name: 'Gestionar Clientes', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR', 'CAJERO'] },
      { id: 'precios-y-listas-precio', slug: 'precios-y-listas-precio', name: 'Gestionar Precios y Listas de Precio', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR'] },
      { id: 'gestionar-cotizaciones', slug: 'gestionar-cotizaciones', name: 'Gestionar Cotizaciones', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR'] },
      { id: 'pedidos-de-venta', slug: 'pedidos-de-venta', name: 'Gestionar Pedidos de Venta', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR'] },
      { id: 'validacion-comercial', slug: 'validacion-comercial', name: 'Validación Comercial', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR'] },
      { id: 'gestionar-despacho-de-venta', slug: 'gestionar-despacho-de-venta', name: 'Gestionar Despacho de Venta', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO', 'VENDEDOR'] },
      { id: 'facturacion', slug: 'facturacion', name: 'Registrar Venta, Facturación y Anulación', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO', 'VENDEDOR'] },
      { id: 'devoluciones-y-notas-de-credito', slug: 'devoluciones-y-notas-de-credito', name: 'Gestionar Devoluciones y Notas de Crédito', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO', 'VENDEDOR', 'ALMACENERO'] }
    ]
  },
  {
    id: 'compras-y-proveedores',
    name: 'Compras y Proveedores',
    icon: '⇄',
    functions: [
      { id: 'gestionar-proveedores', slug: 'gestionar-proveedores', name: 'Gestionar Proveedores', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] },
      { id: 'gestionar-ordenes-de-compra', slug: 'gestionar-ordenes-de-compra', name: 'Gestionar Órdenes de Compra', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] },
      { id: 'registrar-compra-y-devolucion', slug: 'registrar-compra-y-devolucion', name: 'Registrar Compra y Devolución de Proveedores', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] },
      { id: 'gestionar-cuentas-por-pagar', slug: 'gestionar-cuentas-por-pagar', name: 'Gestionar Cuentas por Pagar', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO', 'CONTADOR', 'GERENTE_GENERAL', 'AUDITOR_INTERNO'] }
    ]
  },
  {
    id: 'contabilidad-y-finanzas',
    name: 'Contabilidad y Finanzas',
    icon: '≡',
    functions: [
      { id: 'plan-de-cuentas', slug: 'plan-de-cuentas', name: 'Gestionar Plan de Cuentas', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'periodos-contables', slug: 'periodos-contables', name: 'Gestionar Periodos Contables', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'asientos-contables', slug: 'asientos-contables', name: 'Gestionar Asientos Contables', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'integracion-contable-automatica', slug: 'integracion-contable-automatica', name: 'Integración Contable Automática', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cuentas-por-cobrar-y-cobros', slug: 'cuentas-por-cobrar-y-cobros', name: 'Gestionar Cuentas por Cobrar y Cobros', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO'] },
      { id: 'consultar-libros-contables', slug: 'consultar-libros-contables', name: 'Consultar Libros Contables', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'consultar-informacion-financiera', slug: 'consultar-informacion-financiera', name: 'Consultar Información Financiera', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] }
    ]
  },
  {
    id: 'tesoreria-y-cajas',
    name: 'Tesorería y Cajas',
    icon: '▣',
    functions: [
      { id: 'apertura-de-caja', slug: 'apertura-de-caja', name: 'Apertura de Caja', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO'] },
      { id: 'venta-en-caja-pos', slug: 'venta-en-caja-pos', name: 'Venta en Caja (POS)', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO'] },
      { id: 'registrar-movimiento-de-caja', slug: 'registrar-movimiento-de-caja', name: 'Registrar Movimiento de Caja', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO'] },
      { id: 'cierre-arqueo-consulta-caja', slug: 'cierre-arqueo-consulta-caja', name: 'Cierre, Arqueo y Consulta de Caja', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO'] }
    ]
  },
  {
    id: 'reportes-y-bi',
    name: 'Reportes',
    icon: '◰',
    functions: [
      { id: 'dashboard-ejecutivo', slug: 'dashboard-ejecutivo', name: 'Dashboard Ejecutivo', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR', 'GERENTE_GENERAL', 'CONTADOR', 'AUDITOR_INTERNO'] },
      { id: 'gestionar-reportes-predefinidos', slug: 'gestionar-reportes-predefinidos', name: 'Gestionar Reportes Predefinidos', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR', 'ALMACENERO', 'CAJERO'] },
      { id: 'gestionar-reportes-qbe', slug: 'gestionar-reportes-qbe', name: 'Gestionar Reportes QBE', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'gestionar-reportes-por-voz', slug: 'gestionar-reportes-por-voz', name: 'Gestionar Reportes por Voz', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR'] }
    ]
  }
];
