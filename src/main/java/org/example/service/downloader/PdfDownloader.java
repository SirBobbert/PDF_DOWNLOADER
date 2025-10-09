package org.example.service.downloader;


import java.net.URL;
import java.nio.file.Path;

/**
 * PdfDownloader defines the responsibility of downloading PDF files from given URLs.
 * Implementation try primary URL, then fallback url (if provided) on failure and return a result object.
 */

public interface PdfDownloader {


    /**
     * Attemps to download a PDF from the given URLs.
     *
     * @param brNum   - identifier for the row
     * @param primary - primary PDF URL
     * @return result object with download outcome
     * @fallback - optional fallback URL if primary fails
     * @target - path where the file should be saved
     */
    DownloadResult download(String brNum, URL primary, URL fallback, Path target);


    /**
     * Simple result object for downloading attempt.
     */
    record DownloadResult(String BRnum, URL urlUsed, boolean success, String reason, String errorMessage) {
    }
}
