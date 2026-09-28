package com.certverify.controller;

import com.certverify.dto.CertificateRequest;
import com.certverify.dto.CertificateResponse;
import com.certverify.dto.RevokeResponse;
import com.certverify.dto.VerificationHistoryResponse;
import com.certverify.dto.VerificationResponse;
import com.certverify.service.CertificateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping
    public ResponseEntity<CertificateResponse> issueCertificate(@Valid @RequestBody CertificateRequest request) {
        CertificateResponse response = certificateService.issueCertificate(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CertificateResponse> getCertificateById(@PathVariable Long id) {
        return ResponseEntity.ok(certificateService.getCertificateById(id));
    }

    @GetMapping("/verify/{verificationCode}")
    public ResponseEntity<VerificationResponse> verifyCertificate(@PathVariable String verificationCode) {
        VerificationResponse response = certificateService.verifyCertificate(verificationCode);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/revoke")
    public ResponseEntity<RevokeResponse> revokeCertificate(@PathVariable Long id) {
        RevokeResponse response = certificateService.revokeCertificate(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/verification-history")
    public ResponseEntity<List<VerificationHistoryResponse>> getVerificationHistory(@PathVariable Long id) {
        List<VerificationHistoryResponse> history = certificateService.getVerificationHistory(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadCertificatePdf(@PathVariable Long id, HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        byte[] pdfBytes = certificateService.getCertificatePdf(id, baseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "certificate-" + id + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> getCertificateQrCode(@PathVariable Long id, HttpServletRequest request) throws Exception {
        String baseUrl = getBaseUrl(request);
        byte[] qrBytes = certificateService.getCertificateQrCode(id, baseUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(qrBytes, headers, HttpStatus.OK);
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        return scheme + "://" + serverName + (serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort);
    }
}
