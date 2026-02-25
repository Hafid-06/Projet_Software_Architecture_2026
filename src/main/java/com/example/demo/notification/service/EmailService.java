package com.example.demo.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Value("${app.mail.auth-base-url}")
    private String authBaseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Construit et envoie l'e-mail de vérification.
     *
     * Le lien a la forme :
     *   http://localhost:8080/verify?tokenId=XXX&t=YYY
     *
     * tokenId : identifiant public (pour retrouver le hash en base)
     * t       : le token en clair (comparé au hash via BCrypt côté Auth)
     */
    public void sendVerificationEmail(String to, String tokenId, String tokenClear) {
        String verifyUrl = authBaseUrl + "/verify?tokenId=" + tokenId + "&t=" + tokenClear;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Vérifiez votre adresse e-mail");
        message.setText(
                "Bonjour,\n\n" +
                "Merci de vous être inscrit(e).\n\n" +
                "Cliquez sur le lien ci-dessous pour vérifier votre adresse e-mail " +
                "(valable 30 minutes) :\n\n" +
                verifyUrl + "\n\n" +
                "Si vous n'êtes pas à l'origine de cette inscription, ignorez cet e-mail.\n\n" +
                "— L'équipe Auth Demo"
        );

        mailSender.send(message);
        log.info("[NOTIFICATION] E-mail envoyé à {} avec tokenId={}", to, tokenId);
    }
}