import { CuentaPorCobrar, Cobro } from '../models/cuenta-por-cobrar.model';

const bs = (n: number) => Number(n).toLocaleString('es-BO', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const escapar = (texto: string | null | undefined) =>
  (texto ?? '').replace(
    /[&<>"']/g,
    (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]!,
  );

/** Valida el monto contra el saldo, igual que el backend (que responde 409 si lo supera). */
export function errorMonto(monto: number | null, saldo: number): string {
  const valor = Number(monto) || 0;
  const m = Math.round(valor * 100);
  if (m <= 0) return 'El monto debe ser mayor a cero.';
  if (Math.abs(valor * 100 - m) > 1e-6) return 'El monto admite hasta 2 decimales.';
  if (m > Math.round(saldo * 100)) return `El monto supera el saldo pendiente (Bs ${bs(saldo)}).`;
  return '';
}

/** Texto del vencimiento: "vence en N días", "vence hoy" o "vencida hace N días". */
export function textoVencimiento(fecha: string, vencida: boolean, diasVencida: number, hoy = new Date()): string {
  if (vencida) return `vencida hace ${diasVencida} día${diasVencida === 1 ? '' : 's'}`;
  const [y, m, d] = fecha.split('-').map(Number);
  const inicioHoy = Date.UTC(hoy.getFullYear(), hoy.getMonth(), hoy.getDate());
  const dias = Math.round((Date.UTC(y, m - 1, d) - inicioHoy) / 86400000);
  if (dias === 0) return 'vence hoy';
  return dias > 0 ? `vence en ${dias} día${dias === 1 ? '' : 's'}` : '';
}

/** Saldo que quedó justo después de un cobro: total menos lo cobrado hasta ese recibo inclusive. */
export function saldoTrasCobro(cuenta: CuentaPorCobrar, cobro: Cobro): number {
  const hasta = cuenta.cobros
    .filter((x) => x.id <= cobro.id)
    .reduce((s, x) => s + Math.round(x.monto * 100), 0);
  return (Math.round(cuenta.cuota.montoTotal * 100) - hasta) / 100;
}

/**
 * Abre el recibo del cobro en una ventana aparte y lanza la impresión del navegador,
 * desde donde también se guarda como PDF. Igual que la orden de compra de la CU22.
 */
export function imprimirRecibo(cuenta: CuentaPorCobrar, cobro: Cobro): void {
  const c = cuenta.cuota;
  const extra = cobro.transaccionExterna ? ' · ' + escapar(cobro.transaccionExterna) : '';
  const obs = cobro.observaciones
    ? `<tr><td>Observaciones</td><td>${escapar(cobro.observaciones)}</td></tr>`
    : '';
  const html = `<!doctype html><html lang="es"><head><meta charset="utf-8">
<title>${escapar(cobro.numeroRecibo)}</title>
<style>
  body { font-family: system-ui, sans-serif; margin: 32px; color: #111; }
  h1 { font-size: 20px; margin: 0; }
  .sub { color: #555; margin: 2px 0 20px; }
  table { width: 100%; border-collapse: collapse; margin-top: 12px; }
  td { padding: 6px 4px; border-bottom: 1px solid #ddd; }
  td:first-child { color: #555; width: 40%; }
  .monto { font-size: 22px; font-weight: 700; }
  .firma { margin-top: 64px; border-top: 1px solid #333; width: 240px; text-align: center; padding-top: 6px; }
</style></head><body>
<h1>Recibo de cobro ${escapar(cobro.numeroRecibo)}</h1>
<p class="sub">Fecha ${escapar(cobro.fechaCobro)}</p>
<p>Recibimos de <strong>${escapar(c.clienteRazonSocial)}</strong> (NIT/CI ${escapar(c.clienteNitCi)}) la suma de</p>
<p class="monto">Bs ${bs(cobro.monto)}</p>
<table>
  <tr><td>Por concepto de</td><td>Cuota ${c.numeroCuota} de ${escapar(c.documento)}</td></tr>
  <tr><td>Forma de pago</td><td>${escapar(cobro.formaPago)}${extra}</td></tr>
  <tr><td>Total de la cuota</td><td>Bs ${bs(c.montoTotal)}</td></tr>
  <tr><td>Saldo después de este cobro</td><td>Bs ${bs(saldoTrasCobro(cuenta, cobro))}</td></tr>
  ${obs}
  <tr><td>Recibido por</td><td>${escapar(cobro.usuario)}</td></tr>
</table>
<p class="firma">Firma y sello</p>
</body></html>`;
  const ventana = window.open('', '_blank', 'width=720,height=760');
  if (!ventana) return;
  ventana.document.write(html);
  ventana.document.close();
  ventana.focus();
  ventana.print();
}
