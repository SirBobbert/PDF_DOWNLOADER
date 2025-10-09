package org.example.domain;

import lombok.*;

import java.net.URL;

/**
 * ReportEntity represents a single log entry in the report.
 * It stores:
 * - BRnum: the identifier from the original Excel file
 * - url: the actual URL used
 * - urlUsed: label of which URL was attempted ("First URL" or "Second URL")
 * - status: success or error
 * - reason: optional description of why an error occurred
 * - errorMessage: technical error details (e.g., exception message)
 */

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReportEntity {

    private String BRnum;
    private URL url;
    private String urlUsed;
    private String status;
    private String reason;
    private String errorMessage;

    @Override
    public String toString() {
        return "ReportEntity{" +
                "BRnum='" + BRnum + '\'' +
                ", url=" + url +
                ", urlUsed='" + urlUsed + '\'' +
                ", status='" + status + '\'' +
                ", reason='" + reason + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
