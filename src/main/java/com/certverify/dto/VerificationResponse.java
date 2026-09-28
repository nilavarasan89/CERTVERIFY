package com.certverify.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificationResponse {

    private boolean valid;
    private String certificateId;
    private String verificationCode;
    private String participantName;
    private String courseName;
    private LocalDate issuedDate;
    private String status;
    private String message;
}
