import { EstadoPeriodo, PeriodoContable } from '../models/periodo-contable.model';
import { accionDisponible, fechaCorta, gestionesDisponibles, mesesFaltantes } from './periodos.utils';

function periodo(mes: number, estado: EstadoPeriodo): PeriodoContable {
  return {
    id: mes,
    gestion: 2026,
    mes,
    nombre: `Mes ${mes}`,
    fechaInicio: `2026-${String(mes).padStart(2, '0')}-01`,
    fechaFin: `2026-${String(mes).padStart(2, '0')}-28`,
    estado,
    fechaCierre: null,
    cantidadAsientos: 0,
  };
}

describe('CU26 utilidades de periodos contables', () => {
  it('solo permite cerrar el primer mes abierto y reabrir el último cerrado', () => {
    const lista = [periodo(1, 'CERRADO'), periodo(2, 'CERRADO'), periodo(3, 'ABIERTO'), periodo(4, 'ABIERTO')];
    expect(lista.map((p) => accionDisponible(p, lista))).toEqual([null, 'reabrir', 'cerrar', null]);
  });

  it('no permite cerrar si falta un mes anterior', () => {
    const lista = [periodo(1, 'CERRADO'), periodo(3, 'ABIERTO')];
    expect(accionDisponible(lista[1], lista)).toBeNull();
  });

  it('ofrece cinco gestiones atrás, la siguiente y las ya existentes', () => {
    expect(gestionesDisponibles([2018], 2026)).toEqual([2027, 2026, 2025, 2024, 2023, 2022, 2021, 2018]);
  });

  it('formatea fechas y cuenta los meses faltantes', () => {
    expect(fechaCorta('2026-11-01')).toBe('01/11/2026');
    expect(fechaCorta('2026-11-17T10:20:00')).toBe('17/11/2026');
    expect(fechaCorta(null)).toBe('—');
    expect(mesesFaltantes([periodo(1, 'ABIERTO'), periodo(2, 'ABIERTO')])).toBe(10);
  });
});
