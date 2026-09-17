package com.carefinder.backend.chat;

import com.carefinder.backend.hospital.Hospital;
import com.carefinder.backend.hospital.HospitalMapper;
import com.carefinder.backend.hospital.HospitalRepository;
import com.carefinder.backend.insurance.InsuranceProviderRepository;
import com.carefinder.backend.security.CurrentUserService;
import com.carefinder.backend.user.UserAccount;
import com.carefinder.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Pattern LOCATION_PATTERN = Pattern.compile("\\b(?:in|near|at)\\s+([a-z ]{2,40})", Pattern.CASE_INSENSITIVE);

    private final HospitalRepository hospitalRepository;
    private final InsuranceProviderRepository insuranceRepository;
    private final HospitalMapper hospitalMapper;
    private final ChatMessageRepository chatRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public ChatService(
            HospitalRepository hospitalRepository,
            InsuranceProviderRepository insuranceRepository,
            HospitalMapper hospitalMapper,
            ChatMessageRepository chatRepository,
            CurrentUserService currentUserService,
            UserRepository userRepository
    ) {
        this.hospitalRepository = hospitalRepository;
        this.insuranceRepository = insuranceRepository;
        this.hospitalMapper = hospitalMapper;
        this.chatRepository = chatRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChatDtos.ChatResponse reply(ChatDtos.ChatRequest request) {
        String language = "hi".equalsIgnoreCase(request.language()) ? "hi" : "en";
        String normalized = request.message().trim().toLowerCase(Locale.ROOT);
        boolean emergencyIntent = containsAny(normalized, "emergency", "urgent", "accident", "112", "आपात", "इमरजेंसी");
        boolean openIntent = containsAny(normalized, "24x7", "24/7", "open now", "रात", "खुला");

        List<Hospital> all = hospitalRepository.findAll().stream().filter(Hospital::isActive).toList();
        String location = detectLocation(normalized, all);
        String insurer = insuranceRepository.findAllByActiveTrueOrderByNameAsc().stream()
                .map(provider -> provider.getName())
                .filter(name -> normalized.contains(name.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);

        List<Hospital> matches = all.stream()
                .filter(hospital -> location == null
                        || hospital.getCity().equalsIgnoreCase(location)
                        || hospital.getState().equalsIgnoreCase(location))
                .filter(hospital -> !emergencyIntent || hospital.isEmergency())
                .filter(hospital -> !openIntent || hospital.isOpen24x7())
                .filter(hospital -> insurer == null || hospital.getInsuranceProviders().stream()
                        .anyMatch(provider -> provider.getName().equalsIgnoreCase(insurer)))
                .sorted(Comparator.comparingDouble(Hospital::getRating).reversed()
                        .thenComparing(Hospital::getReviews, Comparator.reverseOrder()))
                .limit(5)
                .toList();

        String intent = emergencyIntent ? "EMERGENCY_SEARCH"
                : insurer != null ? "INSURANCE_SEARCH"
                : location != null ? "LOCATION_SEARCH"
                : openIntent ? "OPEN_24X7_SEARCH"
                : "HOSPITAL_HELP";
        String answer = answer(language, matches, location, insurer, emergencyIntent);
        ChatDtos.ChatResponse response = new ChatDtos.ChatResponse(
                answer,
                language,
                intent,
                matches.stream().map(hospital -> hospitalMapper.toResponse(hospital, null)).toList(),
                emergencyIntent
        );
        saveForAuthenticatedUser(request.message().trim(), answer, language);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatDtos.ChatHistoryResponse> history() {
        UserAccount user = currentUserService.requireCurrentUser();
        return chatRepository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(message -> new ChatDtos.ChatHistoryResponse(
                        message.getId(),
                        message.getUserMessage(),
                        message.getAssistantMessage(),
                        message.getLanguage(),
                        message.getCreatedAt()
                ))
                .toList();
    }

    @Transactional
    public void clearHistory() {
        chatRepository.deleteByUserId(currentUserService.requireCurrentUser().getId());
    }

    private String detectLocation(String message, List<Hospital> hospitals) {
        Set<String> locations = new LinkedHashSet<>();
        hospitals.forEach(hospital -> {
            locations.add(hospital.getCity());
            locations.add(hospital.getState());
        });
        return locations.stream()
                .filter(value -> message.contains(value.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElseGet(() -> {
                    Matcher matcher = LOCATION_PATTERN.matcher(message);
                    return matcher.find() ? matcher.group(1).trim() : null;
                });
    }

    private String answer(
            String language,
            List<Hospital> matches,
            String location,
            String insurer,
            boolean emergency
    ) {
        if (matches.isEmpty()) {
            return language.equals("hi")
                    ? "Mujhe in filters ke saath hospital nahi mila. City ya insurance provider ka naam badal kar dekhiye. Emergency mein 112 par call karein."
                    : "I could not find a hospital with those filters. Try another city or insurer. For an emergency, call 112.";
        }
        String scope = location != null ? location : insurer != null ? insurer : "your search";
        if (language.equals("hi")) {
            return matches.size() + " hospital mile hain (" + scope + "). Pehle cards compare karein aur treatment se pehle hospital aur insurer se details confirm karein."
                    + (emergency ? " Emergency mein turant 112 par call karein." : "");
        }
        return "I found " + matches.size() + " hospitals for " + scope
                + ". Compare the cards and confirm services and insurance directly before treatment."
                + (emergency ? " If this is an emergency, call 112 now." : "");
    }

    private void saveForAuthenticatedUser(String userMessage, String answer, String language) {
        currentUserService.principal().ifPresent(principal -> {
            UserAccount user = userRepository.getReferenceById(principal.userId());
            chatRepository.save(new ChatMessage(user, userMessage, answer, language));
        });
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
