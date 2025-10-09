package org.example.service.downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * SimplePdfDownloader implements PdfDownloader using HttpURLConnection.
 * It enforces connection and read timeouts, and only reports error if both URLs fail.
 * Also logs time taken for each download.
 */

public class SimplePdfDownloader implements PdfDownloader {

    /**
     * Timeouts in milliseconds
     * - connectTimeout: 10 seconds
     * - readTimeout: 30 seconds
     */
    private final int connectTimeout = 10_000;
    private final int readTimeout = 30_000;

    private static final Logger log = LoggerFactory.getLogger(SimplePdfDownloader.class);

    @Override
    public DownloadResult download(String brNum, URL primary, URL fallback, Path target) {
        log.info("================= Starting download for BRnum={} ================", brNum);
        if (primary != null) log.debug("BRnum={} | Trying Primary URL: {}", brNum, primary);
        if (fallback != null) log.debug("BRnum={} | Backup URL available: {}", brNum, fallback);

        IOException lastError = null;

        for (URL url : new URL[]{primary, fallback}) {
            if (url == null) continue;

            long start = System.nanoTime();
            try (InputStream in = open(url);
                 OutputStream out = Files.newOutputStream(target)) {

                in.transferTo(out);
                long ms = (System.nanoTime() - start) / 1_000_000;
                log.info("BRnum={} | SUCCESS -> {} (took {} s)", brNum, target.getFileName(), ms / 1000.0);
                return new DownloadResult(brNum, url, true, null, null);

            } catch (SocketTimeoutException te) {
                long ms = (System.nanoTime() - start) / 1_000_000;
                String type = te.getMessage() != null && te.getMessage().toLowerCase().contains("connect")
                        ? "Connection timeout" : "Read timeout";
                log.error("BRnum={} | {} after {} s on {} ({})",
                        brNum, type, ms / 1000.0,
                        labelFor(url, primary) + "=" + shortUrl(url),
                        te.getMessage());
                lastError = te;

            } catch (IOException e) {
                long ms = (System.nanoTime() - start) / 1_000_000;
                log.warn("BRnum={} | FAILED on {}={} after {} s ({})",
                        brNum, labelFor(url, primary), shortUrl(url), ms / 1000.0, e.getMessage());
                lastError = e;
            }
        }

        String reason = (fallback == null) ? "Primary URL failed, no backup" : "Both Primary and Backup failed";
        log.error("BRnum={} | Download failed completely -> {}", brNum, reason);
        return new DownloadResult(brNum, null, false, reason,
                lastError != null ? lastError.getMessage() : reason);
    }

    private InputStream open(URL url) throws IOException {
        var conn = url.openConnection();

        if (conn instanceof HttpURLConnection http) {
            http.setConnectTimeout(connectTimeout);
            http.setReadTimeout(readTimeout);
            http.setInstanceFollowRedirects(true);

            int code = http.getResponseCode();
            if (code >= 400) throw new IOException("HTTP error " + code);
            return http.getInputStream();
        }
        return conn.getInputStream();
    }

    private static String labelFor(URL candidate, URL primary) {
        if (candidate == null) return "(null)";
        return candidate.equals(primary) ? "Pdf_URL" : "Html_URL";
    }

    private static String shortUrl(URL u) {
        if (u == null) return "(null)";
        String s = u.toString();
        return s.length() <= 120 ? s : s.substring(0, 117) + "...";
    }

}