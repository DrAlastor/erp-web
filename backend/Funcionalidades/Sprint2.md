## HU-19 — Registrar Venta, Facturación y Anulación (Ventas y Facturación)

**Responsable:** Nicolas

**Módulo Backend:** `com.erp.backend.modulo.ventas_y_facturacion`

**Módulo Frontend:** `ventas_y_facturacion`

**Prioridad:** Alta

### Descripción Funcional

Permite consolidar la venta de productos, la emisión del comprobante fiscal oficial (factura legal SIN o nota de venta), la recepción de cobros en múltiples formas de pago (o crédito), el descuento automático en inventario con registro en Kardex, la generación de asientos contables automáticos y el proceso de anulación con reversión integral.

### Flujo Principal

1. **Emisión de Venta y Facturación Legal:**
* Endpoint `POST /api/ventas` (Requiere `VENTAS:FACTURACION:ESCRITURA`).
* El cliente envía la solicitud con `sucursal_id`, `cliente_id`, `cajero_id`, `turno_caja_id`, `tipo_venta` (`CONTADO` / `CREDITO`), detalles de productos y pagos.
* Obtiene la secuencia activa de la tabla `secuencia_facturacion`, asigna el `numero_factura` correlativo, recupera `numero_autorizacion`, `leyenda_sin` y calcula el `codigo_control`.
* Registra el encabezado en `ventas` calculando de forma exacta:

$$\text{Subtotal} = \sum (\text{Cantidad} \times \text{Precio Unitario} - \text{Descuento})$$


$$\text{IVA (13\%)} = \text{Total} \times 0.13 \quad \vert{} \quad \text{IT (3\%)} = \text{Total} \times 0.03$$


* Inserta las líneas del comprobante en `venta_detalles`.


2. **Procesamiento de Pagos o Cuentas por Cobrar:**
* **Si `tipo_venta = 'CONTADO'`:** Registra una o más formas de pago en `pago_venta` (`forma_pago_id`, `importe`, `transaccion_externa`), validando que $\sum \text{Importes} = \text{Total Venta}$.
* **Si `tipo_venta = 'CREDITO'`:** Inserta automáticamente el plan de deuda en `cuentas_por_cobrar` asignando `monto_total = Total Venta`, `monto_cobrado = 0.00`, `saldo = Total Venta` y la `fecha_vencimiento`.


3. **Efectos Secundarios Automáticos (Invento y Contabilidad):**
* **Inventario:** Actualiza la tabla `inventario_sucursal` decrementando el `stock_disponible` de cada producto.
* **Kardex:** Registra una entrada por ítem en `kardex_movimientos` con `tipo_movimiento = 'SALIDA'`, motivo `'Venta según Factura'`, vinculando el `usuario_id` y actualizando `saldo_cantidad` y `saldo_valor`.
* **Asiento Contable Automático:** Inserta en `asientos_contables` y `asiento_detalles` el comprobante de diario:
* **Debe:** Cuenta Disponibilidades / Caja - Banco (o Cuentas por Cobrar) por el Total + Cuenta IT Gasto (3%).
* **Haber:** Venta de Productos / Ingreso (87%) + Débito Fiscal IVA (13%) + IT por Pagar (3%).




4. **Anulación de Venta:**
* Endpoint `PATCH /api/ventas/{id}/anular` (Requiere `VENTAS:FACTURACION:ESCRITURA`).
* Cambia el campo `estado` en la tabla `ventas` a `'ANULADA'`.
* Restablece el stock incrementando `stock_disponible` en `inventario_sucursal`.
* Registra en `kardex_movimientos` la entrada compensatoria (`tipo_movimiento = 'ENTRADA'`) con motivo `'Anulación de Factura'`.
* Genera el asiento contable de reversión neutralizando el débito fiscal, IT e ingreso.



### Tablas Implicadas

| Tabla | Operación | Motivo |
| --- | --- | --- |
| `ventas` | Inserción y Modificación | Almacena el encabezado de la venta, datos fiscales, cálculo de IVA/IT y estado. |
| `venta_detalles` | Inserción | Registra los artículos, cantidades, precios unitarios y descuentos aplicados. |
| `pago_venta` | Inserción | Registra el desglose de importes cobrados según la forma de pago (Efectivo, QR, Tarjeta). |
| `secuencia_facturacion` | Lectura y Modificación | Obtención de dosificación SIN y actualización del `numero_actual`. |
| `inventario_sucursal` | Modificación | Actualización transaccional del stock disponible por sucursal. |
| `kardex_movimientos` | Inserción | Registro del movimiento de salida valorado en el historial físico. |
| `cuentas_por_cobrar` | Inserción | Creación de la cuenta pendiente si la venta se registra a crédito. |
| `asientos_contables` | Inserción | Registro del comprobante contable automático en el Libro Diario. |
| `asiento_detalles` | Inserción | Desglose de partidas dobles (Debe y Haber) según el plan de cuentas. |

### Dependencias y Conexiones

* **Dependencias Backend:** Ejecutado por `VentaController`, procesado de forma transaccional (`@Transactional`) por `VentaService` e integrado con `KardexService` y `ContabilidadService`.
* **Dependencias Frontend:** Vista `ventas.component.ts` o módulo POS dentro de `/ventas_y_facturacion/facturacion`.

---

## HU-20 — Gestionar Devoluciones y Notas de Crédito (Ventas y Facturación)

**Responsable:** Nicolas

**Módulo Backend:** `com.erp.backend.modulo.ventas_y_facturacion`

**Módulo Frontend:** `ventas_y_facturacion`

**Prioridad:** Media

### Descripción Funcional

Permite procesar devoluciones parciales o totales de mercadería vendida previamente mediante la emisión formal de una Nota de Crédito. Restituye los productos devueltos al inventario, genera el reajuste contable del débito/crédito fiscal e impacta el saldo de la cuenta por cobrar o genera la devolución del importe al cliente.

### Flujo Principal

1. **Búsqueda y Selección de Venta:**
* Endpoint `GET /api/ventas/buscar` (Requiere `VENTAS:DEVOLUCIONES:LECTURA`).
* Permite consultar ventas activas (`estado = 'EMITIDA'`) mediante `numero_factura` o `nit_cliente`.
* Carga el detalle desde `venta_detalles` proyectando las cantidades originalmente compradas y las cantidades previamente devueltas.


2. **Registro de la Devolución y Nota de Crédito:**
* Endpoint `POST /api/notas-credito` (Requiere `VENTAS:DEVOLUCIONES:ESCRITURA`).
* Recibe `venta_id`, `motivo` (ej. `'CAMBIO_PRODUCTO'`, `'DEFECTUOSO'`), arreglo de productos a devolver con sus cantidades y `emitido_por` (ID de usuario).
* Valida la regla de negocio: $\text{Cantidad a Devolver} \le \text{Cantidad Vendida Original} - \text{Devoluciones Anteriores}$.
* Inserta la cabecera en `notas_credito` generando un `numero_nota` único y estableciendo `estado = 'EMITIDA'`.
* Inserta las líneas en `notas_credito_detalles` enlazando con `venta_detalle_id` y calculando el subtotal devuelto.


3. **Reingreso a Inventario (Kardex):**
* Incrementa en `inventario_sucursal` el `stock_disponible` según los productos y cantidades devueltas.
* Registra una fila en `kardex_movimientos` con `tipo_movimiento = 'ENTRADA'`, `motivo = 'Devolución de cliente segun Nota de Crédito'`, actualizando el saldo físico y valorado en la sucursal.


4. **Ajuste Financiero y Reversión Contable:**
* **Ajuste en Cuentas por Cobrar / Tesorería:** Si la venta fue a crédito, actualiza `cuentas_por_cobrar` reduciendo el `saldo`. Si fue al contado, genera un egreso en `movimientos_caja` del turno activo por el reembolso otorgado.
* **Asiento Contable de Ajuste Fiscal:** Genera automáticamente el asiento en `asientos_contables` y `asiento_detalles`:
* **Debe:** Devoluciones sobre Ventas / Egreso (87%) + Crédito Fiscal IVA (13%).
* **Haber:** Cuentas por Cobrar (o Caja/Bancos).





### Tablas Implicadas

| Tabla | Operación | Motivo |
| --- | --- | --- |
| `notas_credito` | Inserción y Lectura | Almacena el encabezado de la nota de crédito, cliente, motivo y monto total. |
| `notas_credito_detalles` | Inserción | Detalle de artículos devueltos, cantidades y subtotales. |
| `ventas` | Lectura | Consulta y validación del estado y datos de la venta original. |
| `venta_detalles` | Lectura | Verificación de cantidades máximas permitidas para devolución. |
| `inventario_sucursal` | Modificación | Retorno e incremento del stock devuelto a la sucursal correspondiente. |
| `kardex_movimientos` | Inserción | Registro del movimiento de entrada por devolución en el historial de almacén. |
| `cuentas_por_cobrar` | Modificación | Recálculo y disminución del saldo deudor del cliente (si aplica). |
| `movimientos_caja` | Inserción | Registro del egreso de dinero en caja en devoluciones de ventas al contado. |
| `asientos_contables` | Inserción | Registro contable automático para el ajuste fiscal y devolución. |

### Dependencias y Conexiones

* **Dependencias Backend:** Controlado por `NotaCreditoController`, procesado por `NotaCreditoService` e interconectado con `InventarioService`, `CajaService` y `ContabilidadService`.
* **Dependencias Frontend:** Vista `devoluciones.component.ts` ubicada en la ruta `/ventas_y_facturacion/devoluciones`.