import { Cuenta, CuentaNodo, FiltroCuentas } from '../models/plan-cuentas.model';
import {
  aplanar,
  buscarNodo,
  codigoValido,
  esHijoDirecto,
  idsExpandibles,
  siguienteCodigoHijo,
  siguienteCodigoRaiz,
} from './plan-cuentas.utils';

function nodo(id: number, codigo: string, nombre: string, hijos: CuentaNodo[] = [], activo = true): CuentaNodo {
  const cuenta: Cuenta = {
    id,
    codigo,
    nombre,
    nivel: codigo.split('.').length,
    tipo: 'ACTIVO',
    naturaleza: 'DEUDORA',
    permiteMovimiento: hijos.length === 0,
    padreId: null,
    codigoPadre: null,
    activo,
    tieneHijos: hijos.length > 0,
    tieneMovimientos: false,
    cuentaIntegracion: false,
  };
  return { cuenta, hijos };
}

const SIN_FILTRO: FiltroCuentas = { texto: '', tipo: '', activo: '' };

describe('CU25 utilidades del plan de cuentas', () => {
  const caja = nodo(4, '1.1.1.01', 'Caja');
  const banco = nodo(5, '1.1.1.02', 'Banco', [], false);
  const disponible = nodo(3, '1.1.1', 'Disponibilidades', [caja, banco]);
  const corriente = nodo(2, '1.1', 'Activo Corriente', [disponible]);
  const arbol = [nodo(1, '1', 'Activo', [corriente]), nodo(9, '2', 'Pasivo')];

  it('valida códigos y la relación padre-hijo', () => {
    expect(codigoValido('1.1.1.01')).toBe(true);
    expect(codigoValido('1..1')).toBe(false);
    expect(codigoValido('1.a')).toBe(false);
    expect(esHijoDirecto('1.1', '1.1.3')).toBe(true);
    expect(esHijoDirecto('1.1', '1.10')).toBe(false);
    expect(esHijoDirecto('1.1', '1.1.3.01')).toBe(false);
  });

  it('aplana respetando las cuentas expandidas', () => {
    expect(aplanar(arbol, new Set(), SIN_FILTRO).map((f) => f.cuenta.codigo)).toEqual(['1', '2']);
    const filas = aplanar(arbol, new Set([1, 2]), SIN_FILTRO);
    expect(filas.map((f) => f.cuenta.codigo)).toEqual(['1', '1.1', '1.1.1', '2']);
    expect(filas.map((f) => f.profundidad)).toEqual([0, 1, 2, 0]);
    expect(filas[2].expandible).toBe(true);
    expect(filas[2].expandida).toBe(false);
  });

  it('con filtro muestra las coincidencias y sus ancestros', () => {
    const porTexto = aplanar(arbol, new Set(), { texto: 'caja', tipo: '', activo: '' });
    expect(porTexto.map((f) => f.cuenta.codigo)).toEqual(['1', '1.1', '1.1.1', '1.1.1.01']);

    const inactivas = aplanar(arbol, new Set(), { texto: '', tipo: '', activo: 'false' });
    expect(inactivas.map((f) => f.cuenta.codigo)).toEqual(['1', '1.1', '1.1.1', '1.1.1.02']);
  });

  it('sugiere el siguiente código respetando el ancho de los hermanos', () => {
    expect(siguienteCodigoHijo(disponible)).toBe('1.1.1.03');
    expect(siguienteCodigoHijo(corriente)).toBe('1.1.2');
    expect(siguienteCodigoHijo(nodo(20, '1.1.2', 'Exigible'))).toBe('1.1.2.01');
    expect(siguienteCodigoHijo(nodo(21, '3', 'Patrimonio'))).toBe('3.1');
    expect(siguienteCodigoRaiz(arbol)).toBe('3');
    expect(siguienteCodigoRaiz([])).toBe('1');
  });

  it('busca nodos y lista los expandibles', () => {
    expect(buscarNodo(arbol, 4)?.cuenta.codigo).toBe('1.1.1.01');
    expect(buscarNodo(arbol, 99)).toBeNull();
    expect(idsExpandibles(arbol)).toEqual([1, 2, 3]);
  });
});
