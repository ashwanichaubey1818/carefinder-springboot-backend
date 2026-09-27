package com.carefinder.backend.chat;

import com.carefinder.backend.hospital.HospitalDtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class ChatDtos {

        private ChatDtos() {
        }

        public record ChatRequest(
                        @NotBlank @Size(max = 1000) String message,
                        @Pattern(regexp = "^(en|hi)$", message = "must be en or hi") String language) {
        }

        public record ChatResponse(
                        String answer,
                        String language,
                        String intent,
                        List<HospitalDtos.HospitalResponse> hospitals,
                        boolean emergencyDisclaimer) {
        }

        public record ChatHistoryResponse(
                        Long id,
                        String userMessage,
                        String assistantMessage,
                        String language,
                        Instant createdAt) {
        }
}
