package org.example.service.core;

import org.example.domain.ReportEntity;
import org.example.service.downloader.PdfDownloader;
import org.example.service.reader.ExcelReader;
import org.example.service.report.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.nio.file.Path;

/**
 * ------------------------------------------------------------------------------------------------
 * ConverterService orchestrates the workflow.
 * Responsibilities:
 * ------------------------------------------------------------------------------------------------
 * - Ensure report exists via {@link ReportRepository}.
 * - Load existing BRnums via {@link ReportRepository}.
 * - Read input rows via {@link ExcelReader}.
 * - Download PDFs via {@link PdfDownloader}.
 * - Append results via {@link ReportRepository}.
 * ------------------------------------------------------------------------------------------------
 * Flow:
 * 1) {@link ReportRepository#ensureReport(java.nio.file.Path)}.
 * 2) {@link ReportRepository#loadExistingBRnums(java.nio.file.Path)}.
 * 3) {@link ExcelReader#readRows(java.nio.file.Path)}.
 * 4) Skip rows whose BRnum is already present.
 * 5) {@link PdfDownloader#download(String, java.net.URL, java.net.URL, java.nio.file.Path)}.
 * 6) Build {@link org.example.domain.ReportEntity}.
 * 7) {@link ReportRepository#append(java.nio.file.Path, java.util.List)}.
 * ------------------------------------------------------------------------------------------------
 * Collaborators:
 * - {@link ExcelReader} (input)
 * - {@link PdfDownloader} (I/O)
 * - {@link ReportRepository} (persistence)
 * ------------------------------------------------------------------------------------------------
 */

public class ConverterService {

    private final Path excelPath;
    private final Path reportFile;
    private final Path downloadDir;

    private final ExcelReader excelReader;
    private final PdfDownloader pdfDownloader;
    private final ReportRepository reportRepository;

    private static final Logger log = LoggerFactory.getLogger(ConverterService.class);

    /**
     * Initializes service with dependencies.
     *
     * @param excelPath        path to input Excel
     * @param reportFile       full path to report.xlsx
     * @param downloadDir      directory for PDF downloads
     * @param excelReader      component that reads Excel rows
     * @param pdfDownloader    component that downloads PDFs
     * @param reportRepository component that reads/writes the report
     */
    public ConverterService(
            Path excelPath,
            Path reportFile,
            Path downloadDir,
            ExcelReader excelReader,
            PdfDownloader pdfDownloader,
            ReportRepository reportRepository
    ) {
        this.excelPath = excelPath;
        this.reportFile = reportFile;
        this.downloadDir = downloadDir;
        this.excelReader = excelReader;
        this.pdfDownloader = pdfDownloader;
        this.reportRepository = reportRepository;
    }

    /**
     * Executes the main workflow of the application.
     */
    public void executeProgram() {

        long startNs = System.nanoTime();
        int ok = 0, fail = 0;

        log.info("Checking if report exists at: {}", reportFile);
        reportRepository.ensureReport(reportFile);

        Set<String> existing = reportRepository.loadExistingBRnums(reportFile);
        log.info("Found {} existing BRnums in report", existing.size());

        List<ExcelReader.InputRow> all = excelReader.readRows(excelPath);
        log.info("Loaded {} rows from Excel file", all.size());

        // worklist = rows that have at least one URL (primary or backup)
        List<ExcelReader.InputRow> work = all.stream()
                .filter(r -> r.pdfUrl() != null || r.htmlUrl() != null)
                .toList();
        int total = work.size();
        log.info("Prepared {} rows with at least one URL", total);

        List<ReportEntity> toAppend = new ArrayList<>();
        int i = 0;

        for (ExcelReader.InputRow row : work) {
            if (row.BRnum() != null && existing.contains(row.BRnum())) {
                log.warn("[DUPLICATE] Skipping BRnum={} (row {})", row.BRnum(), row.rowIndex());
                continue;
            }
            if (row.BRnum() == null || row.BRnum().isBlank()) {
                log.warn("[INPUT] Row {} has empty BRnum", row.rowIndex());
            }

            i++;
            log.info("({}/{}) BRnum={} | row={} | preparing download...", i, total, row.BRnum(), row.rowIndex());

            var target = downloadDir.resolve("file_" + i + ".pdf");
            var result = pdfDownloader.download(row.BRnum(), row.pdfUrl(), row.htmlUrl(), target);

            String label = "";
            if (result.urlUsed() != null) {
                if (row.pdfUrl() != null && result.urlUsed().equals(row.pdfUrl())) label = "Primary URL";
                else label = "Backup URL";
            }

            toAppend.add(ReportEntity.builder()
                    .BRnum(row.BRnum())
                    .url(result.urlUsed())
                    .urlUsed(label)
                    .status(result.success() ? "success" : "error")
                    .reason(result.reason())
                    .errorMessage(result.errorMessage())
                    .build());

            if (result.success()) ok++;
            else fail++;

            if (result.success()) {
                log.info("({}/{}) BRnum={} | SUCCESS -> {}", i, total, row.BRnum(),
                        result.urlUsed() != null ? result.urlUsed() : "(no URL)");
            } else {
                log.error("({}/{}) BRnum={} | FAILED -> {}", i, total, row.BRnum(),
                        result.errorMessage() != null ? result.errorMessage() : result.reason());
            }
        }

        if (!toAppend.isEmpty()) {
            log.info("[REPORT] Appending {} new entries...", toAppend.size());
            reportRepository.append(reportFile, toAppend);
            log.info("[REPORT] Done.");
        } else {
            log.info("[REPORT] No new entries to update.");
        }

        System.out.println("=============================================================================\n");
        long durMs = (System.nanoTime() - startNs) / 1_000_000L;
        log.info("""
                ✅ Finished PDF run
                - Total rows considered : {}
                - Downloads succeeded   : {}
                - Downloads failed      : {}
                - Elapsed               : {} ms
                Report path             : {}
                """, ok + fail, ok, fail, durMs, reportFile);
        System.out.println("=============================================================================");
    }

}
