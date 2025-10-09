package org.example.service.core;

import org.slf4j.MDC;
import org.example.domain.ReportEntity;
import org.example.service.downloader.PdfDownloader;
import org.example.service.reader.ExcelReader;

import java.net.URL;
import java.nio.file.Path;

/**
 * DownloadTask wraps a single row downloaded so it can run in a thread.
 * Inputs:
 * - Row meta (index, BRnum, primary/fallback URLs)
 * - Target file path for the PDF
 * - PdfDownloader strategy
 * Output:
 * - A fully built ReportEntity ready to be appended to the report
 */
public class DownloadTask implements java.util.concurrent.Callable<ReportEntity> {

    private final int sequence;
    private final ExcelReader.InputRow row;
    private final Path targetFile;
    private final PdfDownloader downloader;

    /**
     * Constructs a task with immutable data used by the thread.
     */
    public DownloadTask(int sequence,
                        ExcelReader.InputRow row,
                        Path targetFile,
                        PdfDownloader downloader) {
        this.sequence = sequence;
        this.row = row;
        this.targetFile = targetFile;
        this.downloader = downloader;
    }

    /**
     * Performs the download and returns the report row descriping the result
     */
    @Override
    public ReportEntity call() {

        MDC.put("tid", String.valueOf(Thread.currentThread().threadId()));
        MDC.put("seq", String.valueOf(sequence));
        MDC.put("br", row.BRnum());

        try {

            // prepare inputs
            String br = row.BRnum();
            URL primary = row.pdfUrl();
            URL fallback = row.htmlUrl();

            // execute download (thread-safe in current downloader)
            var res = downloader.download(br, primary, fallback, targetFile);

            // compute lable for which url was used
            String label = "";
            if (res.urlUsed() != null) {
                label = res.urlUsed().equals(primary) ? "Primary URL"
                        : "Backup URL";
            }

            // build the report entry
            return ReportEntity.builder()
                    .BRnum(br)
                    .url(res.urlUsed())
                    .urlUsed(label)
                    .status(res.success() ? "success" : "error")
                    .reason(res.reason())
                    .errorMessage(res.errorMessage())
                    .build();
        } finally {
            MDC.clear();
        }
    }
}
