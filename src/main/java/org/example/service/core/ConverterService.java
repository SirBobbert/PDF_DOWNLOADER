package org.example.service.core;

import org.slf4j.MDC;
import org.example.domain.ReportEntity;
import org.example.service.downloader.PdfDownloader;
import org.example.service.reader.ExcelReader;
import org.example.service.report.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

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

        MDC.put("tid", String.valueOf(Thread.currentThread().threadId()));
        MDC.put("seq", "-");
        MDC.put("br", "-");

        try {


            // timing + counters
            long startNs = System.nanoTime();
            int ok = 0, fail = 0;

            log.info("Checking if report exists at: {}", reportFile);
            reportRepository.ensureReport(reportFile);

            Set<String> existing = reportRepository.loadExistingBRnums(reportFile);
            log.info("Found {} existing BRnums in report", existing.size());

            List<ExcelReader.InputRow> all = excelReader.readRows(excelPath);
            log.info("Loaded {} rows from Excel file", all.size());

            // Filter to rows that have a URL and are not duplicates
            List<ExcelReader.InputRow> work = all.stream()
                    .filter(r -> (r.pdfUrl() != null || r.htmlUrl() != null)
                            && (r.BRnum() == null || !existing.contains(r.BRnum())))
                    .toList();
            int total = work.size();
            log.info("Prepared {} rows with at least one URL", total);

            // create a bounded thread pool (tweakable size)
            int poolSize = Math.max(4, Math.min(6, Runtime.getRuntime().availableProcessors()));
            log.info("Using download thread pool size: {}", poolSize);

            var pool = (java.util.concurrent.ThreadPoolExecutor)
                    java.util.concurrent.Executors.newFixedThreadPool(poolSize, r -> {
                        Thread t = new Thread(r);
                        t.setName("dl-" + t.threadId());

                        log.info("======================= Creating thread: {} =======================", t.getName());

                        t.setUncaughtExceptionHandler((th, ex) ->
                                log.error("Uncaught in {}: {}", th.getName(), ex.toString(), ex));
                        return t;
                    });

            try {
                // submit tasks
                List<java.util.concurrent.Future<ReportEntity>> futures = new java.util.ArrayList<>();
                for (int i = 0; i < work.size(); i++) {
                    var row = work.get(i);
                    var seq = i + 1;
                    log.info("({}/{}) BRnum={} | row={} | preparing download...", seq, total, row.BRnum(), row.rowIndex());
                    Path target = downloadDir.resolve("file_" + seq + ".pdf");
                    futures.add(pool.submit(new DownloadTask(seq, row, target, pdfDownloader)));
                }

                // wait for completion and collect results
                List<ReportEntity> toAppend = new java.util.ArrayList<>();
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        ReportEntity re = futures.get(i).get(); // waits for each task
                        toAppend.add(re);
                        if ("success".equalsIgnoreCase(re.getStatus())) {
                            ok++;
                            log.info("({}/{}) BRnum={} | SUCCESS -> {}", i + 1, total, re.getBRnum(),
                                    re.getUrl() != null ? re.getUrl() : "(no URL)");
                        } else {
                            fail++;
                            log.error("({}/{}) BRnum={} | FAILED -> {}", i + 1, total, re.getBRnum(),
                                    re.getErrorMessage() != null ? re.getErrorMessage() : re.getReason());
                        }
                    } catch (Exception e) {
                        fail++;
                        log.error("({}/{}) UNCAUGHT task failure -> {}", i + 1, total, e.getMessage());
                    }
                }

                // append results (if any)
                if (!toAppend.isEmpty()) {
                    log.info("[REPORT] Appending {} new entries...", toAppend.size());
                    reportRepository.append(reportFile, toAppend);
                    log.info("[REPORT] Done.");
                } else {
                    log.info("[REPORT] No new entries to update.");
                }

            } finally {
                // shutdown pool
                log.info("Shutting down thread pool...");
                pool.shutdown();
                try {
                    if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                        pool.shutdownNow();
                        if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                            log.warn("Pool did not terminate cleanly");
                        }
                    }
                } catch (InterruptedException ie) {
                    pool.shutdownNow();
                    Thread.currentThread().interrupt(); // her
                    log.warn("Interrupted while awaiting termination", ie);
                }
            }

            // execution start to finish in ms
            long durMs = (System.nanoTime() - startNs) / 1_000_000L;
            double durSec = durMs / 1000.0;

            log.info("""
                            \n
                            ===============================================================================
                            ✅ Finished PDF run
                            - Max threads allowed     : {}
                            - Threads actually spawned: {}
                            - Total rows considered   : {}
                            - Downloads succeeded     : {}
                            - Downloads failed        : {}
                            - Elapsed                 : {} ms
                            - Report path             : {}
                            ===============================================================================
                            """,
                    poolSize,
                    pool.getLargestPoolSize(),
                    ok + fail,
                    ok,
                    fail,
                    durSec,
                    reportFile
            );
        } finally {
            MDC.clear();
        }
    }
}
