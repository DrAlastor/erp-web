package modulo.reportes_y_bi.cu38_constructor_reportes_qbe.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.catalogo.TipoCampo;
import modulo.reportes_y_bi.cu38_constructor_reportes_qbe.dto.ResultadoQbeResponse.ColumnaResultado;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Exportación del resultado de un reporte QBE a Excel (XLSX, con Apache POI) y PDF (con
 * OpenPDF). Recibe columnas y filas ya calculadas: no consulta la base.
 */
@Component
public class ExportadorReporteQbe {

    /** Encabezado del archivo: nombre del reporte y líneas de contexto (filtros, fecha, etc.). */
    public record Encabezado(String titulo, List<String> lineas) {}

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Color PRIMARIO = new Color(37, 99, 235);

    public byte[] xlsx(Encabezado encabezado, List<ColumnaResultado> columnas, List<Map<String, Object>> filas) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Reporte");

            org.apache.poi.ss.usermodel.Font negrita = libro.createFont();
            negrita.setBold(true);
            org.apache.poi.ss.usermodel.Font tituloFuente = libro.createFont();
            tituloFuente.setBold(true);
            tituloFuente.setFontHeightInPoints((short) 14);
            org.apache.poi.ss.usermodel.Font blanca = libro.createFont();
            blanca.setBold(true);
            blanca.setColor(IndexedColors.WHITE.getIndex());

            CellStyle estiloTitulo = libro.createCellStyle();
            estiloTitulo.setFont(tituloFuente);
            CellStyle estiloCabecera = libro.createCellStyle();
            estiloCabecera.setFont(blanca);
            estiloCabecera.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            estiloCabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle estiloMoneda = libro.createCellStyle();
            estiloMoneda.setDataFormat(libro.createDataFormat().getFormat("#,##0.00"));
            CellStyle estiloNumero = libro.createCellStyle();
            estiloNumero.setDataFormat(libro.createDataFormat().getFormat("#,##0.##"));
            CellStyle estiloFecha = libro.createCellStyle();
            estiloFecha.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle estiloFechaHora = libro.createCellStyle();
            estiloFechaHora.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));

            int r = 0;
            Cell titulo = hoja.createRow(r++).createCell(0);
            titulo.setCellValue(encabezado.titulo());
            titulo.setCellStyle(estiloTitulo);
            for (String linea : encabezado.lineas()) {
                hoja.createRow(r++).createCell(0).setCellValue(linea);
            }
            r++;

            int filaCabecera = r;
            Row cabecera = hoja.createRow(r++);
            int[] anchos = new int[columnas.size()];
            for (int c = 0; c < columnas.size(); c++) {
                Cell celda = cabecera.createCell(c);
                celda.setCellValue(columnas.get(c).etiqueta());
                celda.setCellStyle(estiloCabecera);
                anchos[c] = columnas.get(c).etiqueta().length();
            }

            for (Map<String, Object> fila : filas) {
                Row row = hoja.createRow(r++);
                for (int c = 0; c < columnas.size(); c++) {
                    ColumnaResultado col = columnas.get(c);
                    Object valor = fila.get(col.clave());
                    if (valor == null) continue;
                    anchos[c] = Math.max(anchos[c], texto(valor, col.tipo()).length());
                    Cell celda = row.createCell(c);
                    if (valor instanceof Number n) {
                        celda.setCellValue(n.doubleValue());
                        celda.setCellStyle(col.tipo() == TipoCampo.MONEDA ? estiloMoneda : estiloNumero);
                    } else if (valor instanceof LocalDateTime t) {
                        celda.setCellValue(t);
                        celda.setCellStyle(estiloFechaHora);
                    } else if (valor instanceof LocalDate d) {
                        celda.setCellValue(d);
                        celda.setCellStyle(estiloFecha);
                    } else if (valor instanceof Boolean b) {
                        celda.setCellValue(b ? "Sí" : "No");
                    } else {
                        celda.setCellValue(valor.toString());
                    }
                }
            }

            if (!filas.isEmpty()) {
                hoja.setAutoFilter(new CellRangeAddress(filaCabecera, r - 1, 0, columnas.size() - 1));
            }
            hoja.createFreezePane(0, filaCabecera + 1);
            // Ancho por cantidad de caracteres: autoSizeColumn necesita fuentes del sistema,
            // que la imagen Alpine del contenedor no trae.
            for (int c = 0; c < columnas.size(); c++) {
                hoja.setColumnWidth(c, Math.min(anchos[c] + 3, 60) * 256);
            }
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el archivo Excel", e);
        }
    }

    public byte[] pdf(Encabezado encabezado, List<ColumnaResultado> columnas, List<Map<String, Object>> filas) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(columnas.size() > 5 ? PageSize.A4.rotate() : PageSize.A4, 28, 28, 32, 32);
        PdfWriter.getInstance(documento, salida);
        documento.open();

        Font fuenteTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, PRIMARIO);
        Font fuenteLinea = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        Font fuenteCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
        Font fuenteCelda = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.BLACK);

        documento.add(new Paragraph(encabezado.titulo(), fuenteTitulo));
        for (String linea : encabezado.lineas()) {
            documento.add(new Paragraph(linea, fuenteLinea));
        }

        PdfPTable tabla = new PdfPTable(columnas.size());
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(10);
        tabla.setHeaderRows(1);
        for (ColumnaResultado col : columnas) {
            PdfPCell celda = new PdfPCell(new Phrase(col.etiqueta(), fuenteCabecera));
            celda.setBackgroundColor(PRIMARIO);
            celda.setPadding(5);
            celda.setHorizontalAlignment(numerica(col) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            tabla.addCell(celda);
        }
        boolean par = false;
        for (Map<String, Object> fila : filas) {
            for (ColumnaResultado col : columnas) {
                PdfPCell celda = new PdfPCell(new Phrase(texto(fila.get(col.clave()), col.tipo()), fuenteCelda));
                celda.setPadding(4);
                celda.setHorizontalAlignment(numerica(col) ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                if (par) celda.setBackgroundColor(new Color(243, 244, 246));
                tabla.addCell(celda);
            }
            par = !par;
        }
        if (filas.isEmpty()) {
            PdfPCell vacia = new PdfPCell(new Phrase("No hay datos para los filtros indicados.", fuenteCelda));
            vacia.setColspan(columnas.size());
            vacia.setPadding(8);
            tabla.addCell(vacia);
        }
        documento.add(tabla);
        documento.close();
        return salida.toByteArray();
    }

    private static boolean numerica(ColumnaResultado col) {
        return col.tipo() == TipoCampo.NUMERO || col.tipo() == TipoCampo.MONEDA;
    }

    /** Texto de una celda con formato boliviano: punto de miles y coma decimal. */
    static String texto(Object valor, TipoCampo tipo) {
        if (valor == null) return "";
        if (valor instanceof LocalDateTime t) return t.format(FECHA_HORA);
        if (valor instanceof LocalDate d) return d.format(FECHA);
        if (valor instanceof Boolean b) return b ? "Sí" : "No";
        if (valor instanceof Number n) {
            DecimalFormatSymbols simbolos = new DecimalFormatSymbols(Locale.of("es", "BO"));
            simbolos.setGroupingSeparator('.');
            simbolos.setDecimalSeparator(',');
            DecimalFormat formato = new DecimalFormat(tipo == TipoCampo.MONEDA ? "#,##0.00" : "#,##0.##", simbolos);
            BigDecimal numero = new BigDecimal(n.toString()).setScale(2, RoundingMode.HALF_UP);
            return (tipo == TipoCampo.MONEDA ? "Bs. " : "") + formato.format(numero);
        }
        return valor.toString();
    }
}
