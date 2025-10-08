//package org.example.service;
//
//import junit.framework.TestCase;
//
//import java.io.*;
//import java.net.URL;
//import java.util.*;
//
//public class ConverterServiceTest extends TestCase {
//
//    private final ConverterService service = new ConverterService();
//
//    public void setUp() throws Exception {
//        System.out.println("Global setup");
//        super.setUp();
//    }
//
//    public void tearDown() throws Exception {
//        System.out.println("Global teardown");
//        super.tearDown();
//    }
//
//    // Test to read the Excel file and print the URLs in the "Pdf_URL" column
//    public void testGetURLFromExcel() throws IOException {
//
//        // Open the Excel file
//        String dataPath = "C:\\Specialisterne - Opgaver\\Uge 4\\data\\GRI_2017_2020_TEST.xlsx";
////        Map<String, String> pdfUrls = service.getURLFromExcel(dataPath);
//
////        assertEquals(4, pdfUrls.size());
//    }
//
//    public void testDownloadPDF() throws Exception {
//
//        // the url of the pdf to be downloaded
//        URL url = new URL("http://cdn12.a1.net/m/resources/media/pdf/A1-Umwelterkl-rung-2016-2017.pdf");
//
//        // name of the downloaded pdf
//        String fileName = "MyDownloadedFile.pdf";
//
//        // target path to save the downloaded pdf
//        String targetPath = "Downloads";
//
////        boolean isOk = service.downloadPDF(url, fileName, targetPath);
////        assertTrue(isOk);
//    }
//}
//
//
//
//
