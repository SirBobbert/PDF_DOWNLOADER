package org.example.app;

import org.example.service.ConverterService;

public class Main {
    public static void main(String[] args) {

        String excelDataPath = "C:\\Specialisterne - Opgaver\\Uge 4\\data\\GRI_2017_2020_TEST.xlsx";
        String reportSavePath = System.getProperty("user.home") + "\\Downloads\\test";

        ConverterService converter = new ConverterService(excelDataPath, reportSavePath);
        converter.executeProgram();

    }
}
