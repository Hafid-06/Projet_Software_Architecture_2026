package com.example.demo.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Service responsable de l'envoi d'e-mails via MailHog (serveur SMTP simulé)
@Service
public class EmailService {

    // Logger pour tracer les envois d'email dans la console
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    // Interface Spring pour envoyer des emails (injectée automatiquement, configurée dans application.properties)
    private final JavaMailSender mailSender;

    // Adresse expéditeur lue depuis application.properties (app.mail.from = noreply@auth-demo.local)
    @Value("${app.mail.from}")
    private String from;

    // URL de base pour le lien de vérification (ex: http://localhost)
    // Lue depuis la variable d'env APP_MAIL_AUTH_BASE_URL ou application.properties
    @Value("${app.mail.auth-base-url}")
    private String authBaseUrl;

    // Injection du JavaMailSender par constructeur (Spring le fournit automatiquement)
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Construit et envoie l'e-mail de vérification à l'utilisateur.
     *
     * Étapes :
     * 1. Construit l'URL de vérification : http://localhost/verify?tokenId=XXX&t=YYY
     *    - tokenId : identifiant public du token (pour le retrouver en BDD)
     *    - t : le token en clair (sera comparé au hash BCrypt stocké en BDD)
     * 2. Crée un SimpleMailMessage avec from, to, sujet et corps
     * 3. Envoie l'email via JavaMailSender → MailHog (port 1025)
     * 4. L'email est visible dans l'interface web MailHog (http://localhost:8025)
     *
     * @param to         adresse email du destinataire
     * @param tokenId    identifiant UUID du token (clé primaire en BDD)
     * @param tokenClear token en clair (envoyé dans l'URL, jamais stocké en BDD)
     */
    public void sendVerificationEmail(String to, String tokenId, String tokenClear) {
        // Construction de l'URL complète que l'utilisateur cliquera dans l'email
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