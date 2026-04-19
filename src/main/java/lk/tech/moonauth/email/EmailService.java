package lk.tech.moonauth.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService{

    private final EmailClient emailClient;

    public void sendVerificationEmail(String to, String token) {
        log.info("Sending verification email to {}", to);
        sendEmail(to, EmailTemplate.VERIFICATION_EMAIL, token);
    }

    public void sendPasswordResetEmail(String to, String token) {
        log.info("Sending password reset email to {}", to);
        sendEmail(to, EmailTemplate.RESET_PASSWORD, token);
    }

    private void sendEmail(String to, EmailTemplate templateKey, String token) {
        try {
            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(to);
            emailRequest.setTemplateKey(templateKey.name());
            emailRequest.setToken(token);
            emailClient.send(emailRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
