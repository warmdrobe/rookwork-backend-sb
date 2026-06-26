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
}