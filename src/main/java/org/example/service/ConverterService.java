package org.example.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ConverterService {

    // TODO: Check for URL status code/is accessible
    // TODO: Check if URL is a valid PDF
    // TODO: Check if URL is not too large
    // TODO: Check if URL is corrupted
    // TODO: Check if URL is not empty
    // TODO: Check if PDF is a duplicate
    // TODO: Check if PDF is not encrypted
    // TODO: Parser to get URL from excel file
    // TODO: Limit threads

    public List<String> getURLFromExcel(String excelFilePath) throws IOException {
        // Read the Excel file and extract URLs from the "Pdf_URL" column
        // Return the URLs as a list of strings
        // TODO: For each URL, call the downloadPDF method to download the PDF (threading)
        // TODO: Add option for using the alternative URL (Report Html Address)

        List<String> pdfUrls = new java.util.ArrayList<>();

        // Open the Excel file
        FileInputStream file = new FileInputStream(excelFilePath);
        Workbook workbook = new XSSFWorkbook(file);

        // Get the first sheet
        Sheet sheet = workbook.getSheetAt(0);

        // iterate through the rows in the sheet, starting from row 1 (row 0 is the header)
        for (int i = 1; i < sheet.getPhysicalNumberOfRows(); i++) {

            // get the current row and the header row
            Row currentRow = sheet.getRow(i);
            Row headerRow = sheet.getRow(0);

            // iterate through the cells in the header row
            for (int col = 0; col < headerRow.getLastCellNum(); col++) {

                // get the cell in the header row
                Cell cell = headerRow.getCell(col);

                if (cell != null && "Pdf_URL".equals(cell.getStringCellValue())) {
                    // get the index of the column where the header is "Pdf_URL"
                    int pdf_index = cell.getColumnIndex();

                    // get the cell in the current row at the index of the "Pdf_URL" column
                    Cell pdfLink = currentRow.getCell(pdf_index);

                    // print the value of the cell
                    pdfUrls.add(pdfLink.getStringCellValue());
                }
            }
        }

        return pdfUrls;
    }


    // Download PDF from URL and save it to specified path with the specified file name
    public boolean downloadPDF(URL urlStr, String fileName, String targetPath) {

        // TODO: Threading: Make this method take an array of URLs and download them in parallel using threads
        // TODO: If the targetPath does not exist, create it
        // TODO: If the file exists, ask the user if they want to overwrite it
        // TODO: If the file already exists, append a number to the file name
        System.out.println("Opening connection to download PDF...");
        try {
            String downloadsPath = System.getProperty("user.home") + "\\" + targetPath + "\\" + fileName;

            // open the connection to the url
            InputStream in = urlStr.openStream();
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
            System.out.println("Downloaded at: ");
            System.out.println(downloadsPath);
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Path downloadPDF2(URL url, String fileName, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        Path out = targetDir.resolve(fileName);

        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(10_000);
        con.setReadTimeout(20_000);
        con.setInstanceFollowRedirects(true);
        con.setRequestProperty("Accept", "application/pdf,*/*;q=0.9");
        con.connect();

        int code = con.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP " + code + " for " + url);
        }

        try (InputStream in = con.getInputStream();
             OutputStream fos = Files.newOutputStream(out)) {
            in.transferTo(fos);
        }
        return out;
    }

}