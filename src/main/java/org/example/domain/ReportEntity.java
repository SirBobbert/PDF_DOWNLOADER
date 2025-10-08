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

    @Override
    public String toString() {
        return "ReportEntity{" +
                "reason='" + reason + '\'' +
                ", url=" + url +
                ", status='" + status + '\'' +
                '}';
    }
}
