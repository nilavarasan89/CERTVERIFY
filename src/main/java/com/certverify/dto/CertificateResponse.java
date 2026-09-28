package com.certverify.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateResponse {

    private Long id;
    private String certificateId;
    private String verificationCode;
    private Long participantId;
    private String participantName;
    private String participantEmail;
    private Long courseId;
    private String courseName;
    private LocalDate issuedDate;
    private boolean revoked;
    private String status;
}
