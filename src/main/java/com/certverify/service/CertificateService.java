package com.certverify.service;

import com.certverify.dto.CertificateRequest;
import com.certverify.dto.CertificateResponse;
import com.certverify.dto.RevokeResponse;
import com.certverify.dto.VerificationHistoryResponse;
import com.certverify.dto.VerificationResponse;
import com.certverify.entity.Certificate;
import com.certverify.entity.Course;
import com.certverify.entity.Participant;
import com.certverify.entity.VerificationRecord;
import com.certverify.exception.ResourceNotFoundException;
import com.certverify.repository.CertificateRepository;
import com.certverify.repository.CourseRepository;
import com.certverify.repository.ParticipantRepository;
import com.certverify.repository.VerificationRecordRepository;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final ParticipantRepository participantRepository;
    private final CourseRepository courseRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final QrCodeGeneratorService qrCodeGeneratorService;

    private static final String CODE_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public CertificateResponse issueCertificate(CertificateRequest request) {
        Participant participant = participantRepository.findById(request.getParticipantId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found with id: " + request.getParticipantId()));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));

        String certificateId = generateUniqueCertificateId();
        String verificationCode = generateUniqueVerificationCode();

        Certificate certificate = Certificate.builder()
                .certificateId(certificateId)
                .verificationCode(verificationCode)
                .participant(participant)
                .course(course)
                .issuedDate(LocalDate.now())
                .revoked(false)
                .build();

        Certificate saved = certificateRepository.save(certificate);
        return mapToResponse(saved);
    }

    @Transactional
    public VerificationResponse verifyCertificate(String verificationCode) {
        Optional<Certificate> certOpt = certificateRepository.findByVerificationCode(verificationCode);
        LocalDateTime now = LocalDateTime.now();

        if (certOpt.isEmpty()) {
            // Record failed verification attempt
            VerificationRecord record = VerificationRecord.builder()
                    .certificate(null)
                    .searchedCode(verificationCode)
                    .verifiedAt(now)
                    .result("INVALID")
                    .build();
            verificationRecordRepository.save(record);

            return VerificationResponse.builder()
                    .valid(false)
                    .status("INVALID")
                    .message("Invalid verification code")
                    .build();
        }

        Certificate certificate = certOpt.get();

        if (certificate.isRevoked()) {
            // Record revoked verification attempt
            VerificationRecord record = VerificationRecord.builder()
                    .certificate(certificate)
                    .searchedCode(verificationCode)
                    .verifiedAt(now)
                    .result("INVALID")
                    .build();
            verificationRecordRepository.save(record);

            return VerificationResponse.builder()
                    .valid(false)
                    .certificateId(certificate.getCertificateId())
                    .verificationCode(certificate.getVerificationCode())
                    .participantName(certificate.getParticipant().getName())
                    .courseName(certificate.getCourse().getCourseName())
                    .issuedDate(certificate.getIssuedDate())
                    .status("REVOKED")
                    .message("Certificate has been revoked")
                    .build();
        }

        // Record valid verification attempt
        VerificationRecord record = VerificationRecord.builder()
                .certificate(certificate)
                .searchedCode(verificationCode)
                .verifiedAt(now)
                .result("VALID")
                .build();
        verificationRecordRepository.save(record);

        return VerificationResponse.builder()
                .valid(true)
                .certificateId(certificate.getCertificateId())
                .verificationCode(certificate.getVerificationCode())
                .participantName(certificate.getParticipant().getName())
                .courseName(certificate.getCourse().getCourseName())
                .issuedDate(certificate.getIssuedDate())
                .status("VALID")
                .message("Certificate is valid and authentic")
                .build();
    }

    @Transactional
    public RevokeResponse revokeCertificate(Long id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with id: " + id));

        if (certificate.isRevoked()) {
            return RevokeResponse.builder()
                    .message("Certificate is already revoked")
                    .certificateId(certificate.getCertificateId())
                    .status("REVOKED")
                    .build();
        }

        certificate.setRevoked(true);
        certificateRepository.save(certificate);

        return RevokeResponse.builder()
                .message("Certificate revoked successfully")
                .certificateId(certificate.getCertificateId())
                .status("REVOKED")
                .build();
    }

    @Transactional(readOnly = true)
    public CertificateResponse getCertificateById(Long id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with id: " + id));
        return mapToResponse(certificate);
    }

    @Transactional(readOnly = true)
    public List<VerificationHistoryResponse> getVerificationHistory(Long id) {
        // Ensure certificate exists
        if (!certificateRepository.existsById(id)) {
            throw new ResourceNotFoundException("Certificate not found with id: " + id);
        }

        List<VerificationRecord> records = verificationRecordRepository.findByCertificateIdOrderByVerifiedAtDesc(id);

        return records.stream()
                .map(rec -> VerificationHistoryResponse.builder()
                        .verifiedAt(rec.getVerifiedAt())
                        .result(rec.getResult())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public byte[] getCertificatePdf(Long id, String baseUrl) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with id: " + id));
        String verifyUrl = baseUrl + "/api/certificates/verify/" + certificate.getVerificationCode();
        return pdfGeneratorService.generateCertificatePdf(certificate, verifyUrl);
    }

    @Transactional(readOnly = true)
    public byte[] getCertificateQrCode(Long id, String baseUrl) throws Exception {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with id: " + id));
        String verifyUrl = baseUrl + "/api/certificates/verify/" + certificate.getVerificationCode();
        return qrCodeGeneratorService.generateQrCodeImage(verifyUrl, 250, 250);
    }

    private String generateUniqueCertificateId() {
        long count = certificateRepository.count();
        long nextSeq = 100001 + count;
        String candidateId = String.format("CERT-%06d", nextSeq);

        while (certificateRepository.existsByCertificateId(candidateId)) {
            nextSeq++;
            candidateId = String.format("CERT-%06d", nextSeq);
        }
        return candidateId;
    }

    private String generateUniqueVerificationCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder("CV-");
            for (int i = 0; i < 8; i++) {
                sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            code = sb.toString();
        } while (certificateRepository.existsByVerificationCode(code));
        return code;
    }

    public CertificateResponse mapToResponse(Certificate cert) {
        return CertificateResponse.builder()
                .id(cert.getId())
                .certificateId(cert.getCertificateId())
                .verificationCode(cert.getVerificationCode())
                .participantId(cert.getParticipant().getId())
                .participantName(cert.getParticipant().getName())
                .participantEmail(cert.getParticipant().getEmail())
                .courseId(cert.getCourse().getId())
                .courseName(cert.getCourse().getCourseName())
                .issuedDate(cert.getIssuedDate())
                .revoked(cert.isRevoked())
                .status(cert.isRevoked() ? "REVOKED" : "VALID")
                .build();
    }
}
