package com.example.comic.service;

import com.example.comic.exception.UnauthenticatedException;
import com.example.comic.model.EmailVerificationOtp;
import com.example.comic.repository.EmailVerificationOtpRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailOtpServiceTest {

    @Mock
    private EmailVerificationOtpRepository emailVerificationOtpRepository;

    @Mock
    private JavaMailSender mailSender;

    private EmailOtpService emailOtpService;

    @BeforeEach
    void setUp() {
        emailOtpService = new EmailOtpService(emailVerificationOtpRepository, mailSender);
        ReflectionTestUtils.setField(emailOtpService, "otpExpirationSeconds", 300L);
        ReflectionTestUtils.setField(emailOtpService, "fromEmail", "no-reply@comic.local");
    }

    @Test
    void issueOtp_shouldSaveRecordAndSendEmail() {
        when(emailVerificationOtpRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(emailVerificationOtpRepository.save(any(EmailVerificationOtp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailOtpService.issueOtp("test@example.com", "Test User");

        ArgumentCaptor<EmailVerificationOtp> captor = ArgumentCaptor.forClass(EmailVerificationOtp.class);
        verify(emailVerificationOtpRepository).save(captor.capture());
        EmailVerificationOtp saved = captor.getValue();
        assertEquals("test@example.com", saved.getEmail());
        assertNotNull(saved.getOtp());
        assertEquals(6, saved.getOtp().length());
        assertNotNull(saved.getExpiresAt());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertEquals("no-reply@comic.local", mailCaptor.getValue().getFrom());
        assertEquals(1, mailCaptor.getValue().getTo().length);
        assertEquals("test@example.com", mailCaptor.getValue().getTo()[0]);
    }

    @Test
    void verifyOtp_shouldDeleteRecordWhenOtpIsCorrect() {
        EmailVerificationOtp record = EmailVerificationOtp
            .builder()
            .email("test@example.com")
            .otp("123456")
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
        when(emailVerificationOtpRepository.findByEmail("test@example.com")).thenReturn(Optional.of(record));

        emailOtpService.verifyOtp("test@example.com", "123456");

        verify(emailVerificationOtpRepository).deleteByEmail("test@example.com");
    }

    @Test
    void verifyOtp_shouldRejectExpiredRecord() {
        EmailVerificationOtp record = EmailVerificationOtp
            .builder()
            .email("test@example.com")
            .otp("123456")
            .expiresAt(Instant.now().minusSeconds(60))
            .build();
        when(emailVerificationOtpRepository.findByEmail("test@example.com")).thenReturn(Optional.of(record));

        assertThrows(UnauthenticatedException.class, () -> emailOtpService.verifyOtp("test@example.com", "123456"));
        verify(emailVerificationOtpRepository).deleteByEmail("test@example.com");
    }

    @Test
    void verifyOtp_shouldRejectWrongOtp() {
        EmailVerificationOtp record = EmailVerificationOtp
            .builder()
            .email("test@example.com")
            .otp("123456")
            .expiresAt(Instant.now().plusSeconds(60))
            .build();
        when(emailVerificationOtpRepository.findByEmail("test@example.com")).thenReturn(Optional.of(record));

        assertThrows(UnauthenticatedException.class, () -> emailOtpService.verifyOtp("test@example.com", "000000"));
        verify(emailVerificationOtpRepository, never()).deleteByEmail(any());
    }

    @Test
    void issueOtp_shouldTranslateMailFailure() {
        when(emailVerificationOtpRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(emailVerificationOtpRepository.save(any(EmailVerificationOtp.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new RuntimeException("mail down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThrows(IllegalStateException.class, () -> emailOtpService.issueOtp("test@example.com", "Test User"));
    }

    @Test
    void issueOtp_shouldUseDefaultDisplayNameWhenFullNameBlank() {
        when(emailVerificationOtpRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(emailVerificationOtpRepository.save(any(EmailVerificationOtp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailOtpService.issueOtp("test@example.com", "   ");

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertTrue(mailCaptor.getValue().getText().contains("Xin chào bạn"));
    }

    @Test
    void issueOtp_shouldUseDefaultDisplayNameWhenFullNameNull() {
        when(emailVerificationOtpRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(emailVerificationOtpRepository.save(any(EmailVerificationOtp.class))).thenAnswer(invocation -> invocation.getArgument(0));

        emailOtpService.issueOtp("test@example.com", null);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertTrue(mailCaptor.getValue().getText().contains("Xin chào bạn"));
    }
}
