package com.certverify.repository;

import com.certverify.entity.Participant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    boolean existsByEmail(String email);
    Optional<Participant> findByEmail(String email);
}
