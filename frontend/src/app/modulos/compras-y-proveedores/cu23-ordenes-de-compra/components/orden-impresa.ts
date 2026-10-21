import { OrdenCompra } from '../models/orden-compra.model';

const bs = (n: number) =>
  Number(n).toLocaleString('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const escapar = (texto: string | null | undefined) =>
  (texto ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);

/**
 * Abre la orden de compra en una ventana aparte y lanza la impresión del navegador, desde
 * donde se guarda como PDF para mandarla al proveedor. Igual que la proforma de CU15.
 */
export function imprimirOrdenCompra(o: OrdenCompra): void {
  const filas = o.detalles
    .map(
      (d) => `<tr>
        <td>${escapar(d.codigo)}</td>
        <td>${escapar(d.producto)}</td>
        <td class="n">${bs(d.cantidad)}</td>
        <td class="n">${bs(d.costoUnitario)}</td>
        <td class="n">${bs(d.subtotal)}</td>
      </tr>`,
    )
    .join('');
  const condicion =
    o.proveedorCondicionPago === 'CREDITO' ? `Crédito a ${o.proveedorDiasCredito ?? 0} días` : 'Contado';

  const html = `<!doctype html><html lang="es"><head><meta charset="utf-8">
  <title>Orden de compra ${escapar(o.numeroOrden)}</title>
  <style>
    body { font-family: Arial, sans-serif; color: #1a1a1a; margin: 32px; font-size: 12px; }
    h1 { font-size: 20px; margin: 0; }
    .cab { display: flex; justify-content: space-between; border-bottom: 2px solid #1f3864; padding-bottom: 12px; }
    .meta { text-align: right; line-height: 1.6; }
    .prov { margin: 18px 0; line-height: 1.6; }
    table { width: 100%; border-collapse: collapse; }
    th { background: #deebf7; text-align: left; }
    th, td { border: 1px solid #bfbfbf; padding: 6px 8px; }
    .n { text-align: right; white-space: nowrap; }
    .totales { margin-top: 12px; margin-left: auto; width: 280px; }
    .totales td { border: none; padding: 3px 8px; }
    .totales .t td { font-weight: bold; font-size: 14px; border-top: 2px solid #1f3864; }
    .nota { margin-top: 24px; color: #555; font-size: 11px; white-space: pre-line; }
    .firmas { display: flex; gap: 60px; margin-top: 70px; }
    .firmas div { flex: 1; border-top: 1px solid #333; text-align: center; padding-top: 6px; }
  </style></head><body>
  <div class="cab">
    <div><h1>ORDEN DE COMPRA</h1><div>Estado: ${escapar(o.estado)}</div></div>
    <div class="meta">
      <strong>${escapar(o.numeroOrden)}</strong><br>
      Fecha: ${escapar(o.fecha)}<br>
      Entrega esperada: ${escapar(o.fechaEntrega) || 'a coordinar'}
    </div>
  </div>
  <div class="prov">
    <strong>Proveedor:</strong> ${escapar(o.proveedor)}<br>
    <strong>NIT/CI:</strong> ${escapar(o.proveedorNit)}<br>
    <strong>Condición de pago:</strong> ${condicion}<br>
    <strong>Solicitada por:</strong> ${escapar(o.creadoPor)}
  </div>
  <table>
    <thead><tr><th>Código</th><th>Producto</th><th class="n">Cantidad</th>
      <th class="n">Costo unit. Bs</th><th class="n">Subtotal Bs</th></tr></thead>
    <tbody>${filas}</tbody>
  </table>
  <table class="totales">
    <tr><td>Subtotal</td><td class="n">${bs(o.subtotal)}</td></tr>
    <tr><td>Descuento</td><td class="n">- ${bs(o.descuento)}</td></tr>
    <tr class="t"><td>Total Bs</td><td class="n">${bs(o.total)}</td></tr>
  </table>
  ${o.observaciones ? `<p class="nota"><strong>Observaciones:</strong>\n${escapar(o.observaciones)}</p>` : ''}
  <div class="firmas"><div>Solicitado por</div><div>Aprobado por</div><div>Recibido por el proveedor</div></div>
  </body></html>`;

  const ventana = window.open('', '_blank', 'width=900,height=700');
  if (!ventana) return;
  ventana.document.open();
  ventana.document.write(html);
  ventana.document.close();
  ventana.focus();
  ventana.print();
}
