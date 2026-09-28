package com.certverify.repository;

import com.certverify.entity.Certificate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByVerificationCode(String verificationCode);
    boolean existsByVerificationCode(String verificationCode);
    Optional<Certificate> findByCertificateId(String certificateId);
    boolean existsByCertificateId(String certificateId);

    List<Certificate> findByParticipantId(Long participantId);
    List<Certificate> findByCourseId(Long courseId);

    long countByCourseId(Long courseId);
    long countByCourseIdAndRevokedFalse(Long courseId);
    long countByCourseIdAndRevokedTrue(Long courseId);

    long countByRevokedFalse();
    long countByRevokedTrue();
}
