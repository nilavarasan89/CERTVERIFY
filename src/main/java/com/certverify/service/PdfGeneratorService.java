package com.certverify.service;

import com.certverify.entity.Certificate;
import java.io.ByteArrayOutputStream;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

    private final QrCodeGeneratorService qrCodeGeneratorService;

    public byte[] generateCertificatePdf(Certificate certificate, String verificationUrl) {
        try (PDDocument document = new PDDocument()) {
            // Create landscape A4 page
            PDRectangle landscapeA4 = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            PDPage page = new PDPage(landscapeA4);
            document.addPage(page);

            float pageWidth = landscapeA4.getWidth();
            float pageHeight = landscapeA4.getHeight();

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                PDType1Font italicFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

                // Draw Decorative Border
                contentStream.setLineWidth(3f);
                contentStream.addRect(20, 20, pageWidth - 40, pageHeight - 40);
                contentStream.setStrokingColor(41, 128, 185); // Professional Blue border
                contentStream.stroke();

                contentStream.setLineWidth(1f);
                contentStream.addRect(25, 25, pageWidth - 50, pageHeight - 50);
                contentStream.setStrokingColor(52, 73, 94);
                contentStream.stroke();

                // Institution Name
                contentStream.beginText();
                contentStream.setFont(titleFont, 24);
                contentStream.setNonStrokingColor(41, 128, 185);
                String instTitle = "CertVerify Academic Institution";
                float instWidth = titleFont.getStringWidth(instTitle) / 1000 * 24;
                contentStream.newLineAtOffset((pageWidth - instWidth) / 2, pageHeight - 80);
                contentStream.showText(instTitle);
                contentStream.endText();

                // Certificate Header
                contentStream.beginText();
                contentStream.setFont(titleFont, 28);
                contentStream.setNonStrokingColor(44, 62, 80);
                String certHeader = "CERTIFICATE OF COMPLETION";
                float certHeaderWidth = titleFont.getStringWidth(certHeader) / 1000 * 28;
                contentStream.newLineAtOffset((pageWidth - certHeaderWidth) / 2, pageHeight - 130);
                contentStream.showText(certHeader);
                contentStream.endText();

                // Subtitle: This is to certify that
                contentStream.beginText();
                contentStream.setFont(italicFont, 16);
                contentStream.setNonStrokingColor(127, 140, 141);
                String subtitle = "This is to certify that";
                float subtitleWidth = italicFont.getStringWidth(subtitle) / 1000 * 16;
                contentStream.newLineAtOffset((pageWidth - subtitleWidth) / 2, pageHeight - 170);
                contentStream.showText(subtitle);
                contentStream.endText();

                // Participant Name
                contentStream.beginText();
                contentStream.setFont(titleFont, 26);
                contentStream.setNonStrokingColor(41, 128, 185);
                String pName = certificate.getParticipant().getName();
                float pNameWidth = titleFont.getStringWidth(pName) / 1000 * 26;
                contentStream.newLineAtOffset((pageWidth - pNameWidth) / 2, pageHeight - 215);
                contentStream.showText(pName);
                contentStream.endText();

                // Participant Description
                contentStream.beginText();
                contentStream.setFont(regularFont, 14);
                contentStream.setNonStrokingColor(44, 62, 80);
                String descText = "has successfully completed the course/event";
                float descWidth = regularFont.getStringWidth(descText) / 1000 * 14;
                contentStream.newLineAtOffset((pageWidth - descWidth) / 2, pageHeight - 250);
                contentStream.showText(descText);
                contentStream.endText();

                // Course Name
                contentStream.beginText();
                contentStream.setFont(titleFont, 22);
                contentStream.setNonStrokingColor(39, 174, 96);
                String cName = certificate.getCourse().getCourseName();
                float cNameWidth = titleFont.getStringWidth(cName) / 1000 * 22;
                contentStream.newLineAtOffset((pageWidth - cNameWidth) / 2, pageHeight - 290);
                contentStream.showText(cName);
                contentStream.endText();

                // Issue Date & Status
                contentStream.beginText();
                contentStream.setFont(regularFont, 12);
                contentStream.setNonStrokingColor(44, 62, 80);
                String issueInfo = "Issued on: " + certificate.getIssuedDate() + " | Status: " + (certificate.isRevoked() ? "REVOKED" : "VALID");
                float issueWidth = regularFont.getStringWidth(issueInfo) / 1000 * 12;
                contentStream.newLineAtOffset((pageWidth - issueWidth) / 2, pageHeight - 330);
                contentStream.showText(issueInfo);
                contentStream.endText();

                // Certificate ID & Verification Code (Bottom Left)
                contentStream.beginText();
                contentStream.setFont(regularFont, 11);
                contentStream.setNonStrokingColor(127, 140, 141);
                contentStream.newLineAtOffset(50, 90);
                contentStream.showText("Certificate ID: " + certificate.getCertificateId());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(regularFont, 11);
                contentStream.setNonStrokingColor(127, 140, 141);
                contentStream.newLineAtOffset(50, 72);
                contentStream.showText("Verification Code: " + certificate.getVerificationCode());
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(regularFont, 9);
                contentStream.setNonStrokingColor(127, 140, 141);
                contentStream.newLineAtOffset(50, 54);
                contentStream.showText("Verify online at: " + verificationUrl);
                contentStream.endText();

                // Embed QR Code (Bottom Right)
                try {
                    byte[] qrImageBytes = qrCodeGeneratorService.generateQrCodeImage(verificationUrl, 110, 110);
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, qrImageBytes, "QR Code");
                    contentStream.drawImage(pdImage, pageWidth - 160, 45, 110, 110);
                } catch (Exception e) {
                    // Fallback if QR generation encounters an issue
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF certificate: " + e.getMessage(), e);
        }
    }
}
