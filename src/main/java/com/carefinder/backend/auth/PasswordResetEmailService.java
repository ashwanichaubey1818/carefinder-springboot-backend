package com.carefinder.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class PasswordResetEmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetEmailService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;

    public PasswordResetEmailService(
            RestClient.Builder restClientBuilder,
            @Value("${app.brevo.api-key:}") String apiKey,
            @Value("${app.brevo.sender-email:}") String senderEmail,
            @Value("${app.brevo.sender-name:CareFinder}") String senderName) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.brevo.com/v3")
                .build();

        this.apiKey = apiKey;
        this.senderEmail = senderEmail;
        this.senderName = senderName;
    }

    public void sendPasswordResetEmail(
            String recipientEmail,
            String resetLink) {
        if (apiKey == null || apiKey.isBlank()) {
            LOGGER.warn(
                    "Password reset email was not sent because BREVO_API_KEY is missing.");
            return;
        }

        if (senderEmail == null || senderEmail.isBlank()) {
            LOGGER.warn(
                    "Password reset email was not sent because BREVO_SENDER_EMAIL is missing.");
            return;
        }

        Map<String, Object> requestBody = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", senderEmail),
                "to", List.of(
                        Map.of("email", recipientEmail)),
                "subject", "Reset your CareFinder password",
                "htmlContent", createEmailContent(resetLink));

        try {
            restClient.post()
                    .uri("/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .toBodilessEntity();

            LOGGER.info(
                    "Password reset email request was accepted by Brevo.");

        } catch (RestClientResponseException exception) {
            LOGGER.error(
                    "Brevo rejected password reset email. HTTP status: {}",
                    exception.getStatusCode());

        } catch (RestClientException exception) {
            LOGGER.error(
                    "Unable to send password reset email through Brevo.",
                    exception);
        }
    }

    private String createEmailContent(String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; color: #163047;">
                    <h2>Reset your CareFinder password</h2>

                    <p>Hello,</p>

                    <p>
                        We received a request to reset your
                        CareFinder account password.
                    </p>

                    <p>Click the button below to create a new password:</p>

                    <p>
                        <a href="%s"
                           style="
                               display: inline-block;
                               padding: 12px 20px;
                               background-color: #079c94;
                               color: white;
                               text-decoration: none;
                               border-radius: 6px;
                           ">
                            Reset password
                        </a>
                    </p>

                    <p>
                        This link will expire shortly for security reasons.
                    </p>

                    <p>
                        If you did not request this reset,
                        you can safely ignore this email.
                    </p>

                    <p>CareFinder Team</p>
                </body>
                </html>
                """.formatted(resetLink);
    }
}