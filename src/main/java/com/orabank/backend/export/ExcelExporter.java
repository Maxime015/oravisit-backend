package com.orabank.backend.export;

import com.orabank.backend.exception.ApiException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Ecrit un vrai classeur .xlsx (et non un CSV) a partir d'un tableau de valeurs :
 * les dates et les nombres gardent leur type a l'ouverture.
 */
@Component
@RequiredArgsConstructor
public class ExcelExporter {

    /** Le classeur est construit en memoire : au-dela, l'export est refuse. */
    private static final int MAX_ROWS = 10_000;
    /** Largeur de colonne exprimee en 1/256e de caractere, unite attendue par POI. */
    private static final int CHARACTER_WIDTH = 256;

    private final Clock clock;

    /**
     * @param subject   sujet de l'export, utilise pour l'onglet et le nom du fichier
     * @param headers   intitules des colonnes
     * @param rows      valeurs : String, Number, LocalDate, LocalDateTime ou null
     */
    public ExportedFile write(String subject, List<String> headers, List<List<Object>> rows) {
        if (rows.size() > MAX_ROWS) {
            throw ApiException.businessRule("EXPORT_TOO_LARGE",
                    "L'export dépasse %d lignes (%d %s) : affinez les filtres avant de réessayer."
                            .formatted(MAX_ROWS, rows.size(), subject));
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(subject);
            CellStyle dateTimeStyle = dataFormat(workbook, "dd/mm/yyyy hh:mm");
            CellStyle dateStyle = dataFormat(workbook, "dd/mm/yyyy");

            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = headerStyle(workbook);
            for (int column = 0; column < headers.size(); column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(headers.get(column));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(column, columnWidth(headers.get(column)));
            }

            for (int index = 0; index < rows.size(); index++) {
                Row row = sheet.createRow(index + 1);
                List<Object> values = rows.get(index);
                for (int column = 0; column < values.size(); column++) {
                    fill(row.createCell(column), values.get(column), dateTimeStyle, dateStyle);
                }
            }

            // Confort de lecture : entete figee et filtres actifs.
            sheet.createFreezePane(0, 1);
            if (!headers.isEmpty()) {
                sheet.setAutoFilter(new CellRangeAddress(0, rows.size(), 0, headers.size() - 1));
            }

            workbook.write(output);
            return new ExportedFile("%s-%s.xlsx".formatted(subject, LocalDate.now(clock)), output.toByteArray());
        } catch (IOException exception) {
            throw new UncheckedIOException("Generation du classeur impossible", exception);
        }
    }

    private void fill(Cell cell, Object value, CellStyle dateTimeStyle, CellStyle dateStyle) {
        switch (value) {
            case null -> cell.setBlank();
            case String text -> cell.setCellValue(text);
            case Number number -> cell.setCellValue(number.doubleValue());
            case LocalDateTime dateTime -> {
                cell.setCellValue(dateTime);
                cell.setCellStyle(dateTimeStyle);
            }
            case LocalDate date -> {
                cell.setCellValue(date);
                cell.setCellStyle(dateStyle);
            }
            default -> cell.setCellValue(String.valueOf(value));
        }
    }

    /** Colonne dimensionnee sur son intitule : autoSizeColumn coute trop cher sur un gros export. */
    private int columnWidth(String header) {
        return Math.clamp(header.length() + 6, 14, 40) * CHARACTER_WIDTH;
    }

    private CellStyle dataFormat(Workbook workbook, String pattern) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat(pattern));
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
