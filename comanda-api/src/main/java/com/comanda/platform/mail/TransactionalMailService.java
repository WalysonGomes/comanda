package com.comanda.platform.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Wraps {@link JavaMailSender} so a down SMTP provider never brings the application down
 * (deploy-vps 6.3, PRD 4.5 "nenhum estado silencioso de falha"). The recipient address is never
 * logged (PRD Regra 14 — no PII in logs).
 */
@Component
public class TransactionalMailService {

    private static final Logger log = LoggerFactory.getLogger(TransactionalMailService.class);

    private final JavaMailSender mailSender;

    public TransactionalMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("Falha ao enviar e-mail transacional (provedor SMTP indisponível): {}", e.getMessage());
        }
    }
}
