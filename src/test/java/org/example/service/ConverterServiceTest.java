package org.example.service;

import junit.framework.TestCase;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.net.URL;
import java.util.*;

public class ConverterServiceTest extends TestCase {

    private final String dataPath = "C:\\Specialisterne - Opgaver\\Uge 4\\data\\GRI_2017_2020_TEST.xlsx";

    public void setUp() throws Exception {
        System.out.println("Global setup");
        super.setUp();
    }

    public void tearDown() throws Exception {
        System.out.println("Global teardown");
        super.tearDown();
    }

    // Test to read the Excel file and print the URLs in the "Pdf_URL" column
    public void testGetURLFromExcel() throws IOException {

        // Open the Excel file
        FileInputStream file = new FileInputStream(dataPath);
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
                    System.out.println("PDF Link for row " + i + ": " + pdfLink.getStringCellValue());
                }
            }
        }
    }

    public void testDownloadPDF() {
        System.out.println("Opening connection to download PDF...");
        try {
            // the url of the pdf to be downloaded
            URL url = new URL("http://cdn12.a1.net/m/resources/media/pdf/A1-Umwelterkl-rung-2016-2017.pdf");

            // name of the downloaded pdf
            String fileName = "MyDownloadedFile.pdf";

            // download path
            String downloadsPath = System.getProperty("user.home") + "\\Downloads\\" + fileName;

            // open the connection to the url
            InputStream in = url.openStream();
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
            System.out.println("File path: " + downloadsPath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}




