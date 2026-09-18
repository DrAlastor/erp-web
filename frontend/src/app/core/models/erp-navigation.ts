export interface ErpModule {
  id: string;
  name: string;
  icon: string;
  functions: { id: string; name: string }[];
}
const definitions = [
  [
    'seguridad-y-auditoria',
    'Seguridad y Auditoría',
    '◎',
    'Acceso al Sistema|Gestión de Usuarios|Roles y Permisos|Indicadores y Auditoría',
  ],
  [
    'inventario-y-almacenes',
    'Inventario y Almacenes',
    '▦',
    'Catálogo de Artículos|Movimientos de Inventario|Despachos de Venta',
  ],
  [
    'comercial-y-preventa',
    'Comercial y Preventa',
    '↗',
    'Gestión de Clientes|Perfil Personal|Precios y Descuentos|Cotizaciones|Pedidos de Venta|Validación Comercial|Ventas y Facturación|Anulación y Registro de Ventas',
  ],
  ['compras-y-proveedores', 'Compras y Proveedores', '⇄', ''],
  [
    'contabilidad-e-impuestos',
    'Contabilidad e Impuestos',
    '≡',
    'Plan de Cuentas|Asientos Contables|Integración Contable|Cuentas por Cobrar|Información Financiera',
  ],
  ['caja-y-arqueo', 'Caja y Arqueo', '▣', 'Caja y Cierre Diario'],
];
export const ERP_MODULES: ErpModule[] = definitions.map(([id, name, icon, functions]) => ({
  id,
  name,
  icon,
  functions: functions
    ? functions.split('|').map((name) => ({
        name,
        id: name
          .normalize('NFD')
          .replace(/[\u0300-\u036f]/g, '')
          .toLowerCase()
          .replace(/ /g, '-'),
      }))
    : [],
}));
