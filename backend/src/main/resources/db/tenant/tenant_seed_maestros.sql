-- ============================================================================
-- DATOS MAESTROS OBLIGATORIOS PARA CUALQUIER TENANT ERP NUEVO
-- (Sin datos de prueba, sin clientes, sin productos, sin ventas ni compras)
-- ============================================================================

-- 1. MÓDULOS DEL SISTEMA
INSERT INTO MODULO (NOMBRE) VALUES 
('Configuracion y Seguridad'),
('Catalogo e Inventarios'),
('Ventas y Facturacion'),
('Compras y Proveedores'),
('Contabilidad y Finanzas'),
('Tesoreria y Cajas'),
('Reportes');

-- 2. FUNCIONES DEL SISTEMA
INSERT INTO FUNCION (MODULO_ID, NOMBRE) VALUES 
-- MÓDULO 1: Configuración y Seguridad (CU01 - CU08)
(1,'Gestionar Usuarios'),
(1,'Gestionar Roles y Permisos'),
(1,'Gestionar Empleados'),
(1,'Gestionar Empresas, Sucursales y Configuracion'),
(1,'Personalizar Apariencia'),
(1,'Gestionar Backups y Restore'),
(1,'Consultar Bitacora'),
-- MÓDULO 2: Catálogo e Inventarios (CU09 - CU12)
(2,'Gestionar Datos Maestros de Inventario'),
(2,'Gestionar Catalogo de Articulos'),
(2,'Gestionar Movimientos de Inventario'),
(2,'Consultar Kardex'),
-- MÓDULO 3: Ventas y Facturación (CU13 - CU20)
(3,'Gestionar Clientes'),
(3,'Gestionar Precios y Listas de Precio'),
(3,'Gestionar Cotizaciones'),
(3,'Gestionar Pedidos de Venta'),
(3,'Validacion Comercial'),
(3,'Gestionar Despacho de Venta'),
(3,'Registrar Venta, Facturacion y Anulacion'),
(3,'Gestionar Devoluciones y Notas de Credito'),
-- MÓDULO 4: Compras y Proveedores (CU21 - CU24)
(4,'Gestionar Proveedores'),
(4,'Gestionar Ordenes de Compra'),
(4,'Registrar Compra y Devolucion de Proveedores'),
(4,'Gestionar Cuentas por Pagar'),
-- MÓDULO 5: Contabilidad y Finanzas (CU25 - CU31)
(5,'Gestionar Plan de Cuentas'),
(5,'Gestionar Periodos Contables'),
(5,'Gestionar Asientos Contables'),
(5,'Integracion Contable Automatica'),
(5,'Gestionar Cuentas por Cobrar y Cobros'),
(5,'Consultar Libros Contables'),
(5,'Consultar Informacion Financiera'),
-- MÓDULO 6: Tesorería y Cajas (CU32 - CU35)
(6,'Apertura de Caja'),
(6,'Venta en Caja (POS)'),
(6,'Registrar Movimiento de Caja'),
(6,'Cierre, Arqueo y Consulta de Caja'),
-- MÓDULO 7: Reportes (CU36 - CU39)
(7,'Dashboard Ejecutivo'),
(7,'Gestionar Reportes Predefinido'),
(7,'Gestionar Reportes QBE'),
(7,'Gestionar Reportes por Voz');

-- 3. ROLES DE SISTEMA BASE
INSERT INTO ROL (NOMBRE, PERMISO, ACTIVO) VALUES 
('ADMINISTRADOR','ALL',TRUE),
('VENDEDOR','VENTAS_ONLY',TRUE),
('CAJERO','CAJA_ONLY',TRUE),
('ALMACENERO','INVENTARIO_ONLY',TRUE);

-- 4. PERMISOS POR FUNCIÓN PARA CADA ROL
INSERT INTO ROL_FUNCION (ROL_ID, FUNCION_ID, DESCRIPCION) VALUES 
-- ROL 1: ADMINISTRADOR (Edición en todos los casos de uso)
(1,1,'Edicion'),
(1,2,'Edicion'),
(1,3,'Edicion'),
(1,4,'Edicion'),
(1,5,'Edicion'),
(1,6,'Edicion'),
(1,7,'Edicion'),
(1,8,'Edicion'),
(1,9,'Edicion'),
(1,10,'Edicion'),
(1,11,'Edicion'),
(1,12,'Edicion'),
(1,13,'Edicion'),
(1,14,'Edicion'),
(1,15,'Edicion'),
(1,16,'Edicion'),
(1,17,'Edicion'),
(1,18,'Edicion'),
(1,19,'Edicion'),
(1,20,'Edicion'),
(1,21,'Edicion'),
(1,22,'Edicion'),
(1,23,'Edicion'),
(1,24,'Edicion'),
(1,25,'Edicion'),
(1,26,'Edicion'),
(1,27,'Edicion'),
(1,28,'Edicion'),
(1,29,'Edicion'),
(1,30,'Edicion'),
(1,31,'Edicion'),
(1,32,'Edicion'),
(1,33,'Edicion'),
(1,34,'Edicion'),
(1,35,'Edicion'),
(1,36,'Edicion'),
(1,37,'Edicion'),
(1,38,'Edicion'),
(1,39,'Edicion'),
-- ROL 2: VENDEDOR
(2,9,'Lectura'),
(2,11,'Lectura'),
(2,12,'Edicion'),
(2,13,'Lectura'),
(2,14,'Edicion'),
(2,15,'Edicion'),
(2,16,'Edicion'),
(2,17,'Lectura'),
(2,18,'Edicion'),
(2,19,'Lectura'),
(2,35,'Lectura'),
(2,36,'Lectura'),
-- ROL 3: CAJERO
(3,12,'Lectura'),
(3,18,'Lectura'),
(3,28,'Edicion'),
(3,31,'Edicion'),
(3,32,'Edicion'),
(3,33,'Edicion'),
(3,34,'Edicion'),
-- ROL 4: ALMACENERO
(4,8,'Lectura'),
(4,9,'Lectura'),
(4,10,'Edicion'),
(4,11,'Lectura'),
(4,17,'Edicion'),
(4,20,'Lectura'),
(4,21,'Lectura'),
(4,22,'Edicion');

-- 5. FORMAS DE PAGO ESTÁNDAR
INSERT INTO FORMAS_PAGO (CODIGO, NOMBRE, ACTIVO, FECHA_CREACION) VALUES 
('EFECTIVO','Efectivo Moneda Nacional',TRUE,CURRENT_TIMESTAMP),
('TARJETA','Tarjeta de Débito / Crédito POS',TRUE,CURRENT_TIMESTAMP),
('QR_SIMPLE','Pago Fácil Código QR',TRUE,CURRENT_TIMESTAMP),
('TRANSFERENCIA','Transferencia Bancaria Directa',TRUE,CURRENT_TIMESTAMP),
('PASARELA_ONLINE','Pasarela Digital Stripe / En Línea',TRUE,CURRENT_TIMESTAMP);

-- 6. UNIDADES DE MEDIDA BÁSICAS
INSERT INTO UNIDADES_MEDIDA (CODIGO, NOMBRE, ABREVIATURA, ACTIVO) VALUES 
('UND','Unidad','u',TRUE),
('PZA','Pieza','pza',TRUE),
('KG','Kilogramo','kg',TRUE),
('L','Litro','l',TRUE),
('M','Metro','m',TRUE),
('CAJ','Caja Master','caj',TRUE),
('DOC','Docena','doc',TRUE);

-- 7. IMPUESTOS BÁSICOS
INSERT INTO IMPUESTOS (CODIGO, NOMBRE, PORCENTAJE, ACTIVO) VALUES 
('IVA','Impuesto al Valor Agregado',13.00,TRUE),
('IT','Impuesto a las Transacciones',3.00,TRUE);
