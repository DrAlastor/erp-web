CU38 - Constructor de Reportes QBE (Query By Example)
MÓDULO 7 — Reportes & BI

Descripcion: El Administrador arma reportes sin SQL: elige una entidad del negocio, los
campos (en el orden de las columnas), calculos por columna (suma, promedio, conteo, minimo,
maximo), filtros combinados con AND/OR y el orden. Ve la vista previa paginada, la exporta a
PDF/XLSX y la guarda como plantilla reutilizable.

Endpoints (permiso REPORTES_MODIFICAR: solo Administrador)
  GET    /api/reportes/qbe/catalogo                  Entidades, campos, operadores, agregaciones.
  POST   /api/reportes/qbe/vista-previa?pagina&tamanio   DefinicionQbe -> pagina del resultado.
  POST   /api/reportes/qbe/exportar?formato=XLSX|PDF&nombre  DefinicionQbe -> archivo.
  GET    /api/reportes/qbe/plantillas                Plantillas guardadas.
  GET    /api/reportes/qbe/plantillas/{id}
  POST   /api/reportes/qbe/plantillas                { nombre, descripcion, definicion }
  PUT    /api/reportes/qbe/plantillas/{id}
  DELETE /api/reportes/qbe/plantillas/{id}
  GET    /api/reportes/qbe/plantillas/{id}/exportar?formato=XLSX|PDF

Agrupacion (como la fila "Total" de un QBE): si alguna columna tiene calculo, las columnas sin
calculo pasan a ser el GROUP BY. Las fechas se pueden mostrar por dia, mes o año.

Seguridad: CatalogoEntidadesQbe es la unica fuente de SQL (tablas, JOIN y expresion de cada
campo). Operadores, agregaciones y granularidades son enums con su fragmento fijo, el orden
se indica por posicion de columna y todo valor del usuario viaja como parametro tipado.

Tabla REPORTES_QBE (existente, sin cambios):
  TABLA_BASE           codigo de la entidad (VENTAS, KARDEX_MOVIMIENTOS, ...)
  MODULO               modulo de la entidad
  CAMPOS_SELECCIONADOS claves de las columnas separadas por coma
  FILTROS_APLICADOS    JSON con descripcion, columnas, filtros, operador logico y orden.
                       Las filas antiguas con texto plano ("ESTADO = EMITIDA") se leen igual.

Exportacion: Apache POI (XLSX) y OpenPDF (PDF), declaradas en build.gradle. Maximo 5000 filas.
