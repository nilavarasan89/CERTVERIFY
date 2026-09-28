package com.certverify.service;

import com.certverify.dto.StatisticsResponse;
import com.certverify.entity.Course;
import com.certverify.exception.ResourceNotFoundException;
import com.certverify.repository.CertificateRepository;
import com.certverify.repository.CourseRepository;
import com.certverify.repository.VerificationRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final VerificationRecordRepository verificationRecordRepository;

    @Transactional(readOnly = true)
    public StatisticsResponse getCourseStatistics(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        long total = certificateRepository.countByCourseId(courseId);
        long valid = certificateRepository.countByCourseIdAndRevokedFalse(courseId);
        long revoked = certificateRepository.countByCourseIdAndRevokedTrue(courseId);

        return StatisticsResponse.builder()
                .courseId(course.getId())
                .courseName(course.getCourseName())
                .totalCertificates(total)
                .validCertificates(valid)
                .revokedCertificates(revoked)
                .build();
    }

    @Transactional(readOnly = true)
    public StatisticsResponse getOverallStatistics() {
        long total = certificateRepository.count();
        long valid = certificateRepository.countByRevokedFalse();
        long revoked = certificateRepository.countByRevokedTrue();
        long totalAttempts = verificationRecordRepository.count();

        return StatisticsResponse.builder()
                .totalCertificates(total)
                .validCertificates(valid)
                .revokedCertificates(revoked)
                .totalVerificationAttempts(totalAttempts)
                .build();
    }
}
