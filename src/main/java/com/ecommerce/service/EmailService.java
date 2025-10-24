package com.ecommerce.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML email sent successfully to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    public void sendOtpEmail(String to, String otp) {
        String subject = "OTP for Verification";
        String body = String.format("""
            Hello,
            
            Your OTP for verification is: %s
            
            This OTP is valid for 10 minutes.
            
            If you did not request this, please ignore this email.
            
            Regards,
            SHOPVERSE Team
            """, otp);

        sendSimpleEmail(to, subject, body);
    }

    public void sendAdminApprovalRequest(String adminEmail, String username, String email,
                                         String phone, String address,
                                         String confirmUrl, String rejectUrl) {
        String subject = "Admin Registration Approval Needed";
        String htmlContent = String.format("""
            <html>
            <body>
                <h2>New Admin Registration Request</h2>
                <p>A new admin registration request has been submitted.</p>
                <table border="1" cellpadding="10">
                    <tr><td><strong>Name:</strong></td><td>%s</td></tr>
                    <tr><td><strong>Email:</strong></td><td>%s</td></tr>
                    <tr><td><strong>Phone:</strong></td><td>%s</td></tr>
                    <tr><td><strong>Address:</strong></td><td>%s</td></tr>
                </table>
                <br>
                <p>
                    <a href="%s" style="background-color: #4CAF50; color: white; padding: 10px 20px; 
                       text-decoration: none; border-radius: 5px;">✅ APPROVE</a>
                    &nbsp;&nbsp;
                    <a href="%s" style="background-color: #f44336; color: white; padding: 10px 20px; 
                       text-decoration: none; border-radius: 5px;">❌ REJECT</a>
                </p>
            </body>
            </html>
            """, username, email, phone, address, confirmUrl, rejectUrl);

        sendHtmlEmail(adminEmail, subject, htmlContent);
    }

    public void sendAdminApprovalEmail(String to, String username, String email, String phone) {
        String subject = "Admin Registration Approved";
        String body = String.format("""
            Hi %s,
            
            Your request to become an admin has been approved ✅.
            
            You can now log in using your registered email: %s
            Phone: %s
            
            Thank you and welcome aboard!
            
            Regards,
            Admin Team
            """, username, email, phone);

        sendSimpleEmail(to, subject, body);
    }

    public void sendAdminRejectionEmail(String to, String username) {
        String subject = "Admin Registration Rejected";
        String body = String.format("""
            Hi %s,
            
            We regret to inform you that your admin registration request has been ❌ rejected.
            
            If you believe this is a mistake or have any questions, please contact us.
            
            Regards,
            Admin Team
            """, username);

        sendSimpleEmail(to, subject, body);
    }
}