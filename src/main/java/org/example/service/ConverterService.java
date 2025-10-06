package org.example.service;

import java.io.*;
import java.net.URL;

public class ConverterService {

    // TODO: Check for URL status code/is accessible
    // TODO: Check if URL is a valid PDF
    // TODO: Check if URL is not too large
    // TODO: Check if URL is corrupted
    // TODO: Check if URL is not empty
    // TODO: Check if PDF is a duplicate
    // TODO: Check if PDF is not encrypted
    // TODO: Parser to get URL from excel file

    public void getURLFromExcel(String filePath) {
        // Use Apache POI to read the Excel file and extract URLs from the "Pdf_URL" column
        // Return the URLs as a list of strings
        // For each URL, call the downloadPDF method to download the PDF
        System.out.println(filePath);
        System.out.println("WIP");
    }


    // Download PDF from URL and save it to specified path with the specified file name
    public void downloadPDF(URL urlStr, String fileName, String targetPath) {

        // TODO: Threading: Make this method take an array of URLs and download them in parallel using threads
        // TODO: If the targetPath does not exist, create it
        // TODO: If the file exists, ask the user if they want to overwrite it
        // TODO: If the file already exists, append a number to the file name

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

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}