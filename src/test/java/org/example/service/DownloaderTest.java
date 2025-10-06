package org.example.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/*
 * Tests the ConverterService.downloadPDF method using a local HTTP server
 * Verifies that the PDF is downloaded correctly to a temporary directory
 * Checks file existence, name, size, and content as it matches expected bytes
 */

public class DownloaderTest {

    private static HttpServer server;
    private static int port;

    // Sample PDF content
    private static final byte[] PDF_BYTES = "%PDF-1.4\n%%EOF".getBytes(StandardCharsets.UTF_8);

    // Set up a simple HTTP server that serves a PDF file
    @BeforeAll
    static void startServer() throws IOException {

        // Create an HTTP server on a random available port
        server = HttpServer.create(new InetSocketAddress(0), 0);

        // Get the assigned port
        port = server.getAddress().getPort();

        // Define a context that serves a PDF file
        server.createContext("/test.pdf", exchange -> {

            // Serve the PDF bytes
            byte[] body = PDF_BYTES;
            exchange.getResponseHeaders().add("Content-Type", "application/pdf");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            // Close the exchange
            exchange.close();
        });
        // Start the server
        server.start();
    }

    // Stop the HTTP server after all tests
    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    // Test downloading a PDF to a temporary directory
    @Test
    void downloadsPdfToTempDir(@TempDir Path temp) throws Exception {

        // URL of the local test PDF
        URL url = new URL("http://localhost:" + port + "/test.pdf");

        // Desired file name for the downloaded PDF
        String fileName = "MyDownloadedFile.pdf";

        // Use the ConverterService to download the PDF
        Path saved = new ConverterService().downloadPDF2(url, fileName, temp);

        // Verify the file was saved correctly
        assertTrue(Files.exists(saved));
        assertEquals(fileName, saved.getFileName().toString());
        assertArrayEquals(PDF_BYTES, Files.readAllBytes(saved));
        assertTrue(Files.size(saved) > 0);
    }
}
