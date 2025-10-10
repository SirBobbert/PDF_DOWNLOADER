package org.example.service.core;

import org.slf4j.MDC;
import org.example.domain.ReportEntity;
import org.example.service.downloader.PdfDownloader;
import org.example.service.reader.ExcelReader;
import org.example.service.report.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.nio.file.Path;
import java.util.concurrent.*;

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
     * Steps:
     * 1) Ensure report exists.
     * 2) Load existing BRnums.
     * 3) Read input rows from Excel.
     * 4) Filter rows to process.
     * 5) Create thread pool.
     * 6) Submit download tasks.
     * 7) Collect results.
     * 8) Append new entries to report.
     * 9) Log summary.
     * 10) Shutdown thread pool.
     */
    public void execute() {
        MDC.put("tid", String.valueOf(Thread.currentThread().threadId()));
        MDC.put("seq", "-");
        MDC.put("br", "-");

        long startNs = System.nanoTime();

        try {
            ensureReport();
            Set<String> existing = loadExistingBRnums();
            List<ExcelReader.InputRow> allRows = readInputRows();
            List<ExcelReader.InputRow> workRows = prepareWork(allRows, existing);

            if (workRows.isEmpty()) {
                log.info("Nothing to do. Exiting.");
                return;
            }

            int poolSize = pickPoolSize();
            ThreadPoolExecutor pool = buildPool(poolSize);

            try {
                RunResults results = runDownloads(workRows, pool);
                appendIfAny(results.collected());
                logSummary(results, pool, poolSize, Duration.ofNanos(System.nanoTime() - startNs));
            } finally {
                shutdownPool(pool);
            }
        } finally {
            MDC.clear();
        }
    }


    /**
     * Ensures the report file exists, creating it if it doesn't exist.
     */
    private void ensureReport() {
        log.info("Ensuring report exists at: {}", reportFile);
        reportRepository.ensureReport(reportFile);
    }

    /**
     * Load existing BRnums from the report file.
     * Skips already processed entries.
     *
     * @return set of existing BRnums
     */
    private Set<String> loadExistingBRnums() {
        Set<String> existing = reportRepository.loadExistingBRnums(reportFile);
        log.info("Existing BRnums in report: {}", existing.size());
        return existing;
    }

    /**
     * Read all rows from the Excel input file.
     *
     * @return list of input rows
     */
    private List<ExcelReader.InputRow> readInputRows() {
        List<ExcelReader.InputRow> rows = excelReader.readRows(excelPath);
        log.info("Loaded rows from Excel: {}", rows.size());
        return rows;
    }

    /**
     * Filter out rows that:
     * - have no URL
     * - or their BRnum already exists in the report
     *
     * @param rows     all rows read from Excel
     * @param existing BRnums already processed
     * @return rows that should be processed
     */
    private List<ExcelReader.InputRow> prepareWork(List<ExcelReader.InputRow> rows, Set<String> existing) {
        List<ExcelReader.InputRow> work = rows.stream()
                .filter(r -> (r.pdfUrl() != null || r.htmlUrl() != null) && (r.BRnum() == null || !existing.contains(r.BRnum())))
                .toList();
        log.info("Prepared {} rows with at least one URL", work.size());
        return work;
    }

    /**
     * Pick pool size based on CPU cores.
     * Ensures at least 4 and at most 6 threads.
     *
     * @return number of threads
     */
    private int pickPoolSize() {
        int cores = Runtime.getRuntime().availableProcessors();
        int size = Math.max(4, Math.min(6, cores));
        log.info("Download thread pool size: {}", size);
        return size;
    }

    /**
     * Build a fixed-size thread pool with custom thread naming and uncaught-exception logging.
     * Uses CallerRunsPolicy to apply back-pressure on the caller when the queue is full.
     *
     * @param size size number of threads
     * @return configured ThreadPoolExecutor
     */
    private ThreadPoolExecutor buildPool(int size) {
        BlockingQueue<Runnable> q = new ArrayBlockingQueue<>(1024);
        ThreadFactory tf = r -> {
            Thread t = new Thread(r);
            t.setName("dl-" + t.threadId());
            t.setUncaughtExceptionHandler((th, ex) ->
                    log.error("Uncaught in {}: {}", th.getName(), ex.toString(), ex));
            return t;
        };
        return new ThreadPoolExecutor(size, size, 30, TimeUnit.SECONDS, q, tf, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Submit download tasks to the pool and collect results.
     * Logs success/failure of each task.
     *
     * @param work rows to process
     * @param pool thread pool to use
     * @return RunResults with counts and collected report entries
     */
    private RunResults runDownloads(List<ExcelReader.InputRow> work, ThreadPoolExecutor pool) {
        var cs = new ExecutorCompletionService<ReportEntity>(pool);

        for (int i = 0; i < work.size(); i++) {
            var row = work.get(i);
            int seq = i + 1;
            Path target = downloadDir.resolve("file_" + seq + ".pdf");
            log.info("Prepared task {}/{} for BRnum={} (row={})",
                    seq, work.size(), row.BRnum(), row.rowIndex());
            cs.submit(new DownloadTask(seq, row, target, pdfDownloader));
        }

        int ok = 0, fail = 0;
        List<ReportEntity> collected = new ArrayList<>();

        for (int i = 0; i < work.size(); i++) {
            try {
                ReportEntity re = cs.take().get();
                collected.add(re);
                if ("success".equalsIgnoreCase(re.getStatus())) {
                    ok++;
                    log.info("({}/{}) BRnum={} | SUCCESS -> {}", i + 1, work.size(), re.getBRnum(),
                            re.getUrl() != null ? re.getUrl() : "(no URL)");
                } else {
                    fail++;
                    log.error("({}/{}) BRnum={} | FAILED -> {}", i + 1, work.size(), re.getBRnum(),
                            re.getErrorMessage() != null ? re.getErrorMessage() : re.getReason());
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                fail++;
                log.warn("Interrupted while waiting for task", ie);
            } catch (ExecutionException ee) {
                fail++;
                log.error("Task failed: {}", ee.getCause() != null ? ee.getCause().toString() : ee.toString(), ee);
            }
        }

        return new RunResults(ok, fail, collected);
    }

    /**
     * Append new report entries if there are any.
     *
     * @param list entries to append
     */
    private void appendIfAny(List<ReportEntity> list) {
        if (list.isEmpty()) {
            log.info("[REPORT] No new entries.");
            return;
        }
        log.info("[REPORT] Appending {} entries…", list.size());
        reportRepository.append(reportFile, list);
        log.info("[REPORT] Done.");
    }

    /**
     * Print summary of run: thread stats, counts, timing, report path.
     *
     * @param rr       run results
     * @param pool     thread pool
     * @param poolSize configured thread count
     * @param elapsed  total elapsed time
     */
    private void logSummary(RunResults rr, ThreadPoolExecutor pool, int poolSize, Duration elapsed) {
        log.info("""
                        
                        ===============================================================================
                        ✅ Finished PDF run
                        - Max threads allowed     : {}
                        - Threads actually spawned: {}
                        - Total rows considered   : {}
                        - Downloads succeeded     : {}
                        - Downloads failed        : {}
                        - Elapsed                 : {} seconds
                        - Report path             : {}
                        ===============================================================================
                        """,
                poolSize,
                pool.getLargestPoolSize(),
                rr.ok + rr.fail,
                rr.ok,
                rr.fail,
                elapsed.toSeconds(),
                reportFile
        );
    }

    /**
     * Gracefully shutdown the thread pool.
     * Fallback to force shutdown if termination times out.
     *
     * @param pool thread pool
     */
    private void shutdownPool(ThreadPoolExecutor pool) {
        log.info("Shutting down thread pool…");
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
            Thread.currentThread().interrupt();
            log.warn("Interrupted while awaiting termination", ie);
        }
    }

    private record RunResults(int ok, int fail, List<ReportEntity> collected) {
    }
}

