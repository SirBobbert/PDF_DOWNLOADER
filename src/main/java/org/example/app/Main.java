package org.example.app;

import org.example.service.ConverterService;

import java.net.URL;

public class Main {
    public static void main(String[] args) {

        ConverterService converter = new ConverterService();

        try {
            URL url = new URL("http://cdn12.a1.net/m/resources/media/pdf/A1-Umwelterkl-rung-2016-2017.pdf");
            String excelFilePath = "C:\\Specialisterne - Opgaver\\Uge 4\\data\\GRI_2017_2020_TEST.xlsx";
            String fileName = "testFile";
            String pathToSave = "Downloads";

            converter.getURLFromExcel(excelFilePath);
            converter.downloadPDF(url, fileName + ".pdf", pathToSave);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
