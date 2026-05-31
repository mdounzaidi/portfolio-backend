package com.mdounzaidi.portfolio_backend.account.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AccountMailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    private AccountMailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new AccountMailService(javaMailSender);
        ReflectionTestUtils.setField(mailService, "appBaseUrl", "http://localhost:8080");
    }

    @Test
    void sendEmailVerificationMail_shouldNotSendMail_whenMailIsDisabled() {
        ReflectionTestUtils.setField(mailService, "mailEnabled", false);

        mailService.sendEmailVerificationMail("test@example.com", "raw-token");

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void sendEmailVerificationMail_shouldSendVerificationMessage_whenMailIsEnabled() {
        ReflectionTestUtils.setField(mailService, "mailEnabled", true);
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        mailService.sendEmailVerificationMail("test@example.com", "raw-token");

        verify(javaMailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertArrayEquals(new String[]{"test@example.com"}, message.getTo());
        assertEquals("Verify your account", message.getSubject());
        assertTrue(message.getText().contains("http://localhost:8080/api/public/verify?token=raw-token"));
    }

    @Test
    void sendPasswordResetMail_shouldSendResetMessage_whenMailIsEnabled() {
        ReflectionTestUtils.setField(mailService, "mailEnabled", true);
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        mailService.sendPasswordResetMail("test@example.com", "reset-token");

        verify(javaMailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("Reset Password", message.getSubject());
        assertTrue(message.getText().contains("http://localhost:8080/api/public/reset-password/update?token=reset-token"));
    }
}
