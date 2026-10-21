import { Cotizacion } from '../models/cotizacion.model';

const bs = (n: number) =>
  Number(n).toLocaleString('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const escapar = (texto: string) =>
  texto.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!);

/**
 * Abre la proforma en una ventana aparte y lanza la impresión del navegador, desde donde
 * se puede guardar como PDF. Se arma aparte para no imprimir el menú ni el resto del panel.
 */
export function imprimirProforma(c: Cotizacion): void {
  const filas = c.items
    .map(
      (i) => `<tr>
        <td>${escapar(i.productoCodigo)}</td>
        <td>${escapar(i.productoNombre)}</td>
        <td class="n">${bs(i.cantidad)}</td>
        <td class="n">${bs(i.precioUnitario)}</td>
        <td class="n">${bs(i.descuento)}</td>
        <td class="n">${bs(i.subtotal)}</td>
      </tr>`,
    )
    .join('');

  const html = `<!doctype html><html lang="es"><head><meta charset="utf-8">
  <title>Proforma ${escapar(c.numeroCotizacion)}</title>
  <style>
    body { font-family: Arial, sans-serif; color: #1a1a1a; margin: 32px; font-size: 12px; }
    h1 { font-size: 20px; margin: 0; }
    .cab { display: flex; justify-content: space-between; border-bottom: 2px solid #1f3864; padding-bottom: 12px; }
    .meta { text-align: right; line-height: 1.6; }
    .cliente { margin: 18px 0; line-height: 1.6; }
    table { width: 100%; border-collapse: collapse; }
    th { background: #deebf7; text-align: left; }
    th, td { border: 1px solid #bfbfbf; padding: 6px 8px; }
    .n { text-align: right; white-space: nowrap; }
    .totales { margin-top: 12px; margin-left: auto; width: 280px; }
    .totales td { border: none; padding: 3px 8px; }
    .totales .t td { font-weight: bold; font-size: 14px; border-top: 2px solid #1f3864; }
    .nota { margin-top: 24px; color: #555; font-size: 11px; }
  </style></head><body>
  <div class="cab">
    <div><h1>PROFORMA</h1><div>Cotización de venta</div></div>
    <div class="meta">
      <strong>${escapar(c.numeroCotizacion)}</strong><br>
      Fecha: ${escapar(c.fecha)}<br>
      Válida hasta: ${escapar(c.fechaVencimiento)}
    </div>
  </div>
  <div class="cliente">
    <strong>Cliente:</strong> ${escapar(c.clienteRazonSocial)}<br>
    <strong>NIT/CI:</strong> ${escapar(c.clienteNitCi)}<br>
    <strong>Vendedor:</strong> ${escapar(c.vendedor)}
  </div>
  <table>
    <thead><tr><th>Código</th><th>Producto</th><th class="n">Cantidad</th>
      <th class="n">Precio Bs</th><th class="n">Desc. Bs</th><th class="n">Subtotal Bs</th></tr></thead>
    <tbody>${filas}</tbody>
  </table>
  <table class="totales">
    <tr><td>Subtotal</td><td class="n">${bs(c.subtotal)}</td></tr>
    <tr><td>Descuentos</td><td class="n">- ${bs(c.descuento)}</td></tr>
    <tr class="t"><td>Total Bs</td><td class="n">${bs(c.total)}</td></tr>
  </table>
  ${c.observaciones ? `<p class="nota"><strong>Observaciones:</strong> ${escapar(c.observaciones)}</p>` : ''}
  <p class="nota">Precios con IVA incluido. Esta proforma no reserva mercadería; la
  disponibilidad se confirma al emitir el pedido.</p>
  </body></html>`;

  const ventana = window.open('', '_blank', 'width=900,height=700');
  if (!ventana) return;
  ventana.document.open();
  ventana.document.write(html);
  ventana.document.close();
  ventana.focus();
  ventana.print();
}
