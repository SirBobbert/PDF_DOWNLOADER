package org.example.service.reader;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PoiExcelReader is the Apache POI implementaion of ExcelReader.
 * It extracts BRnum, Pdf_URL and optional Html_URL from the given Excel sheet.
 */

public class PoiExcelReader implements ExcelReader {

    private static final String COL_BRNUM = "BRnum";
    private static final String COL_PDF = "Pdf_URL";
    private static final String COL_HTML = "Report Html Address";


    @Override
    public List<InputRow> readRows(Path excelPath) {
        try (InputStream is = Files.newInputStream(excelPath);
             Workbook wb = new XSSFWorkbook((is))) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() <= 1) return List.of();

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return List.of();

            int brCol = findColumnIndex(headerRow, COL_BRNUM);
            int pdfCol = findColumnIndex(headerRow, COL_PDF);
            int htmlCol = findColumnIndex(headerRow, COL_HTML);

            if (pdfCol == -1) return List.of();

            DataFormatter fmt = new DataFormatter();
            List<InputRow> rows = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowBlank(row)) continue;

                String br = brCol >= 0 ? fmt.formatCellValue(row.getCell(brCol)).trim() : null;

                String pdfStr = fmt.formatCellValue(row.getCell(pdfCol)).trim();
                String htmlStr = htmlCol >= 0 ? fmt.formatCellValue(row.getCell(htmlCol)).trim() : "";

                URL pdfUrl = urlSafe(pdfStr);
                URL htmlUrl = urlSafe(htmlStr);

                rows.add(new InputRow(r + 1, (br == null || br.isBlank()) ? null : br, pdfUrl, htmlUrl));
            }
            return rows;
        } catch (IOException e) {
            throw new RuntimeException("Faield to read Excel file", e);
        }
    }

    private int findColumnIndex(Row header, String target) {
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell c = header.getCell(i);
            if (c != null && target.equalsIgnoreCase(c.getStringCellValue())) return i;
        }
        return -1;
    }

    private boolean isRowBlank(Row row) {
        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell c = row.getCell(i);
            if (c != null && c.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private URL urlSafe(String value) {
        try {
            return (value == null || value.isBlank()) ? null : new URL(value);
        } catch (Exception e) {
            return null;
        }
    }
}


