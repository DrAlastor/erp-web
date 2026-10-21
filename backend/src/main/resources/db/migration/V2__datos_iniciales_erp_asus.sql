-- ===================================================================
-- Datos iniciales de la empresa de referencia (ASUS Bolivia), copiados de
-- "Base de Datos/ERP_Asus.txt": módulos, funciones, roles, usuarios,
-- catálogos y movimientos de ejemplo para desarrollo local.
-- ===================================================================

-- ============================================================================
-- POBLACIÓN DE DATOS INICIALES: ASUS BOLIVIA TECH SOLUTIONS (ERP_TENANT)
-- ============================================================================

-- 1. CONFIGURACIÓN Y CATÁLOGOS BASE
INSERT INTO CONFIGURACION_EMPRESA (TEMA, COLOR_PRIMARIO, COLOR_SECUNDARIO, LOGO_PERSONALIZADO_URL, ENCABEZADO_FACTURA, PIE_FACTURA, FECHA_MODIFICACION) VALUES 
(NULL,NULL,NULL,'https://storage.nexoerp.com/logos/asus_rog.png','ASUS BOLIVIA TECH SOLUTIONS S.R.L. - NIT: 3049182019 - Santa Cruz, Bolivia','Gracias por su compra. Garantia oficial ASUS de 12 meses contra defectos de fabrica.','2026-01-15 09:30:00');

INSERT INTO CIUDAD (NOMBRE, PAIS) VALUES 
('Santa Cruz de la Sierra','Bolivia'),
('La Paz','Bolivia'),
('Cochabamba','Bolivia'),
('Sucre','Bolivia');

INSERT INTO SUCURSAL (CIUDAD_ID, NOMBRE, DIRECCION, TELEFONO, HORA_APERTURA, HORA_CIERRE, ACTIVO) VALUES 
(1,'Sucursal Central Equipetrol','Av. San Martin esq. Calle 4 Oeste #100','33458900','08:30:00','19:30:00',TRUE),
(2,'Sucursal Calacoto La Paz','Av. Ballivian entre Calles 12 y 13 #750','22789400','09:00:00','19:00:00',TRUE);

INSERT INTO MODULO (NOMBRE) VALUES 
('Configuracion y Seguridad'),
('Catalogo e Inventarios'),
('Ventas y Facturacion'),
('Compras y Proveedores'),
('Contabilidad y Finanzas'),
('Tesoreria y Cajas'),
('Reportes');

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


INSERT INTO ROL (NOMBRE, PERMISO, ACTIVO) VALUES 
('ADMINISTRADOR','ALL',TRUE),
('VENDEDOR','VENTAS_ONLY',TRUE),
('CAJERO','CAJA_ONLY',TRUE),
('ALMACENERO','INVENTARIO_ONLY',TRUE);

INSERT INTO ROL_FUNCION (ROL_ID, FUNCION_ID, DESCRIPCION) VALUES 
-- ROL 1: ADMINISTRADOR (Acceso de Edición completo a todos los CUs)
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

INSERT INTO EMPLEADOS (SUCURSAL_ID, CI, NOMBRE, APELLIDO, CARGO, TELEFONO, EMAIL, DIRECCION, FECHA_INGRESO, ACTIVO) VALUES 
(1,'5342981','Carlos','Roca Aguilera','Gerente General','77312345','carlos.roca@asustech.bo','Barrio Sirari Calle Los Claveles #45','2026-01-15',TRUE),
(1,'6891234','Alejandro','Paz Vaca','Ejecutivo de Ventas Gamer','77098765','alejandro.paz@asustech.bo','Av. Banzer 4to Anillo #230','2026-01-16',TRUE),
(1,'7123456','Laura','Morales Justiniano','Cajera Principal','76654321','laura.morales@asustech.bo','Av. Busch Calle 8 #12','2026-01-16',TRUE),
(1,'8234567','Roberto','Vaca Mendez','Jefe de Almacen y Logistica','78543210','roberto.vaca@asustech.bo','Av. Santos Dumont 3er Anillo #55','2026-01-16',TRUE);

INSERT INTO USUARIO (ROL_ID, EMPLEADO_ID, EMAIL, PASSWORD, NIVEL_USUARIO, FECHA_CREACION, ACTIVO) VALUES 
(1,1,'admin@asustech.bo','$2a$10$3nm30eUQJQA9mIUfQ1QL5OKesCXJeqePEUTGMBxEsv9YcpZkD8A5K',1,'2026-01-15 09:30:00',TRUE),
(2,2,'ventas@asustech.bo','$2a$10$P.TZv0p1bjzznYvpeUipS.LEdg7HfojWWnpMqfq3uF0DHdtEDvF6O',2,'2026-01-16 08:30:00',TRUE),
(3,3,'caja@asustech.bo','$2a$10$clHW79UoOASv3N5jbGuPk.n6gSAaA6zdgW6lpCD/1l0xzubYpr4f6',3,'2026-01-16 08:30:00',TRUE),
(4,4,'almacen@asustech.bo','$2a$10$2aRKjcWPH9/It6d.9ZihNOzTXkloFVGto8e7N2udoVkNsrEWkgOa2',2,'2026-01-16 08:30:00',TRUE);

INSERT INTO NOTIFICACIONES (USUARIO_ID, TITULO, MENSAJE, TIPO, FECHA_CREACION, LEIDO) VALUES 
(1,'Bienvenido a NexoERP','Instancia aprovisionada exitosamente para ASUS Bolivia Tech Solutions.','INFO','2026-01-15 09:35:00',FALSE),
(1,'Alerta de Stock Minimo','El producto Laptop ASUS TUF Gaming A15 ha alcanzado el nivel de reposicion.','ALERTA','2026-01-17 11:30:00',FALSE);

-- 2. INVENTARIOS Y CATALOGO DE HARDWARE
INSERT INTO UNIDADES_MEDIDA (CODIGO, NOMBRE, ABREVIATURA, ACTIVO) VALUES 
('UND','Unidad','u',TRUE),
('KIT','Kit o Combo','kit',TRUE),
('CAJ','Caja Master','caj',TRUE);

INSERT INTO CATEGORIAS (NOMBRE, DESCRIPCION, PADRE_ID, ACTIVO, FECHA_CREACION) VALUES 
('Laptops & Portatiles','Equipos portatiles ASUS',NULL,TRUE,'2026-01-15 09:00:00'),
('Laptops Gaming ROG & TUF','Portatiles de alto rendimiento para gaming',1,TRUE,'2026-01-15 09:00:00'),
('Ultrabooks Zenbook & Vivobook','Portatiles delgados para productividad',1,TRUE,'2026-01-15 09:00:00'),
('Componentes de Computadora','Hardware interno para ensamble de PC',NULL,TRUE,'2026-01-15 09:00:00'),
('Placas Madre (Motherboards)','Placas base ASUS ROG, TUF y Prime',4,TRUE,'2026-01-15 09:00:00'),
('Tarjetas de Video (GPU)','Graficas NVIDIA GeForce RTX series ASUS',4,TRUE,'2026-01-15 09:00:00'),
('Almacenamiento SSD M.2','Discos de estado solido NVMe PCIe 4.0/5.0',4,TRUE,'2026-01-15 09:00:00'),
('Memorias RAM DDR5','Modulos de memoria de alta velocidad',4,TRUE,'2026-01-15 09:00:00'),
('Monitores & Perifericos','Pantallas ROG Swift y accesorios gamer',NULL,TRUE,'2026-01-15 09:00:00');

INSERT INTO PRODUCTOS (CATEGORIA_ID, UNIDAD_MEDIDA_ID, CODIGO, NOMBRE, DESCRIPCION, PRECIO_VENTA, COSTO_COMPRA, STOCK_MINIMO, STOCK_MAXIMO, IMAGEN_URL, ACTIVO, FECHA_CREACION) VALUES 
(2,1,'LAP-ROG-SCAR16','Laptop ASUS ROG Strix SCAR 16 G634','Intel Core i9-13980HX, RTX 4080 12GB, 32GB DDR5, 1TB SSD NVMe, QHD 240Hz',24500.00,19800.00,2,10,'https://storage.nexoerp.com/products/rog_scar16.png',TRUE,'2026-01-16 10:00:00'),
(2,1,'LAP-TUF-A15','Laptop ASUS TUF Gaming A15 FA507','AMD Ryzen 7 7735HS, RTX 4060 8GB, 16GB DDR5, 512GB SSD NVMe, FHD 144Hz',10500.00,8200.00,3,15,'https://storage.nexoerp.com/products/tuf_a15.png',TRUE,'2026-01-16 10:00:00'),
(3,1,'LAP-ZEN-14','Laptop ASUS Zenbook 14 OLED UX3405','Intel Core Ultra 7 155H, 16GB LPDDR5X, 1TB SSD NVMe, 14 3K OLED 120Hz',11200.00,8900.00,2,10,'https://storage.nexoerp.com/products/zenbook_14.png',TRUE,'2026-01-16 10:00:00'),
(5,1,'MB-ROG-Z790E','Placa Madre ASUS ROG Strix Z790-E Gaming WiFi II','LGA1700, DDR5, PCIe 5.0, WiFi 7, 5x M.2, Audio ALC4080',4200.00,3350.00,3,12,'https://storage.nexoerp.com/products/rog_z790e.png',TRUE,'2026-01-16 10:00:00'),
(6,1,'GPU-TUF-4070TIS','Tarjeta de Video ASUS TUF Gaming RTX 4070 Ti SUPER 16GB','16GB GDDR6X, DLSS 3, Axial-tech fans, Armor Metal',8900.00,7200.00,2,8,'https://storage.nexoerp.com/products/tuf_4070tis.png',TRUE,'2026-01-16 10:00:00'),
(7,1,'SSD-FUR-2TB','Disco SSD Kingston FURY Renegade 2TB M.2 NVMe','Lectura 7300 MB/s, Escritura 7000 MB/s con disipador',1450.00,1100.00,5,25,'https://storage.nexoerp.com/products/fury_2tb.png',TRUE,'2026-01-16 10:00:00'),
(8,1,'RAM-FURY-32G','Memoria RAM Kingston FURY Beast DDR5 32GB (2x16GB) 6000MHz','Kit 32GB Dual Channel, Intel XMP 3.0 & AMD EXPO',1150.00,880.00,5,30,'https://storage.nexoerp.com/products/fury_ddr5.png',TRUE,'2026-01-16 10:00:00'),
(9,1,'MON-ROG-PG27','Monitor Gamer ASUS ROG Swift OLED PG27AQDM 27','OLED 2K QHD (2560x1440), 240Hz, 0.03ms, G-Sync',9800.00,7900.00,2,6,'https://storage.nexoerp.com/products/rog_pg27.png',TRUE,'2026-01-16 10:00:00');

INSERT INTO INVENTARIO_SUCURSAL (SUCURSAL_ID, PRODUCTO_ID, STOCK_DISPONIBLE, STOCK_MINIMO, ULTIMA_ACTUALIZACION) VALUES 
(1,1,5,2,'2026-01-16 11:00:00'),
(1,2,8,3,'2026-01-16 11:00:00'),
(1,3,6,2,'2026-01-16 11:00:00'),
(1,4,8,3,'2026-01-16 11:00:00'),
(1,5,5,2,'2026-01-16 11:00:00'),
(1,6,15,5,'2026-01-16 11:00:00'),
(1,7,20,5,'2026-01-16 11:00:00'),
(1,8,4,2,'2026-01-16 11:00:00'),
(2,1,3,1,'2026-01-16 11:00:00'),
(2,2,4,2,'2026-01-16 11:00:00'),
(2,5,3,1,'2026-01-16 11:00:00'),
(2,6,8,3,'2026-01-16 11:00:00');

INSERT INTO KARDEX_MOVIMIENTOS (SUCURSAL_ID, PRODUCTO_ID, UNIDAD_MEDIDA_ID, USUARIO_ID, TIPO_MOVIMIENTO, MOTIVO, DOCUMENTO_REFERENCIA, CANTIDAD, COSTO_UNITARIO, SALDO_CANTIDAD, SALDO_VALOR, FECHA_MOVIMIENTO) VALUES 
(1,1,1,4,'ENTRADA','Inventario Inicial de Apertura','DOC-APERTURA-001',5,19800.00,5,99000.00,'2026-01-16 11:00:00'),
(1,2,1,4,'ENTRADA','Inventario Inicial de Apertura','DOC-APERTURA-001',8,8200.00,8,65600.00,'2026-01-16 11:00:00'),
(1,5,1,4,'ENTRADA','Inventario Inicial de Apertura','DOC-APERTURA-001',5,7200.00,5,36000.00,'2026-01-16 11:00:00'),
(1,6,1,4,'ENTRADA','Inventario Inicial de Apertura','DOC-APERTURA-001',15,1100.00,15,16500.00,'2026-01-16 11:00:00'),
(1,7,1,4,'ENTRADA','Inventario Inicial de Apertura','DOC-APERTURA-001',20,880.00,20,17600.00,'2026-01-16 11:00:00');

-- 3. TESORERIA Y CAJAS
INSERT INTO FORMAS_PAGO (CODIGO, NOMBRE, ACTIVO, FECHA_CREACION) VALUES 
('EFECTIVO','Efectivo Moneda Nacional (BOB)',TRUE,'2026-01-15 08:00:00'),
('TARJETA','Tarjeta de Debito / Credito POS',TRUE,'2026-01-15 08:00:00'),
('QR_SIMPLE','Pago Facil QR Simple BNB / BCP',TRUE,'2026-01-15 08:00:00'),
('TRANSFERENCIA','Transferencia Bancaria Directa',TRUE,'2026-01-15 08:00:00'),
('PASARELA_ONLINE','Pasarela Digital Libelula / Stripe',TRUE,'2026-01-15 08:00:00');

INSERT INTO TURNOS_CAJA (SUCURSAL_ID, CAJERO_ID, MONTO_APERTURA, MONTO_CIERRE, DIFERENCIA, FECHA_APERTURA, FECHA_CIERRE, ESTADO, OBSERVACIONES) VALUES 
(1,3,1000.00,NULL,NULL,'2026-01-17 08:30:00',NULL,'ABIERTO','Turno de caja manana en Sucursal Equipetrol');

INSERT INTO MOVIMIENTOS_CAJA (TURNO_CAJA_ID, FORMA_PAGO_ID, TIPO, CONCEPTO, MONTO, NUMERO_COMPROBANTE, FECHA_HORA) VALUES 
(1,1,'INGRESO','Fondo inicial de cambio de turno',1000.00,'ING-001','2026-01-17 08:30:00');

-- 4. COMPRAS Y PROVEEDORES
INSERT INTO PROVEEDORES (RAZON_SOCIAL, NIT_CI, CONTACTO, TELEFONO, EMAIL, DIRECCION, CIUDAD, CONDICION_PAGO, DIAS_CREDITO, ACTIVO, FECHA_CREACION) VALUES 
('ASUS COMPUTER INTERNATIONAL LATAM','9081726354','Roberto Chen','+13055928800','latam_orders@asus.com','800 Corporate Way, Fremont, CA, USA','Miami / Fremont','CREDITO',30,TRUE,'2026-01-15 10:00:00'),
('TECH DATA MAYORISTA BOLIVIA S.R.L.','1029384756','Claudia Saucedo','33445566','pedidos@techdata.bo','4to Anillo esq. Av. San Aurelio #120','Santa Cruz de la Sierra','CREDITO',15,TRUE,'2026-01-15 10:00:00'),
('KINGSTON TECHNOLOGY LATIN AMERICA CORP','8192039485','Fernando Gomez','+13058899000','sales_bolivia@kingston.com','Fountain Valley, CA, USA','California','CONTADO',0,TRUE,'2026-01-15 10:00:00');

INSERT INTO ORDENES_COMPRA (PROVEEDOR_ID, CREADO_POR, NUMERO_ORDEN, FECHA, FECHA_ENTREGA, SUBTOTAL, DESCUENTO, TOTAL, ESTADO, OBSERVACIONES, FECHA_CREACION) VALUES 
(1,1,'OC-2026-0001','2026-01-16','2026-01-20',55400.00,0.00,55400.00,'APROBADA','Pedido de reposicion de laptops ROG y placas Z790','2026-01-16 11:30:00');

INSERT INTO ORDEN_COMPRA_DETALLES (ORDEN_COMPRA_ID, PRODUCTO_ID, CANTIDAD, COSTO_UNITARIO, SUBTOTAL) VALUES 
(1,1,2,19800.00,39600.00),
(1,4,4,3350.00,13400.00),
(1,6,2,1200.00,2400.00);

INSERT INTO COMPRAS (PROVEEDOR_ID, SUCURSAL_ID, USUARIO_ID, NUMERO_FACTURA, NUMERO_AUTORIZACION, FECHA, SUBTOTAL, IVA, TOTAL, TIPO_PAGO, ESTADO, OBSERVACIONES, FECHA_CREACION) VALUES 
(2,1,1,'FC-88902','39485029384','2026-01-16',26800.00,3484.00,26800.00,'CREDITO','COMPLETADA','Compra de componentes Kingston Fury (RAM y SSDs)','2026-01-16 14:00:00');

INSERT INTO COMPRA_DETALLES (COMPRA_ID, PRODUCTO_ID, CANTIDAD, COSTO_UNITARIO, SUBTOTAL) VALUES 
(1,6,10,1100.00,11000.00),
(1,7,18,877.78,15800.00);

INSERT INTO CUENTAS_POR_PAGAR (COMPRA_ID, PROVEEDOR_ID, CREADO_POR, NUMERO_CUOTA, MONTO_TOTAL, MONTO_PAGADO, SALDO, FECHA_VENCIMIENTO, ESTADO, FECHA_CREACION) VALUES 
(1,2,1,1,26800.00,10000.00,16800.00,'2026-01-31','PENDIENTE','2026-01-16 14:05:00');

INSERT INTO PAGOS_PROVEEDOR (CUENTA_POR_PAGAR_ID, FORMA_PAGO_ID, USUARIO_ID, MONTO, FECHA_PAGO, NUMERO_RECIBO, TRANSACCION_EXTERNA, OBSERVACIONES, FECHA_CREACION) VALUES 
(1,4,1,10000.00,'2026-01-16','REC-PAG-001','TRANSF-BNB-9988221','Abono inicial mediante transferencia bancaria BNB','2026-01-16 14:30:00');

-- 5. COMERCIAL, VENTAS Y FACTURACION
INSERT INTO CLIENTES (RAZON_SOCIAL, NIT_CI, TELEFONO, EMAIL, DIRECCION, CIUDAD, CONDICION_PAGO, LIMITE_CREDITO, DIAS_CREDITO, ACTIVO, FECHA_CREACION) VALUES 
('INNOVATECH SOLUTIONS BOLIVIA S.R.L.','4928172013','33448899','compras@innovatech.bo','Av. Marcelo Quiroga Santa Cruz #300','Santa Cruz de la Sierra','CREDITO',50000.00,30,TRUE,'2026-01-16 12:00:00'),
('ESTUDIO DE ARQUITECTURA & RENDER 3D S.R.L.','3819201019','33551122','finanzas@renderbox.bo','Calle Rene Moreno #140','Santa Cruz de la Sierra','CONTADO',0.00,0,TRUE,'2026-01-16 12:00:00'),
('JUAN CARLOS TORREZ MENDEZ','6283910','77011223','jctorrez@gmail.com','Barrio Las Palmas Calle 3 #45','Santa Cruz de la Sierra','CONTADO',0.00,0,TRUE,'2026-01-16 12:00:00');

INSERT INTO LISTAS_PRECIOS (CODIGO, NOMBRE, DESCRIPCION, ES_PRINCIPAL, ACTIVO) VALUES 
('LISTA-PUBLICO','Precio Publico General','Precio de venta para clientes finales en tienda',TRUE,TRUE),
('LISTA-CORPORATIVO','Precio Corporativo y Mayorista','Descuento especial por volumen para empresas registradas',FALSE,TRUE);

INSERT INTO PRECIOS_PRODUCTO (LISTA_PRECIO_ID, PRODUCTO_ID, PRECIO, FECHA_ACTUALIZACION) VALUES 
(1,1,24500.00,'2026-01-16 12:30:00'),
(1,2,10500.00,'2026-01-16 12:30:00'),
(1,5,8900.00,'2026-01-16 12:30:00'),
(1,6,1450.00,'2026-01-16 12:30:00'),
(1,7,1150.00,'2026-01-16 12:30:00'),
(2,1,23200.00,'2026-01-16 12:30:00'),
(2,2,9900.00,'2026-01-16 12:30:00'),
(2,5,8400.00,'2026-01-16 12:30:00'),
(2,6,1320.00,'2026-01-16 12:30:00'),
(2,7,1050.00,'2026-01-16 12:30:00');

INSERT INTO DESCUENTOS (NOMBRE, TIPO, VALOR, FECHA_INICIO, FECHA_FIN, ACTIVO) VALUES 
('Descuento Lanzamiento ROG','PORCENTAJE',5.00,'2026-01-01','2026-02-28',TRUE),
('Promo Combo Ensamble PC','MONTO_FIJO',200.00,'2026-01-01','2026-03-31',TRUE);

INSERT INTO SECUENCIA_FACTURACION (NUMERO_AUTORIZACION, LEYENDA_SIN, NUMERO_INICIAL, NUMERO_FINAL, NUMERO_ACTUAL, FECHA_LIMITE, ACTIVO) VALUES 
('7904018273645','Ley N 453: Los servicios deben prestarse en condiciones de inocuidad y calidad.',1,10000,1002,'2026-12-31',TRUE);

INSERT INTO IMPUESTOS (CODIGO, NOMBRE, PORCENTAJE, ACTIVO) VALUES 
('IVA','Impuesto al Valor Agregado',13.00,TRUE),
('IT','Impuesto a las Transacciones',3.00,TRUE);

INSERT INTO COTIZACIONES (CLIENTE_ID, VENDEDOR_ID, NUMERO_COTIZACION, FECHA, VALIDEZ_DIAS, SUBTOTAL, DESCUENTO, TOTAL, ESTADO, OBSERVACIONES, FECHA_CREACION) VALUES 
(1,2,'COT-2026-0001','2026-01-16',15,33400.00,1000.00,32400.00,'ACEPTADA','Cotizacion para estacion de trabajo de renderizado','2026-01-16 15:00:00');

INSERT INTO COTIZACION_DETALLES (COTIZACION_ID, PRODUCTO_ID, CANTIDAD, PRECIO_UNITARIO, DESCUENTO, SUBTOTAL) VALUES 
(1,1,1,24500.00,1000.00,23500.00),
(1,5,1,8900.00,0.00,8900.00);

INSERT INTO PEDIDOS (CLIENTE_ID, VENDEDOR_ID, SUCURSAL_ID, NUMERO_PEDIDO, FECHA, SUBTOTAL, DESCUENTO, TOTAL, ESTADO, OBSERVACIONES, FECHA_CREACION) VALUES 
(1,2,1,'PED-2026-0001','2026-01-17',33400.00,1000.00,32400.00,'COMPLETADO','Confirmacion en base a Cotizacion COT-2026-0001','2026-01-17 09:00:00');

INSERT INTO PEDIDO_DETALLES (PEDIDO_ID, PRODUCTO_ID, CANTIDAD, PRECIO_UNITARIO, DESCUENTO, SUBTOTAL) VALUES 
(1,1,1,24500.00,1000.00,23500.00),
(1,5,1,8900.00,0.00,8900.00);

INSERT INTO NOTAS_DESPACHO (PEDIDO_ID, SUCURSAL_ID, DESPACHADO_POR, NUMERO_DESPACHO, FECHA, CONDUCTOR, PLACA, ESTADO, OBSERVACIONES) VALUES 
(1,1,4,'DESP-2026-0001','2026-01-17 10:00:00','Marcos Justiniano','4029-XTR','ENTREGADO','Despacho en camioneta corporativa a oficinas de Innovatech');

INSERT INTO DESPACHO_DETALLES (DESPACHO_ID, PRODUCTO_ID, CANTIDAD, OBSERVACIONES) VALUES 
(1,1,1,'Serie: G634-998822 - Impecable estado de embalaje'),
(1,5,1,'Serie: TUF-4070-112233 - Precinto de seguridad original');

INSERT INTO VENTAS (SUCURSAL_ID, CLIENTE_ID, CAJERO_ID, TURNO_CAJA_ID, SECUENCIA_FACTURA_ID, PEDIDO_ID, NUMERO_FACTURA, NUMERO_AUTORIZACION, CODIGO_CONTROL, NIT_CLIENTE, RAZON_SOCIAL_CLIENTE, FECHA, SUBTOTAL, DESCUENTO, IVA, IT, TOTAL, TIPO_VENTA, ESTADO, FECHA_CREACION) VALUES 
(1,1,3,1,1,1,1001,'7904018273645','A1-F4-89-C2','4928172013','INNOVATECH SOLUTIONS BOLIVIA S.R.L.','2026-01-17 10:30:00',33400.00,1000.00,4212.00,972.00,32400.00,'CONTADO','EMITIDA','2026-01-17 10:30:00'),
(1,3,3,1,1,NULL,1002,'7904018273645','B3-E2-11-D4','6283910','JUAN CARLOS TORREZ MENDEZ','2026-01-17 11:15:00',1450.00,0.00,188.50,43.50,1450.00,'CONTADO','EMITIDA','2026-01-17 11:15:00');

INSERT INTO VENTA_DETALLES (VENTA_ID, PRODUCTO_ID, DESCUENTO_ID, CANTIDAD, PRECIO_UNITARIO, DESCUENTO, SUBTOTAL) VALUES 
(1,1,1,1,24500.00,1000.00,23500.00),
(1,5,NULL,1,8900.00,0.00,8900.00),
(2,6,NULL,1,1450.00,0.00,1450.00);

INSERT INTO PAGO_VENTA (VENTA_ID, FORMA_PAGO_ID, IMPORTE, TRANSACCION_EXTERNA, FECHA_PAGO, ESTADO) VALUES 
(1,4,32400.00,'TRANSF-BCP-8839201','2026-01-17 10:30:00','COMPLETADO'),
(2,3,1450.00,'QR-BNB-PAY-443322','2026-01-17 11:15:00','COMPLETADO');

INSERT INTO NOTAS_CREDITO (VENTA_ID, CLIENTE_ID, SUCURSAL_ID, EMITIDO_POR, NUMERO_NOTA, FECHA, MOTIVO, TOTAL, ESTADO, OBSERVACIONES, FECHA_CREACION) VALUES 
(2,3,1,3,'NC-2026-0001','2026-01-17','CAMBIO_PRODUCTO',0.00,'ANULADA','Nota informativa por soporte tecnico','2026-01-17 12:00:00');

INSERT INTO NOTAS_CREDITO_DETALLES (NOTA_CREDITO_ID, PRODUCTO_ID, VENTA_DETALLE_ID, CANTIDAD, PRECIO_UNITARIO, SUBTOTAL) VALUES 
(1,6,3,0,1450.00,0.00);

-- 6. CONTABILIDAD GENERAL Y FINANZAS
INSERT INTO PERIODOS_CONTABLES (GESTION, MES, NOMBRE, FECHA_INICIO, FECHA_FIN, ESTADO, FECHA_CIERRE) VALUES 
(2026,1,'Enero 2026','2026-01-01','2026-01-31','ABIERTO',NULL);

INSERT INTO CUENTAS_CONTABLES (CODIGO, NOMBRE, NIVEL, TIPO, NATURALEZA, PERMITE_MOVIMIENTO, PADRE_ID, ACTIVO) VALUES 
('1','ACTIVO',1,'ACTIVO','DEUDORA',FALSE,NULL,TRUE),
('1.1','ACTIVO CORRIENTE',2,'ACTIVO','DEUDORA',FALSE,1,TRUE),
('1.1.1','DISPONIBILIDADES',3,'ACTIVO','DEUDORA',FALSE,2,TRUE),
('1.1.1.01','Caja Moneda Nacional',4,'ACTIVO','DEUDORA',TRUE,3,TRUE),
('1.1.1.02','Banco BNB Cuenta Corriente',4,'ACTIVO','DEUDORA',TRUE,3,TRUE),
('1.1.3','INVENTARIOS',3,'ACTIVO','DEUDORA',FALSE,2,TRUE),
('1.1.3.01','Inventario de Mercaderia Tecnologia',4,'ACTIVO','DEUDORA',TRUE,6,TRUE),
('2','PASIVO',1,'PASIVO','ACREEDORA',FALSE,NULL,TRUE),
('2.1','PASIVO CORRIENTE',2,'PASIVO','ACREEDORA',FALSE,8,TRUE),
('2.1.1.01','Cuentas por Pagar Proveedores',3,'PASIVO','ACREEDORA',TRUE,9,TRUE),
('3','PATRIMONIO',1,'PATRIMONIO','ACREEDORA',FALSE,NULL,TRUE),
('3.1.1.01','Capital Social Aportado',2,'PATRIMONIO','ACREEDORA',TRUE,11,TRUE),
('4','INGRESOS',1,'INGRESO','ACREEDORA',FALSE,NULL,TRUE),
('4.1.1.01','Ventas de Productos Tecnologia',2,'INGRESO','ACREEDORA',TRUE,13,TRUE),
('5','EGRESOS',1,'EGRESO','DEUDORA',FALSE,NULL,TRUE),
('5.1.1.01','Costo de Mercaderia Vendida',2,'EGRESO','DEUDORA',TRUE,15,TRUE);

INSERT INTO ASIENTOS_CONTABLES (PERIODO_CONTABLE_ID, CREADO_POR, NUMERO_ASIENTO, FECHA, TIPO_ASIENTO, GLOSA, TOTAL_DEBE, TOTAL_HABER, ESTADO, FECHA_CREACION) VALUES 
(1,1,'AS-2026-0001','2026-01-15','APERTURA','Balance de apertura gestion ASUS Tech Bolivia',500000.00,500000.00,'MAYORIZADO','2026-01-15 09:00:00'),
(1,1,'AS-2026-0002','2026-01-17','VENTAS','Ventas de mercaderia segun facturas 1001 y 1002',33850.00,33850.00,'MAYORIZADO','2026-01-17 12:00:00');

INSERT INTO ASIENTO_DETALLES (ASIENTO_ID, CUENTA_CONTABLE_ID, GLOSA, DEBE, HABER) VALUES 
(1,5,'Aporte en cuenta bancaria BNB',261500.00,0.00),
(1,7,'Stock inicial de mercaderia valorada',238500.00,0.00),
(1,12,'Capital social fundacional ASUS Tech',0.00,500000.00),
(2,5,'Ingreso a bancos por venta factura 1001',32400.00,0.00),
(2,4,'Ingreso a caja por venta factura 1002',1450.00,0.00),
(2,14,'Venta neta mercaderia tecnologia',0.00,33850.00);

INSERT INTO CUENTAS_POR_COBRAR (VENTA_ID, CLIENTE_ID, SUCURSAL_ID, CREADO_POR, NUMERO_CUOTA, MONTO_TOTAL, MONTO_COBRADO, SALDO, FECHA_VENCIMIENTO, ESTADO, FECHA_CREACION) VALUES 
(1,1,1,3,1,32400.00,32400.00,0.00,'2026-01-17','PAGADO','2026-01-17 10:30:00');

INSERT INTO COBROS (CUENTA_POR_COBRAR_ID, FORMA_PAGO_ID, TURNO_CAJA_ID, USUARIO_ID, NUMERO_RECIBO, TRANSACCION_EXTERNA, MONTO, FECHA_COBRO, OBSERVACIONES, FECHA_CREACION) VALUES 
(1,4,1,3,'REC-COB-001','TRANSF-BCP-8839201',32400.00,'2026-01-17','Liquidacion total de venta corporativa','2026-01-17 10:35:00');

-- 7. REPORTES Y BUSINESS INTELLIGENCE
INSERT INTO REPORTES_QBE (USUARIO_ID, NOMBRE, MODULO, TABLA_BASE, CAMPOS_SELECCIONADOS, FILTROS_APLICADOS, FECHA_CREACION) VALUES 
(1,'Reporte de Ventas por Vendedor','Ventas','VENTAS','NUMERO_FACTURA,FECHA,TOTAL,CAJERO_ID','ESTADO = EMITIDA','2026-01-17 13:00:00'),
(1,'Kardex Valorizado ROG','Inventarios','KARDEX_MOVIMIENTOS','PRODUCTO_ID,CANTIDAD,SALDO_CANTIDAD,SALDO_VALOR','TIPO_MOVIMIENTO = ENTRADA','2026-01-17 13:00:00');