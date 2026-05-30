package com.mdounzaidi.portfolio_backend.account.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class AccountMailService {
    final private JavaMailSender javaMailSender;

    @Value("${app.base-url}")
    private String appBaseUrl;

    public AccountMailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendEmailVerificationMail(String to, String token) {
        String link = appBaseUrl+"/api/public/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Verify your account");
        message.setText("Click to verify: " + link);

        javaMailSender.send(message);
    }

    public void sendAccountInviteMail(String to, String token){
        String link =appBaseUrl+"/api/public/complete-invite?token="+token;
        SimpleMailMessage message =new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Join us");
        message.setText("click here to create your account"+link);

        javaMailSender.send(message);
    }

    public void sendPasswordResetMail(String to, String token){
        String link =appBaseUrl+"/api/public/reset-password/update?token="+token;
        SimpleMailMessage message =new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Reset Password");
        message.setText("click here to reset your account password"+link);

        javaMailSender.send(message);
    }

}


//    public void sendHelloMAil(){
//        SimpleMailMessage message=new SimpleMailMessage();
//        message.setTo("mdounzaidi@gmail.com");
//        message.setSubject("Test Mail");
//        message.setText("Hello World");
//        message.setFrom("mailservice4portfolio@gmail.com");
//
//        javaMailSender.send(message);
//    }
//}
