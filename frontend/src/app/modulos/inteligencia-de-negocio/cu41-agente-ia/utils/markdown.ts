/**
 * CU41 — Markdown MÍNIMO y seguro para las respuestas del asistente.
 *
 * Convierte el texto en una estructura de bloques y fragmentos que la plantilla de Angular dibuja
 * con elementos propios: nunca se inserta HTML (sin innerHTML), así que un texto como
 * `<img onerror=...>` se muestra literal y no se ejecuta.
 * Soporta: párrafos, títulos (#), listas (-, *, 1.), tablas con |, **negrita**, *cursiva* y `código`.
 */

export type Fragmento = { tipo: 'texto' | 'negrita' | 'cursiva' | 'codigo'; texto: string };

export type Bloque =
  | { tipo: 'parrafo'; partes: Fragmento[] }
  | { tipo: 'titulo'; partes: Fragmento[] }
  | { tipo: 'lista'; ordenada: boolean; items: Fragmento[][] }
  | { tipo: 'tabla'; encabezado: Fragmento[][]; filas: Fragmento[][][] };

const PATRON = /(\*\*[^*]+\*\*|`[^`]+`|\*[^*\s][^*]*\*|_[^_\s][^_]*_)/g;

export function fragmentos(linea: string): Fragmento[] {
  const salida: Fragmento[] = [];
  let ultimo = 0;
  for (const m of linea.matchAll(PATRON)) {
    const i = m.index ?? 0;
    if (i > ultimo) salida.push({ tipo: 'texto', texto: linea.slice(ultimo, i) });
    const t = m[0];
    if (t.startsWith('**')) salida.push({ tipo: 'negrita', texto: t.slice(2, -2) });
    else if (t.startsWith('`')) salida.push({ tipo: 'codigo', texto: t.slice(1, -1) });
    else salida.push({ tipo: 'cursiva', texto: t.slice(1, -1) });
    ultimo = i + t.length;
  }
  if (ultimo < linea.length) salida.push({ tipo: 'texto', texto: linea.slice(ultimo) });
  return salida;
}

const celdas = (linea: string): string[] =>
  linea.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map((c) => c.trim());

const esSeparador = (linea: string): boolean => /^\s*\|?\s*:?-{2,}:?\s*(\|\s*:?-{2,}:?\s*)*\|?\s*$/.test(linea);

export function bloques(texto: string): Bloque[] {
  const lineas = (texto ?? '').replace(/\r\n/g, '\n').split('\n');
  const salida: Bloque[] = [];
  let parrafo: string[] = [];
  const cerrarParrafo = () => {
    if (parrafo.length) salida.push({ tipo: 'parrafo', partes: fragmentos(parrafo.join(' ')) });
    parrafo = [];
  };

  for (let i = 0; i < lineas.length; i++) {
    const linea = lineas[i];
    const limpia = linea.trim();
    if (!limpia) {
      cerrarParrafo();
      continue;
    }
    // Tabla: fila con | seguida de separador ---
    if (limpia.includes('|') && i + 1 < lineas.length && esSeparador(lineas[i + 1])) {
      cerrarParrafo();
      const encabezado = celdas(limpia).map(fragmentos);
      const filas: Fragmento[][][] = [];
      i += 2;
      while (i < lineas.length && lineas[i].trim().includes('|')) {
        filas.push(celdas(lineas[i]).map(fragmentos));
        i++;
      }
      i--;
      salida.push({ tipo: 'tabla', encabezado, filas });
      continue;
    }
    const titulo = /^#{1,6}\s+(.*)$/.exec(limpia);
    if (titulo) {
      cerrarParrafo();
      salida.push({ tipo: 'titulo', partes: fragmentos(titulo[1]) });
      continue;
    }
    const item = /^([-*•]|\d+[.)])\s+(.*)$/.exec(limpia);
    if (item) {
      cerrarParrafo();
      const ordenada = /^\d/.test(item[1]);
      const previo = salida[salida.length - 1];
      if (previo && previo.tipo === 'lista' && previo.ordenada === ordenada) previo.items.push(fragmentos(item[2]));
      else salida.push({ tipo: 'lista', ordenada, items: [fragmentos(item[2])] });
      continue;
    }
    parrafo.push(limpia);
  }
  cerrarParrafo();
  return salida;
}
