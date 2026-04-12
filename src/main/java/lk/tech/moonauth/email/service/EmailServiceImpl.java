package lk.tech.moonauth.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    public void sendVerificationEmail(String to, String token) {
        log.info("Sending verification email to {}", to);
        sendEmail(to, "Confirm your email", "verification-email", token);
    }

    @Override
    public void sendPasswordResetEmail(String to, String token) {
        log.info("Sending password reset email to {}", to);
        sendEmail(to, "Reset your password", "password-reset-email", token);
    }

    private void sendEmail(String to, String subject, String templateName, String token) {
        try {

            System.out.println("Sending email TOKEN:" + token);
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

            Context context = new Context();
            context.setVariable("token", token);
            String html = templateEngine.process(templateName, context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.setFrom("noreply@moonauth.tech");

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
            throw new RuntimeException("Failed to send email");
        }
    }
}
