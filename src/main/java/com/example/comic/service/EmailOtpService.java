package com.example.comic.service;

import com.example.comic.exception.UnauthenticatedException;
import com.example.comic.model.EmailVerificationOtp;
import com.example.comic.repository.EmailVerificationOtpRepository;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailOtpService {

    private final EmailVerificationOtpRepository emailVerificationOtpRepository;
    private final JavaMailSender mailSender;

    @Value("${application.security.otp.expiration-seconds:300}")
    private long otpExpirationSeconds;

    @Value("${application.security.otp.mail.from:no-reply@comic.local}")
    private String fromEmail;

    @Transactional
    public void issueOtp(String email, String fullName) {
        String otp = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        Instant expiresAt = Instant.now().plusSeconds(otpExpirationSeconds);

        EmailVerificationOtp record = emailVerificationOtpRepository
            .findByEmail(email)
            .orElse(EmailVerificationOtp.builder().email(email).build());

        record.setOtp(otp);
        record.setExpiresAt(expiresAt);
        emailVerificationOtpRepository.save(record);

        sendOtpEmail(email, fullName, otp);
    }

    @Transactional
    public void verifyOtp(String email, String otp) {
        EmailVerificationOtp record = emailVerificationOtpRepository
            .findByEmail(email)
            .orElseThrow(() -> new UnauthenticatedException("Mã OTP không chính xác hoặc đã hết hạn."));

        if (record.getExpiresAt().isBefore(Instant.now())) {
            emailVerificationOtpRepository.deleteByEmail(email);
            throw new UnauthenticatedException("Mã OTP đã hết hạn. Vui lòng dùng chức năng gửi lại OTP.");
        }

        if (!record.getOtp().equals(otp)) {
            throw new UnauthenticatedException("Mã OTP không chính xác.");
        }

        emailVerificationOtpRepository.deleteByEmail(email);
    }

    private void sendOtpEmail(String email, String fullName, String otp) {
        String displayName = (fullName == null || fullName.isBlank()) ? "bạn" : fullName;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("[Comic] Mã OTP xác nhận đăng ký");
        message.setText(
            "Xin chào " +
            displayName +
            ",\n\nMã OTP xác nhận tài khoản của bạn là: " +
            otp +
            "\nMã có hiệu lực trong " +
            (otpExpirationSeconds / 60) +
            " phút.\n\nNếu bạn không thực hiện đăng ký, vui lòng bỏ qua email này."
        );

        try {
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể gửi OTP qua email. Vui lòng thử lại sau.");
        }
    }
}
