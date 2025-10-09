package org.example.service.report;

import org.example.domain.ReportEntity;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * ReportRepository defines the responsiblity of managing the Excel report file.
 * It can ensure the report exists, load existing BRnums and append new entries.
 */

public interface ReportRepository {

    void ensureReport(Path reportFile);

    Set<String> loadExistingBRnums(Path reportFile);

    void append(Path reportFile, List<ReportEntity> entries);
}
