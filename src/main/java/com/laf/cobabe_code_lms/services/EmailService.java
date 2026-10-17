
package com.laf.cobabe_code_lms.services;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromAddress) {

        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendEmail(
            String recipient,
            String subject,
            String message) {

        SimpleMailMessage email = new SimpleMailMessage();

        email.setFrom(fromAddress);
        email.setTo(recipient);
        email.setSubject(subject);
        email.setText(message);

        mailSender.send(email);
    }
}
