package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.domain.ReportEntity;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ConverterService {

    private final String excelFilePath;
    private final String reportSaveDir;
    private final String reportFilePath;

    private final List<ReportEntity> reportEntries = new ArrayList<>();

    public ConverterService(String excelFilePath, String reportSaveDir) {
        this.excelFilePath = excelFilePath;
        this.reportSaveDir = reportSaveDir;
        this.reportFilePath = reportSaveDir + File.separator + "URL_Report.xlsx";

    }

    // TODO: Indexes of downloaded pdfs are weird
    // TODO: Check for URL status code/is accessible
    // TODO: Check if URL is not too large
    // TODO: Check if URL is corrupted
    // TODO: Check if URL is not empty
    // TODO: Check if PDF is a duplicate
    // TODO: Check if PDF is not encrypted
    // TODO: Limit threads
    // TODO: Add threading

    public void executeProgram() {

        System.out.println("=========================================");
        System.out.println("Executing program...");

        createReport();
        getURLFromExcel();
        updateReport();
    }

    /*
    Read URLs from the specified Excel file
    The Excel file should have columns named "Pdf_URL" and optionally "Report Html Address"
    For each row, attempt to download the PDF from "Pdf_URL"
    If the download fails and "Report Html Address" is provided, attempt to download from there
    Save the downloaded PDFs to the specified directory
     */
    public void getURLFromExcel() {

        System.out.println("=========================================");
        System.out.println("Get URLS from excel file...");

        // Open the Excel file
        try (FileInputStream fis = new FileInputStream(excelFilePath)) {

            // Create Workbook instance for XLSX file
            Workbook workbook = new XSSFWorkbook(fis);

            // Get the first sheet
            Sheet sheet = workbook.getSheetAt(0);

            // check if sheet is empty
            if (sheet == null || sheet.getPhysicalNumberOfRows() < 0) return;

            // get header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return;

            // find specified columns
            Integer pdfCol = null, htmlCol = null;

            // loop through header row to find the columns (pdf and html)
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {

                // read cell value
                Cell h = headerRow.getCell(c);
                String name = h != null ? h.getStringCellValue() : null;

                // check if it matches the specified column names
                if ("Pdf_URL".equalsIgnoreCase(name)) pdfCol = c;
                if ("Report Html Address".equalsIgnoreCase(name)) htmlCol = c;
            }

            // if pdf column is not found, exit
            if (pdfCol == null) return;


            // loop through rows and process each URL
            DataFormatter formatter = new DataFormatter();

            // start from row 1 (skip header)
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {

                // get row
                Row row = sheet.getRow(r);

                // skip empty rows
                if (row == null || isRowBlank(row)) continue;

                // define URL variables
                URL pdfUrl, htmlUrl = null;
                String BRnum = null;


                //add BRnum to report - acts as an identifier
                Cell brCell = row.getCell(0);
                if (brCell != null) {
                    BRnum = formatter.formatCellValue(brCell).trim();
                    ReportEntity.builder().BRnum(BRnum).build();
                }


                // read pdf url
                Cell pdfCell = row.getCell(pdfCol);
                String pdfStr = pdfCell != null ? formatter.formatCellValue(pdfCell).trim() : "";

                // check if empty
                if (pdfStr.isEmpty()) {
                    // empty cell
                    reportEntries.add(ReportEntity.builder()
                            .BRnum(BRnum)
                            .url(null)
                            .status("error")
                            .reason("missing PDF url at row " + (r + 1))
                            .build());
                    continue;
                }
                try {
                    pdfUrl = new URL(pdfStr);

                } catch (Exception e) {
                    // invalid URL
                    reportEntries.add(ReportEntity.builder()
                            .BRnum(BRnum)
                            .urlUsed("First URL")
                            .status("error")
                            .reason("invalid PDF url at row " + (r + 1) + ": " + pdfStr)
                            .build());
                    continue;
                }

                // read html url if available
                if (htmlCol != null) {
                    Cell htmlCell = row.getCell(htmlCol);
                    String htmlStr = htmlCell != null ? formatter.formatCellValue(htmlCell).trim() : "";

                    //
                    if (!htmlStr.isEmpty()) {
                        try {
                            htmlUrl = new URL(htmlStr);
                        } catch (Exception e) {
                            System.out.println("Invalid HTML URL at row " + (r + 1) + ": " + htmlStr);
                        }
                    }
                }
                // download pdf
                downloadPDF(BRnum, pdfUrl, htmlUrl, "file" + "_" + (r + 1) + ".pdf");
            }
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
    Check if a row is blank (all cells are empty or null)
    If the row is blank, skip it but continue processing other rows
     */
    private boolean isRowBlank(Row row) {

        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    /*
    Download a PDF from the given URL
    If the download from the first URL fails and a second URL is provided, attempt to download from there
    Save the downloaded PDF to the specified directory with the given file name
     */
    public void downloadPDF(String BRnum, URL urlStr1, URL urlStr2, String fileName) {

        System.out.println("=========================================");
        System.out.println("Opening connection to download the PDF...");

        // create a ReportEntity builder
        ReportEntity.ReportEntityBuilder b = ReportEntity.builder().url(urlStr1).urlUsed("First URL");

        // define download path
        String downloadsPath = reportSaveDir + File.separator + fileName;

        // try to download the PDF
        try (InputStream in = tryOpen(BRnum, urlStr1, urlStr2, b);
             // create output stream to save the file
             FileOutputStream fos = new FileOutputStream(downloadsPath)) {

            // read from input stream and write to output stream
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) != -1; ) fos.write(buf, 0, n);
            b.BRnum(BRnum).status("success");
            System.out.println("PDF downloaded successfully!");

        } catch (Exception e) {
            b.BRnum(BRnum).status("error").errorMessage("Failed to download PDF: " + e.getMessage());
            System.out.println("Failed to download PDF: " + e.getMessage());
        }

        // add entry to report list
        reportEntries.add(b.build());
    }

    /*
    Try to open a connection to the given URL
    If the first URL fails and a second URL is provided, attempt to open that one
    Return the InputStream of the successfully opened URL
    If both URLs fail, throw an IOException
     */
    private InputStream tryOpen(String BRnum, URL url1, URL url2, ReportEntity.ReportEntityBuilder b) throws IOException {

        try {
            return url1.openStream();
        } catch (IOException e) {
            if (url2 == null) throw new IOException("both URLs failed");
            b.BRnum(BRnum).url(url2).urlUsed("Second URL").status("error").reason("download failed - both URLs tried");
            return url2.openStream();
        }
    }

    /*
    Create a new Excel report file to log the results of the PDF downloads
    The report file will have columns: URL, URL used, status, reason for failure, error message
    If the report file already exists, do not overwrite it append new entries instead
     */
    public void createReport() {

        System.out.println("=========================================");
        System.out.println("Creating report file...");

        // check if directory exists, if not create it
        try {
            File dir = new File(reportSaveDir);
            System.out.println("Path: " + dir.getAbsolutePath());
            System.out.println("Exists: " + dir.exists());
            System.out.println("IsDirectory: " + dir.isDirectory());

            // create directory if it does not exist
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                System.out.println("Created? " + created);
            }

            // check if directory is writable
            File reportFile = new File(reportFilePath);
            if (reportFile.exists()) {
                System.out.println("Report file already exists at: " + reportFilePath);
                return;
            }

            // create new report file
            try (Workbook wb = new XSSFWorkbook();

                 // create output stream to save the file
                 FileOutputStream fos = new FileOutputStream(reportFile)) {

                // create sheet and header row
                Sheet sheet = wb.createSheet("Report");
                Row headerRow = sheet.createRow(0);

                // create font for header
                Font headerFont = wb.createFont();
                headerFont.setFontName("Arial");
                headerFont.setFontHeightInPoints((short) 12);
                headerFont.setBold(true);
                headerFont.setItalic(true);

                // create cell style for header
                CellStyle headerCellStyle = wb.createCellStyle();
                headerCellStyle.setFont(headerFont);
                headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                // create header cells
                String[] headers = {"BRnum", "URL", "URL Used", "Status", "Reason", "Error Message"};
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(headers[i]);
                    cell.setCellStyle(headerCellStyle);
                }

                // write to file meaning the report file is created
                wb.write(fos);
                System.out.println("Report file created: " + reportFilePath);
            }
        } catch (IOException e) {
            System.out.println("Failed to create report file. " + e.getMessage());
        }
    }

    /*
    Update the report file with new entries from the reportEntries list
    Append new entries to the existing report file
    Auto-size columns for better readability
    Clear the reportEntries list after updating the file
     */
    public void updateReport() {

        System.out.println("=========================================");
        System.out.println("Updating report file...");

        // check if report file exists
        File reportFile = new File(reportFilePath);
        if (!reportFile.exists()) {
            System.out.println("Report file does not exist at: " + reportFilePath);
            return;
        }

        // open existing report file
        try (FileInputStream fis = new FileInputStream(reportFile);

             // create Workbook instance for XLSX file
             Workbook wb = new XSSFWorkbook(fis)) {

            // get the report sheet
            Sheet sheet = wb.getSheet("Report");
            if (sheet == null) sheet = wb.getSheetAt(0);

            // append new entries
            int lastRowNum = sheet.getLastRowNum();
            for (ReportEntity entry : reportEntries) {
                Row row = sheet.createRow(++lastRowNum);
                row.createCell(0).setCellValue(entry.getBRnum() != null ? entry.getBRnum() : "");
                row.createCell(1).setCellValue(entry.getUrl() != null ? entry.getUrl().toString() : "");
                row.createCell(2).setCellValue(entry.getUrlUsed() != null ? entry.getUrlUsed() : "");
                row.createCell(3).setCellValue(entry.getStatus() != null ? entry.getStatus() : "");
                row.createCell(4).setCellValue(entry.getReason() != null ? entry.getReason() : "");
                row.createCell(5).setCellValue(entry.getErrorMessage() != null ? entry.getErrorMessage() : "");
            }

            // write changes back to the file
            try (FileOutputStream fos = new FileOutputStream(reportFile)) {

                // auto-size columns
                for (int c = 0; c < 6; c++) {
                    sheet.autoSizeColumn(c);
                    sheet.setColumnWidth(c, sheet.getColumnWidth(c) + 1250);
                }

                // write to file and flush (save changes)
                wb.write(fos);
                fos.flush();
            }

            // clear report entries after updating
            reportEntries.clear();
            System.out.println("=========================================");
            System.out.println("Report updated successfully!");
            System.out.println("Report path: " + reportFilePath);
            System.out.println("Amount of entries: " + lastRowNum);
            System.out.println("=========================================");

        } catch (IOException e) {
            System.out.println("Failed to update report file. " + e.getMessage());
        }
    }
}
