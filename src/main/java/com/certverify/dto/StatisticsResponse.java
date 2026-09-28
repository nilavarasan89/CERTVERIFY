package com.certverify.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatisticsResponse {

    private Long courseId;
    private String courseName;
    private long totalCertificates;
    private long validCertificates;
    private long revokedCertificates;
    private Long totalVerificationAttempts;
}
