package org.example.app;

import org.example.service.ConverterService;

public class Main {
    public static void main(String[] args) {

        String excelFilePath = "C:\\Specialisterne - Opgaver\\Uge 4\\data\\GRI_2017_2020_TEST.xlsx";
        String reportSaveDir = System.getProperty("user.home") + "\\Downloads\\test";

        ConverterService converter = new ConverterService(excelFilePath, reportSaveDir);
        converter.createReport();
        converter.getURLFromExcel();
        converter.updateReport();
    }
}
