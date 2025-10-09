package org.example.service.report;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.domain.ReportEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PoiReportRepository implements ReportRepository using Apache POI.
 * It handles creation of the report file, reading existing BRnums, and appending new report rows.
 */

public class PoiReportRepository implements ReportRepository {

    @Override
    public void ensureReport(Path reportFile) {
        try {
            Files.createDirectories(reportFile.getParent());
            if (Files.exists(reportFile)) return;

            try (Workbook wb = new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet("Report");
                Row header = sheet.createRow(0);


                // header font
                Font headerFont = wb.createFont();
                headerFont.setFontName("Arial");
                headerFont.setFontHeightInPoints((short) 14);
                headerFont.setBold(true);

                // header styling
                CellStyle headerStyle = wb.createCellStyle();
                headerStyle.setFont(headerFont);

                // header background
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // header borders
                headerStyle.setBorderBottom(BorderStyle.THIN);
                headerStyle.setBorderTop(BorderStyle.THIN);
                headerStyle.setBorderLeft(BorderStyle.THIN);
                headerStyle.setBorderRight(BorderStyle.THIN);

                String[] headers = {"BRnum", "URL", "URL Used", "Status", "Reason", "Error"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerStyle);
                }

                // auto-size kolonner
                for (int i = 0; i < headers.length; i++) {
                    sheet.autoSizeColumn(i);
                }

                try (OutputStream os = Files.newOutputStream(reportFile)) {
                    wb.write(os);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to ensure report file", e);
        }

    }

    @Override
    public Set<String> loadExistingBRnums(Path reportFile) {
        Set<String> result = new HashSet<>();
        if (!Files.exists(reportFile)) return result;

        try (InputStream is = Files.newInputStream(reportFile);
             Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheet("Report");
            if (sheet == null) sheet = wb.getSheetAt(0);

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                Cell c = row.getCell(0);
                if (c == null) continue;
                String br = c.getStringCellValue();
                if (br != null && !br.isBlank()) result.add(br.trim());
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load BRnums", e);
        }
    }

    @Override
    public void append(Path reportFile, List<ReportEntity> entries) {
        try (InputStream is = Files.newInputStream(reportFile);
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheet("Report");
            if (sheet == null) sheet = wb.getSheetAt(0);
            int last = sheet.getLastRowNum();

            for (ReportEntity entry : entries) {
                Row row = sheet.createRow(++last);
                row.createCell(0).setCellValue(entry.getBRnum() != null ? entry.getBRnum() : "");
                row.createCell(1).setCellValue(entry.getUrl() != null ? entry.getUrl().toString() : "");
                row.createCell(2).setCellValue(entry.getUrlUsed() != null ? entry.getUrlUsed() : "");
                row.createCell(3).setCellValue(entry.getStatus() != null ? entry.getStatus() : "");
                row.createCell(4).setCellValue(entry.getReason() != null ? entry.getReason() : "");
                row.createCell(5).setCellValue(entry.getErrorMessage() != null ? entry.getErrorMessage() : "");
            }

            for (int c = 0; c < 6; c++) {
                sheet.autoSizeColumn(c);
            }

            try (OutputStream os = Files.newOutputStream(reportFile)) {
                wb.write(os);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to append to report", e);
        }
    }
}
