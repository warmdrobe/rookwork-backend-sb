package com.example.rookwork_backend_sb.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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

    private final TemplateEngine templateEngine;
    private final SesClient sesClient;

    @Value("${app.email.from:no-reply@rookwork.asia}")
    private String fromEmail;

    @Async("emailExecutor")
    public void sendIssueAssignment(String toEmail, String issueName, String projectName,
                                     String assignedByName, String issueUrl) {
        Context ctx = new Context();
        ctx.setVariable("issueName", issueName);
        ctx.setVariable("projectName", projectName);
        ctx.setVariable("assignedByName", assignedByName);
        ctx.setVariable("issueUrl", issueUrl);

        try {
            String html = templateEngine.process("email/issue-assignment", ctx);
            send(toEmail, "New task assigned: " + issueName, html);
        } catch (Exception e) {
            log.error("Failed to process/send issue assignment email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async("emailExecutor")
    public void sendCommentNotification(String toEmail, String issueName, String projectName,
                                         String commentByName, String commentContent, String issueUrl,
                                         boolean isReply) {
        Context ctx = new Context();
        ctx.setVariable("issueName", issueName);
        ctx.setVariable("projectName", projectName);
        ctx.setVariable("commentByName", commentByName);
        ctx.setVariable("commentContent", commentContent);
        ctx.setVariable("issueUrl", issueUrl);
        ctx.setVariable("isReply", isReply);

        String subject = isReply 
            ? "New reply on task: " + issueName 
            : "New comment on task: " + issueName;

        try {
            String html = templateEngine.process("email/comment-notification", ctx);
            send(toEmail, subject, html);
        } catch (Exception e) {
            log.error("Failed to process/send comment notification email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async("emailExecutor")
    public void sendEventInvitation(String toEmail, String eventName, String eventDescription,
                                     String eventTime, String location, String creatorName,
                                     String projectName, String calendarUrl) {
        Context ctx = new Context();
        ctx.setVariable("eventName", eventName);
        ctx.setVariable("eventDescription", eventDescription);
        ctx.setVariable("eventTime", eventTime);
        ctx.setVariable("location", location);
        ctx.setVariable("creatorName", creatorName);
        ctx.setVariable("projectName", projectName);
        ctx.setVariable("calendarUrl", calendarUrl);

        try {
            String html = templateEngine.process("email/event-invitation", ctx);
            send(toEmail, "Event Invitation: " + eventName, html);
        } catch (Exception e) {
            log.error("Failed to process/send event invitation email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async("emailExecutor")
    public void sendEventUpdated(String toEmail, String eventName, String eventTime,
                                  String location, String updatedByName, String changeSummary,
                                  String calendarUrl) {
        Context ctx = new Context();
        ctx.setVariable("eventName", eventName);
        ctx.setVariable("eventTime", eventTime);
        ctx.setVariable("location", location);
        ctx.setVariable("updatedByName", updatedByName);
        ctx.setVariable("changeSummary", changeSummary);
        ctx.setVariable("calendarUrl", calendarUrl);

        try {
            String html = templateEngine.process("email/event-updated", ctx);
            send(toEmail, "Event Updated: " + eventName, html);
        } catch (Exception e) {
            log.error("Failed to process/send event updated email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async("emailExecutor")
    public void sendEventCancelled(String toEmail, String eventName, String eventTime,
                                    String cancelledByName) {
        Context ctx = new Context();
        ctx.setVariable("eventName", eventName);
        ctx.setVariable("eventTime", eventTime);
        ctx.setVariable("cancelledByName", cancelledByName);

        try {
            String html = templateEngine.process("email/event-cancelled", ctx);
            send(toEmail, "Event Cancelled: " + eventName, html);
        } catch (Exception e) {
            log.error("Failed to process/send event cancelled email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async("emailExecutor")
    public void sendProjectInvitation(String toEmail, String projectName, String invitedByName,
                                       String invitationUrl, boolean isNewUser) {
        Context ctx = new Context();
        ctx.setVariable("projectName", projectName);
        ctx.setVariable("invitedByName", invitedByName);
        ctx.setVariable("invitationUrl", invitationUrl);
        ctx.setVariable("isNewUser", isNewUser);
        ctx.setVariable("footerNote", "This is an important email regarding your project invitation on Rookwork. You cannot unsubscribe from this type of email.");

        try {
            String html = templateEngine.process("email/project-invitation", ctx);
            send(toEmail, invitedByName + " invited you to project: " + projectName, html);
        } catch (Exception e) {
            log.error("Failed to process/send project invitation email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async("emailExecutor")
    public void sendProjectInvitationResponse(String toEmail, String projectName, String respondedByName,
                                               boolean accepted, String projectUrl) {
        Context ctx = new Context();
        ctx.setVariable("projectName", projectName);
        ctx.setVariable("respondedByName", respondedByName);
        ctx.setVariable("accepted", accepted);
        ctx.setVariable("projectUrl", projectUrl);

        String statusStr = accepted ? "accepted" : "declined";

        try {
            String html = templateEngine.process("email/project-invitation-response", ctx);
            send(toEmail, respondedByName + " has " + statusStr + " the invitation to project " + projectName, html);
        } catch (Exception e) {
            log.error("Failed to process/send project invitation response email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    @Async("emailExecutor")
    public void sendOtpEmail(String toEmail, String otpCode) {
        Context ctx = new Context();
        ctx.setVariable("otpCode", otpCode);

        log.info("[OTP REGISTRATION] Sending OTP {} to email {}", otpCode, toEmail);

        try {
            String html = templateEngine.process("email/otp-email", ctx);
            send(toEmail, "Rookwork Account Verification Code", html);
        } catch (Exception e) {
            log.error("Failed to process/send OTP email to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    private void send(String toEmail, String subject, String htmlBody) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder().html(Content.builder().data(htmlBody).charset("UTF-8").build()).build())
                            .build())
                    .source(fromEmail)
                    .build();
            sesClient.sendEmail(request);
            log.info("Email successfully sent to {} with subject: {}", toEmail, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} (subject: {}): {}", toEmail, subject, e.getMessage());
        }
    }
}