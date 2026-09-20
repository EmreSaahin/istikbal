package com.emresahin.repository;

import com.emresahin.model.EmailRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailRecipientRepository extends JpaRepository<EmailRecipient, Long> {
    Optional<EmailRecipient> findByEmail(String email);
}
