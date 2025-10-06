package org.example.app;


import org.example.service.ConverterService;

public class Main {
    public static void main(String[] args) {


        ConverterService converter = new ConverterService();
        converter.convertExcelToCSV();

    }
}
