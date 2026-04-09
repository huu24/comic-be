package com.example.comic.repository;

import com.example.comic.model.EmailVerificationOtp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationOtpRepository extends JpaRepository<EmailVerificationOtp, Long> {
    Optional<EmailVerificationOtp> findByEmail(String email);

    void deleteByEmail(String email);
}
