import { CuentaNodo, FilaCuenta, FiltroCuentas } from '../models/plan-cuentas.model';

const CODIGO = /^\d+(\.\d+)*$/;

export function codigoValido(codigo: string): boolean {
  return CODIGO.test(codigo);
}

export function nivel(codigo: string): number {
  return codigo.split('.').length;
}

/** Si `hijo` cuelga directamente de `padre` (un segmento más). */
export function esHijoDirecto(padre: string, hijo: string): boolean {
  return hijo.startsWith(`${padre}.`) && nivel(hijo) === nivel(padre) + 1;
}

export function hayFiltro(f: FiltroCuentas): boolean {
  return !!f.texto.trim() || !!f.tipo || !!f.activo;
}

function coincide(nodo: CuentaNodo, f: FiltroCuentas): boolean {
  const c = nodo.cuenta;
  const texto = f.texto.trim().toLowerCase();
  if (texto && !c.codigo.toLowerCase().includes(texto) && !c.nombre.toLowerCase().includes(texto)) {
    return false;
  }
  if (f.tipo && c.tipo !== f.tipo) return false;
  if (f.activo && String(c.activo) !== f.activo) return false;
  return true;
}

/**
 * Aplana el árbol en filas visibles. Sin filtro se respetan las cuentas expandidas; con filtro
 * se muestran las que coinciden junto con sus ancestros, todo expandido.
 */
export function aplanar(
  nodos: readonly CuentaNodo[],
  expandidas: ReadonlySet<number>,
  filtro: FiltroCuentas,
): FilaCuenta[] {
  const filas: FilaCuenta[] = [];
  const filtrando = hayFiltro(filtro);

  const visitar = (nodo: CuentaNodo, profundidad: number): FilaCuenta[] => {
    const propias: FilaCuenta[] = [];
    const expandida = filtrando || expandidas.has(nodo.cuenta.id);
    const descendientes: FilaCuenta[] = [];
    if (expandida || filtrando) {
      for (const hijo of nodo.hijos) descendientes.push(...visitar(hijo, profundidad + 1));
    }
    if (!filtrando || coincide(nodo, filtro) || descendientes.length) {
      propias.push({
        cuenta: nodo.cuenta,
        profundidad,
        expandible: nodo.hijos.length > 0,
        expandida,
      });
      propias.push(...descendientes);
    }
    return propias;
  };

  for (const raiz of nodos) filas.push(...visitar(raiz, 0));
  return filas;
}

/** Busca un nodo por id en todo el árbol. */
export function buscarNodo(nodos: readonly CuentaNodo[], id: number): CuentaNodo | null {
  for (const n of nodos) {
    if (n.cuenta.id === id) return n;
    const encontrado = buscarNodo(n.hijos, id);
    if (encontrado) return encontrado;
  }
  return null;
}

/**
 * Sugiere el código de la siguiente subcuenta: último segmento más uno, con el mismo ancho que
 * los hermanos (o dos dígitos desde el cuarto nivel, como 1.1.1.01).
 */
export function siguienteCodigoHijo(padre: CuentaNodo): string {
  const base = padre.cuenta.codigo;
  const segmentos = padre.hijos
    .map((h) => h.cuenta.codigo)
    .filter((c) => esHijoDirecto(base, c))
    .map((c) => c.slice(base.length + 1));
  const ancho = segmentos.length
    ? Math.max(...segmentos.map((s) => s.length))
    : nivel(base) >= 3
      ? 2
      : 1;
  const siguiente = segmentos.length ? Math.max(...segmentos.map(Number)) + 1 : 1;
  return `${base}.${String(siguiente).padStart(ancho, '0')}`;
}

/** Siguiente código raíz libre (ej. "6" si existen 1 a 5). */
export function siguienteCodigoRaiz(nodos: readonly CuentaNodo[]): string {
  const raices = nodos.map((n) => n.cuenta.codigo).filter((c) => /^\d+$/.test(c)).map(Number);
  return String(raices.length ? Math.max(...raices) + 1 : 1);
}

/** Todos los ids con hijos, para "expandir todo". */
export function idsExpandibles(nodos: readonly CuentaNodo[]): number[] {
  const ids: number[] = [];
  const visitar = (n: CuentaNodo) => {
    if (n.hijos.length) {
      ids.push(n.cuenta.id);
      n.hijos.forEach(visitar);
    }
  };
  nodos.forEach(visitar);
  return ids;
}
