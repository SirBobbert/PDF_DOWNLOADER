package org.example.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.domain.ReportEntity;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // TODO: Check for URL status code/is accessible
    // TODO: Check if URL is a valid PDF - if not, use alternative URL (Report Html Address)
    // TODO: Check if URL is not too large
    // TODO: Check if URL is corrupted
    // TODO: Check if URL is not empty
    // TODO: Check if PDF is a duplicate
    // TODO: Check if PDF is not encrypted
    // TODO: Parser to get URL from excel file
    // TODO: Limit threads
    // TODO: Creation of new excel file with status of each URL (downloaded, failed, reason for failure)

    public void getURLFromExcel() {
        // Read the Excel file and extract URLs from the "Pdf_URL" column
        // Return the URLs as a list of strings
        // TODO: For each URL, call the downloadPDF method to download the PDF (threading)
        // TODO: Add option for using the alternative URL (Report Html Address)

        Map<URL, URL> pdfUrls = new LinkedHashMap<>();

        try (FileInputStream fis = new FileInputStream(excelFilePath)) {


            // Open the Excel file
            Workbook workbook = new XSSFWorkbook(fis);

            // Get the first sheet
            Sheet sheet = workbook.getSheetAt(0);

            if (sheet == null || sheet.getPhysicalNumberOfRows() < 0) return;

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return;

            // get col indexes of "Pdf_URL" and "Report Html Address"
            Integer pdfCol = null, htmlCol = null;
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {

                Cell h = headerRow.getCell(c);
                String name = h != null ? h.getStringCellValue() : null;

                if ("Pdf_URL".equalsIgnoreCase(name)) pdfCol = c;
                if ("Report Html Address".equalsIgnoreCase(name)) htmlCol = c;
            }

            if (pdfCol == null) return;

            DataFormatter formatter = new DataFormatter();

            for (int r = 1; r < sheet.getPhysicalNumberOfRows(); r++) {

                Row row = sheet.getRow(r);

                if (row == null) continue;

                URL pdfUrl, htmlUrl = null;

                // read pdf
                Cell pdfCell = row.getCell(pdfCol);
                String pdfStr = pdfCell != null ? formatter.formatCellValue(pdfCell).trim() : "";

                if (!pdfStr.isEmpty()) {
                    try {
                        pdfUrl = new URL(pdfStr);
                    } catch (Exception e) {
                        System.out.println("Invalid PDF URL at row " + (r + 1) + ": " + pdfStr);
                        continue;
                    }
                } else {
                    continue;
                }

                // read html
                if (htmlCol != null) {
                    Cell htmlCell = row.getCell(htmlCol);
                    String htmlStr = htmlCell != null ? formatter.formatCellValue(htmlCell).trim() : "";

                    if (!htmlStr.isEmpty()) {
                        try {
                            htmlUrl = new URL(htmlStr);
                        } catch (Exception e) {
                            System.out.println("Invalid HTML URL at row " + (r + 1) + ": " + htmlStr);
                        }
                    }
                }
                downloadPDF(pdfUrl, htmlUrl, "file" + "_" + (r + 1) + ".pdf");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Download PDF from URL and save it to specified path with the specified file name
    public void downloadPDF(URL urlStr1, URL urlStr2, String fileName) {

//        String fileName = "report.pdf";
        String targetPath = "Downloads";


        ReportEntity.ReportEntityBuilder builder = ReportEntity.builder();

        // TODO: Threading: Make this method take an array of URLs and download them in parallel using threads
        // TODO: If the targetPath does not exist, create it
        // TODO: If the file exists, ask the user if they want to overwrite it
        // TODO: If the file already exists, append a number to the file name
        System.out.println("==========================================");
        System.out.println("Opening connection to download the PDF...");
        try {

//            String downloadsPath = System.getProperty("user.home") + "\\" + targetPath + "\\" + fileName;
            String downloadsPath = reportSaveDir + File.separator + fileName;
            InputStream in;

            // TODO: If URL is not a valid PDF, read the secondary URL (Report Html Address) and download that instead
            // open the connection to the url
            try {
                System.out.println("Trying primary URL: " + urlStr1);
                in = urlStr1.openStream();
                builder.url(urlStr1).urlUsed("First URL").status("success");
            } catch (IOException e) {
                System.out.println("Primary URL failed, trying secondary URL: " + urlStr2);
                if (urlStr2 != null) {
                    in = urlStr2.openStream();
                    builder.url(urlStr2).urlUsed("Second URL").status("success");
                } else {
                    builder.status("error").reason("Both URLs failed");
                    return;
                }
            }

            FileOutputStream fos = new FileOutputStream(downloadsPath);

            // read the data from the url and write it to the file
            System.out.println("Reading data from URL...");
            int length;
            byte[] buffer = new byte[1024];
            while ((length = in.read(buffer)) != -1) {
                fos.write(buffer, 0, length);
            }

            // close the streams
            fos.close();
            in.close();

            System.out.println("PDF downloaded successfully!");
            System.out.println("Downloaded at: " + downloadsPath);
        } catch (Exception e) {
            System.out.println("Failed to download PDF. " + e.getMessage());
            System.out.println("Skipping to next URL...");
        }

        ReportEntity reportEntity = builder.build();
        // add to new Excel file with status of each URL (downloaded, what url worked, failed, reason for failure)

        // add to report entries
        reportEntries.add(reportEntity);
    }


    public void createReport() {

        /*
        Create a new Excel file to store the report
        with columns: URL, URL used, status, reason for failure
        if the file already exists, do not overwrite it
         */

        try {
            File dir = new File(reportSaveDir);
            if (!dir.exists()) dir.mkdirs();

            File reportFile = new File(reportFilePath);
            if (reportFile.exists()) {
                System.out.println("Report file already exists at: " + reportFilePath);
                return;
            }

            try (Workbook wb = new XSSFWorkbook();
                 FileOutputStream fos = new FileOutputStream(reportFile)) {
                Sheet sheet = wb.createSheet("Report");
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("URL");
                headerRow.createCell(1).setCellValue("URL Used");
                headerRow.createCell(2).setCellValue("Status");
                headerRow.createCell(3).setCellValue("Reason");
                wb.write(fos);
            }


            System.out.println("Report file created: " + reportFilePath);
        } catch (IOException e) {
            System.out.println("Failed to create report file. " + e.getMessage());
        }
    }

    public void updateReport() {
        File reportFile = new File(reportFilePath);
        if (!reportFile.exists()) {
            System.out.println("Report file does not exist at: " + reportFilePath);
            return;
        }

        try (FileInputStream fis = new FileInputStream(reportFile);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheet("Report");
            if (sheet == null) sheet = wb.getSheetAt(0);

            int lastRowNum = sheet.getLastRowNum();
            for (ReportEntity entry : reportEntries) {
                Row row = sheet.createRow(++lastRowNum);
                row.createCell(0).setCellValue(entry.getUrl() != null ? entry.getUrl().toString() : "");
                row.createCell(1).setCellValue(entry.getUrlUsed() != null ? entry.getUrlUsed() : "");
                row.createCell(2).setCellValue(entry.getStatus() != null ? entry.getStatus() : "");
            }

            // Write back to the same file
            try (FileOutputStream fos = new FileOutputStream(reportFile)) {
                wb.write(fos);
                fos.flush();
            }

            reportEntries.clear();
            System.out.println("Report file updated: " + reportFilePath);

        } catch (IOException e) {
            System.out.println("Failed to update report file. " + e.getMessage());
        }
    }
}
