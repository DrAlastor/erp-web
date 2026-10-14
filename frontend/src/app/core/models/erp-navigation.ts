export interface ErpFunction {
  id: string;
  name: string;
  code?: string;
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
      { id: 'cu02-gestionar-usuarios', name: 'Gestionar Usuarios', code: 'CU02', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu03-gestionar-roles-y-permisos', name: 'Gestionar Roles y Permisos', code: 'CU03', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu04-gestionar-empleados', name: 'Gestionar Empleados', code: 'CU04', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu05-empresas-sucursales-configuracion', name: 'Gestionar Empresas, Sucursales y Configuración', code: 'CU05', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu06-personalizar-apariencia', name: 'Personalizar Apariencia', code: 'CU06', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu07-backups-y-restore', name: 'Gestionar Backups y Restore', code: 'CU07', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu08-consultar-bitacora', name: 'Consultar Bitácora', code: 'CU08', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] }
    ]
  },
  {
    id: 'catalogo-e-inventario',
    name: 'Catálogo e Inventarios',
    icon: '▦',
    functions: [
      { id: 'cu09-datos-maestros-inventario', name: 'Gestionar Datos Maestros de Inventario', code: 'CU09', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] },
      { id: 'cu10-catalogo-de-articulos', name: 'Gestionar Catálogo de Artículos', code: 'CU10', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO', 'VENDEDOR'] },
      { id: 'cu11-movimientos-de-inventario', name: 'Gestionar Movimientos de Inventario', code: 'CU11', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] },
      { id: 'cu12-consultar-kardex', name: 'Consultar Kardex', code: 'CU12', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] }
    ]
  },
  {
    id: 'ventas-y-facturacion',
    name: 'Ventas y Facturación',
    icon: '↗',
    functions: [
      { id: 'cu13-gestionar-clientes', name: 'Gestionar Clientes', code: 'CU13', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR', 'CAJERO'] },
      { id: 'cu14-precios-y-listas-precio', name: 'Gestionar Precios y Listas de Precio', code: 'CU14', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR'] },
      { id: 'cu15-gestionar-cotizaciones', name: 'Gestionar Cotizaciones', code: 'CU15', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR'] },
      { id: 'cu16-pedidos-de-venta', name: 'Gestionar Pedidos de Venta', code: 'CU16', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR'] },
      { id: 'cu17-validacion-comercial-aprobacion', name: 'Validación Comercial y Aprobación', code: 'CU17', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR'] },
      { id: 'cu18-gestionar-despacho-de-venta', name: 'Gestionar Despacho de Venta', code: 'CU18', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO', 'VENDEDOR'] },
      { id: 'cu19-registrar-venta-facturacion-y-anulacion', name: 'Registrar Venta, Facturación y Anulación', code: 'CU19', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO', 'VENDEDOR'] },
      { id: 'cu20-devoluciones-de-venta', name: 'Gestionar Devoluciones y Notas de Crédito', code: 'CU20', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO', 'VENDEDOR'] }
    ]
  },
  {
    id: 'compras-y-proveedores',
    name: 'Compras y Proveedores',
    icon: '⇄',
    functions: [
      { id: 'cu21-gestionar-proveedores', name: 'Gestionar Proveedores', code: 'CU21', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] },
      { id: 'cu22-solicitudes-y-cotizaciones-compra', name: 'Gestionar Solicitudes y Cotizaciones de Compra', code: 'CU22', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] },
      { id: 'cu23-ordenes-de-compra', name: 'Gestionar Órdenes de Compra', code: 'CU23', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] },
      { id: 'cu24-recepcion-y-facturacion-compras', name: 'Recepción y Facturación de Compras', code: 'CU24', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'ALMACENERO'] }
    ]
  },
  {
    id: 'contabilidad-y-finanzas',
    name: 'Contabilidad y Finanzas',
    icon: '≡',
    functions: [
      { id: 'cu25-plan-de-cuentas', name: 'Gestionar Plan de Cuentas', code: 'CU25', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu26-asientos-contables-comprobantes', name: 'Gestionar Asientos Contables y Comprobantes', code: 'CU26', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu27-ejercicios-periodos-contables', name: 'Gestionar Ejercicios y Periodos Contables', code: 'CU27', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu28-ajustes-y-cierres-contables', name: 'Gestionar Ajustes y Cierres Contables', code: 'CU28', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu29-centros-de-costos', name: 'Gestionar Centros de Costos', code: 'CU29', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu30-activos-fijos', name: 'Gestionar Activos Fijos', code: 'CU30', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu31-consultar-libros-contables', name: 'Consultar Libros Contables', code: 'CU31', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] }
    ]
  },
  {
    id: 'tesoreria-y-cajas',
    name: 'Tesorería y Cajas',
    icon: '▣',
    functions: [
      { id: 'cu32-cuentas-bancarias-y-cajas', name: 'Gestionar Cuentas Bancarias y Cajas', code: 'CU32', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO'] },
      { id: 'cu33-cuentas-por-cobrar-cxc', name: 'Gestionar Cuentas por Cobrar (CxC)', code: 'CU33', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO'] },
      { id: 'cu34-cuentas-por-pagar-cxp', name: 'Gestionar Cuentas por Pagar (CxP)', code: 'CU34', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu35-arqueos-y-conciliaciones', name: 'Gestionar Arqueos y Conciliaciones Bancarias', code: 'CU35', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'CAJERO'] }
    ]
  },
  {
    id: 'reportes-y-bi',
    name: 'Reportes',
    icon: '◰',
    functions: [
      { id: 'cu36-reportes-declaraciones-fiscales', name: 'Reportes y Declaraciones Fiscales', code: 'CU36', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu37-estados-financieros-basicos', name: 'Estados Financieros Básicos', code: 'CU37', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] },
      { id: 'cu38-reportes-operativos-gestion', name: 'Reportes Operativos y de Gestión', code: 'CU38', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN', 'VENDEDOR', 'ALMACENERO', 'CAJERO'] },
      { id: 'cu39-dashboard-analitico-kpis', name: 'Dashboard Analítico y KPIs', code: 'CU39', rolesPermitidos: ['ADMINISTRADOR', 'ADMIN', 'SUPERADMIN'] }
    ]
  }
];
