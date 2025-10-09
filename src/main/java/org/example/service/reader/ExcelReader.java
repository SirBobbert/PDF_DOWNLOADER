package org.example.service.reader;


import java.net.URL;
import java.nio.file.Path;
import java.util.List;

/**
 * ExcelReader defines the responsibility of reading input data (BRnum, URLs) from an Excel file.
 * Implementations return a list of InputRow DTOs with parsed values.
 */

public interface ExcelReader {

    /**
     * Reads rows from the given Excel file and extracts BRnum, Pdf_URL and Html_URL if available.
     *
     * @param excelPath to the Excel file.
     * @return list of InputRow objects with parsed data.
     */
    List<InputRow> readRows(Path excelPath);

    /**
     * Simple DTO representing one row of input data.
     */
    record InputRow(int rowIndex, String BRnum, URL pdfUrl, URL htmlUrl) {}
}
