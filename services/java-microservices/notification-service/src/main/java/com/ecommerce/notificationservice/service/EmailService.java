package com.ecommerce.notificationservice.service;

import com.ecommerce.notificationservice.exception.EmailSendException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.from-name}")
    private String fromName;

    /**
     * Sends an HTML email using a Thymeleaf template.
     *
     * @param to           recipient email address
     * @param subject      email subject
     * @param templateName Thymeleaf template name (without extension)
     * @param variables    template variables
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        log.info("Sending email to={}, subject={}, template={}", to, subject, templateName);

        Context context = new Context();
        context.setVariables(variables);

        String htmlContent = templateEngine.process(templateName, context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to={}", to);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to={}, subject={}: {}", to, subject, e.getMessage());
            throw new EmailSendException("Failed to send email to " + to, e);
        }
    }
}
