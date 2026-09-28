package com.certverify.service;

import com.certverify.dto.CertificateRequest;
import com.certverify.dto.CertificateResponse;
import com.certverify.dto.CourseRequest;
import com.certverify.dto.CourseResponse;
import com.certverify.dto.ParticipantRequest;
import com.certverify.dto.ParticipantResponse;
import com.certverify.dto.RevokeResponse;
import com.certverify.dto.VerificationHistoryResponse;
import com.certverify.dto.VerificationResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class CertificateServiceTest {

    @Autowired
    private ParticipantService participantService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CertificateService certificateService;

    private Long participantId;
    private Long courseId;

    @BeforeEach
    void setUp() {
        ParticipantResponse participant = participantService.createParticipant(
                ParticipantRequest.builder()
                        .name("Rahul Kumar")
                        .email("rahul.test@gmail.com")
                        .phone("9876543210")
                        .build()
        );
        participantId = participant.getId();

        CourseResponse course = courseService.createCourse(
                CourseRequest.builder()
                        .courseName("Java Spring Boot Masterclass")
                        .description("Backend Development Certification")
                        .eventDate(LocalDate.of(2026, 9, 28))
                        .build()
        );
        courseId = course.getId();
    }

    @Test
    @DisplayName("Should successfully issue a certificate with unique ID and verification code")
    void issueCertificate_Success() {
        CertificateRequest request = CertificateRequest.builder()
                .participantId(participantId)
                .courseId(courseId)
                .build();

        CertificateResponse response = certificateService.issueCertificate(request);

        assertNotNull(response.getId());
        assertNotNull(response.getCertificateId());
        assertTrue(response.getCertificateId().startsWith("CERT-"));
        assertNotNull(response.getVerificationCode());
        assertTrue(response.getVerificationCode().startsWith("CV-"));
        assertEquals("Rahul Kumar", response.getParticipantName());
        assertEquals("Java Spring Boot Masterclass", response.getCourseName());
        assertFalse(response.isRevoked());
        assertEquals("VALID", response.getStatus());
    }

    @Test
    @DisplayName("Should return VALID status when verifying an active certificate")
    void verifyCertificate_ValidCode() {
        CertificateResponse cert = certificateService.issueCertificate(
                CertificateRequest.builder().participantId(participantId).courseId(courseId).build()
        );

        VerificationResponse vResponse = certificateService.verifyCertificate(cert.getVerificationCode());

        assertTrue(vResponse.isValid());
        assertEquals("VALID", vResponse.getStatus());
        assertEquals("Certificate is valid and authentic", vResponse.getMessage());
        assertEquals(cert.getCertificateId(), vResponse.getCertificateId());
    }

    @Test
    @DisplayName("Should return INVALID status when verifying a non-existent verification code")
    void verifyCertificate_InvalidCode() {
        VerificationResponse vResponse = certificateService.verifyCertificate("CV-INVALID99");

        assertFalse(vResponse.isValid());
        assertEquals("INVALID", vResponse.getStatus());
        assertEquals("Invalid verification code", vResponse.getMessage());
    }

    @Test
    @DisplayName("Should return REVOKED status when verifying a revoked certificate")
    void verifyCertificate_RevokedCode() {
        CertificateResponse cert = certificateService.issueCertificate(
                CertificateRequest.builder().participantId(participantId).courseId(courseId).build()
        );

        certificateService.revokeCertificate(cert.getId());

        VerificationResponse vResponse = certificateService.verifyCertificate(cert.getVerificationCode());

        assertFalse(vResponse.isValid());
        assertEquals("REVOKED", vResponse.getStatus());
        assertEquals("Certificate has been revoked", vResponse.getMessage());
    }

    @Test
    @DisplayName("Should handle repeated revocation gracefully")
    void revokeCertificate_AlreadyRevoked() {
        CertificateResponse cert = certificateService.issueCertificate(
                CertificateRequest.builder().participantId(participantId).courseId(courseId).build()
        );

        RevokeResponse firstRevoke = certificateService.revokeCertificate(cert.getId());
        assertEquals("Certificate revoked successfully", firstRevoke.getMessage());

        RevokeResponse secondRevoke = certificateService.revokeCertificate(cert.getId());
        assertEquals("Certificate is already revoked", secondRevoke.getMessage());
    }

    @Test
    @DisplayName("Should accurately record and return verification history")
    void getVerificationHistory_Success() {
        CertificateResponse cert = certificateService.issueCertificate(
                CertificateRequest.builder().participantId(participantId).courseId(courseId).build()
        );

        certificateService.verifyCertificate(cert.getVerificationCode());
        certificateService.revokeCertificate(cert.getId());
        certificateService.verifyCertificate(cert.getVerificationCode());

        List<VerificationHistoryResponse> history = certificateService.getVerificationHistory(cert.getId());

        assertEquals(2, history.size());
        assertEquals("INVALID", history.get(0).getResult()); // latest attempt first
        assertEquals("VALID", history.get(1).getResult());
    }
}
