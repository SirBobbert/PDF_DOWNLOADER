package org.example.app;

import org.example.service.core.ConverterService;
import org.example.service.reader.PoiExcelReader;
import org.example.service.report.PoiReportRepository;
import org.example.service.downloader.SimplePdfDownloader;

import java.nio.file.Path;

/**
 * Entry point for the application.
 * Initializes the dependencies and executes the program workflow.
 */

public class Main {
    public static void main(String[] args) {

        // Path to the input Excel file containing BRnum, Pdf_URL, and Html_URL columns.
        Path excelPath = Path.of("src/main/java/org/example/util/data/GRI_2017_2020_TEST.xlsx");

        // Path to the directory where downloaded PDFs will be stored.
        Path downloadDir = Path.of("src/main/java/org/example/util/download");

        //Path to the output report Excel file that will be created/updated.
        Path reportFile = downloadDir.resolve("Report.xlsx");

        ConverterService service = new ConverterService(
                excelPath,
                reportFile,
                downloadDir,
                new PoiExcelReader(),
                new SimplePdfDownloader(),
                new PoiReportRepository()
        );

        service.execute();
    }
}

// TODO: Check if URL is not too large
// TODO: Check if URL is corrupted
// TODO: Check if PDF is not encrypted
// TODO: Krav spec
// TODO: Readme.md
// TODO: Downlaod status to report (look in metadata dataset)
