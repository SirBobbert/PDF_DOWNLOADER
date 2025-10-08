package org.example.app;

import org.example.service.ConverterService;

public class Main {
    public static void main(String[] args) {

        String excelDataPath = "src/main/java/org/example/util/data/GRI_2017_2020_TEST.xlsx";
        String reportSavePath = "src/main/java/org/example/util/download";

        ConverterService converter = new ConverterService(excelDataPath, reportSavePath);
        converter.executeProgram();
    }
}
