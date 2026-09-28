package com.certverify.repository;

import com.certverify.entity.VerificationRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VerificationRecordRepository extends JpaRepository<VerificationRecord, Long> {
    List<VerificationRecord> findByCertificateIdOrderByVerifiedAtDesc(Long certificateId);
    List<VerificationRecord> findBySearchedCodeOrderByVerifiedAtDesc(String searchedCode);
}
