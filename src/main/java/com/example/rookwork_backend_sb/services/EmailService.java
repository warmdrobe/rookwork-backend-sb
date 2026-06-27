package com.example.rookwork_backend_sb.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final SesClient sesClient;

    @Value("${app.email.from}")
    private String fromEmail;

    public void sendProjectInvitation(String toEmail, String projectName, String invitedByName) {
        String htmlBody = """
                    <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                        <h2>Bạn được mời vào project: %s</h2>
                        <p><b>%s</b> đã mời bạn tham gia project này trên Rookwork.</p>
                        <p>Vào ứng dụng để chấp nhận hoặc từ chối lời mời.</p>
                    </div>
                """.formatted(projectName, invitedByName);

        String fromDisplay = invitedByName + " via Rookwork <" + fromEmail + ">";

        try {
            sesClient.sendEmail(SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data("[Rookwork] " + invitedByName + " mời bạn vào project: " + projectName)
                                    .charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                    .build())
                            .build())
                    .source(fromDisplay)
                    .build());
            log.info("Project invitation email successfully sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send project invitation email via AWS SES to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendPasswordSetupOtp(String toEmail, String otp) {
        String htmlBody = """
                    <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                        <h2>Thiết lập mật khẩu Rookwork</h2>
                        <p>Mã xác nhận (OTP) của bạn là: <b style="font-size:24px;color:#4f46e5">%s</b></p>
                        <p>Mã này có hiệu lực trong 10 phút. Vui lòng không chia sẻ mã này với người khác.</p>
                    </div>
                """.formatted(otp);
        try {
            sesClient.sendEmail(SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data("[Rookwork] Mã xác nhận thiết lập mật khẩu")
                                    .charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                    .build())
                            .build())
                    .source("Rookwork <" + fromEmail + ">")
                    .build());
            log.info("Password setup OTP email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password setup OTP to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String name) {
        String htmlBody = """
                    <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                        <h2>Chào mừng bạn đến với Rookwork!</h2>
                        <p>Xin chào <b>%s</b>,</p>
                        <p>Cảm ơn bạn đã đăng ký tài khoản trên hệ thống Rookwork.</p>
                        <p><i>Lưu ý: Nếu bạn không thực hiện việc đăng ký này, vui lòng bỏ qua email này hoặc liên hệ với bộ phận hỗ trợ của chúng tôi.</i></p>
                    </div>
                """
                .formatted(name != null ? name : "bạn");
        try {
            sesClient.sendEmail(SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .data("[Rookwork] Chào mừng bạn gia nhập!")
                                    .charset("UTF-8").build())
                            .body(Body.builder()
                                    .html(Content.builder().data(htmlBody).charset("UTF-8").build())
                                    .build())
                            .build())
                    .source("Rookwork <" + fromEmail + ">")
                    .build());
            log.info("Welcome email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}