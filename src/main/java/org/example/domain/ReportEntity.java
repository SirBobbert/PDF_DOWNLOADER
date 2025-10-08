package org.example.domain;

import lombok.*;

import java.net.URL;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReportEntity {

    private URL url;
    private String urlUsed;
    private String status;
    private String reason;
    private String errorMessage;

    @Override
    public String toString() {
        return "ReportEntity{" +
                "errorMessage='" + errorMessage + '\'' +
                ", url=" + url +
                ", urlUsed='" + urlUsed + '\'' +
                ", status='" + status + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
