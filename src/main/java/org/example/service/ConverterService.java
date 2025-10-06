package org.example.service;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ConverterService {


    public void convertExcelToCSV() {

        String csvPath = "C:\\Specialisterne - Opgaver\\Uge 4\\data\\GRI_2017_2020_TEST.xlsx";


        try (BufferedReader br = new BufferedReader(new FileReader(csvPath))) {

            System.out.println(br);
            System.out.println(br.read());
            System.out.println(br.readLine().getClass().getName());



        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

