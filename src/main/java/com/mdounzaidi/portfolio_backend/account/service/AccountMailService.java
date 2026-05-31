package com.mdounzaidi.portfolio_backend.account.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AccountMailService {
    private static final Logger log = LoggerFactory.getLogger(AccountMailService.class);

    final private JavaMailSender javaMailSender;

    @Value("${app.base-url}")
    private String appBaseUrl;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    public AccountMailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendEmailVerificationMail(String to, String token) {
        String link = appBaseUrl + "/api/public/verify?token=" + token;
        sendMail(to, "Verify your account", "Click to verify: " + link, link);
    }

    public void sendAccountInviteMail(String to, String token){
        String link = appBaseUrl + "/api/public/complete-invite?token=" + token;
        sendMail(to, "Join us", "Click here to create your account: " + link, link);
    }

    public void sendPasswordResetMail(String to, String token){
        String link = appBaseUrl + "/api/public/reset-password/update?token=" + token;
        sendMail(to, "Reset Password", "Click here to reset your account password: " + link, link);
    }

    private void sendMail(String to, String subject, String text, String link) {
        if (!mailEnabled) {
            log.info("Mail sending disabled. {} email for {}: {}", subject, to, link);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        javaMailSender.send(message);
    }

}
