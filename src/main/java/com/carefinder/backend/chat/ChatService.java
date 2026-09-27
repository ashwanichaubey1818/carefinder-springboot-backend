package com.carefinder.backend.chat;

import com.carefinder.backend.hospital.Hospital;
import com.carefinder.backend.hospital.HospitalMapper;
import com.carefinder.backend.insurance.InsuranceProvider;
import com.carefinder.backend.security.CurrentUserService;
import com.carefinder.backend.user.UserAccount;
import com.carefinder.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    private final GeminiChatInterpreter geminiInterpreter;
    private final LocalChatInterpreter localInterpreter;
    private final HospitalChatQueryService queryService;
    private final HospitalMapper hospitalMapper;
    private final ChatMessageRepository chatRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public ChatService(
            GeminiChatInterpreter geminiInterpreter,
            LocalChatInterpreter localInterpreter,
            HospitalChatQueryService queryService,
            HospitalMapper hospitalMapper,
            ChatMessageRepository chatRepository,
            CurrentUserService currentUserService,
            UserRepository userRepository) {
        this.geminiInterpreter = geminiInterpreter;
        this.localInterpreter = localInterpreter;
        this.queryService = queryService;
        this.hospitalMapper = hospitalMapper;
        this.chatRepository = chatRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ChatDtos.ChatResponse reply(
            ChatDtos.ChatRequest request) {
        String message = request.message().trim();
        String language = normalizeLanguage(request.language());

        Optional<ChatQueryPlan> aiPlan = geminiInterpreter.interpret(
                message,
                queryService.databaseVocabulary());

        ChatQueryPlan plan = aiPlan.orElseGet(
                () -> localInterpreter.interpret(message));

        ChatOutcome outcome = createOutcome(
                plan,
                language);

        ChatDtos.ChatResponse response = new ChatDtos.ChatResponse(
                outcome.answer(),
                language,
                responseIntent(plan),
                outcome.hospitals()
                        .stream()
                        .map(
                                hospital -> hospitalMapper.toResponse(
                                        hospital,
                                        null))
                        .toList(),
                outcome.emergencyDisclaimer());

        saveForAuthenticatedUser(
                message,
                outcome.answer(),
                language);

        return response;
    }

    @Transactional(readOnly = true)
    public List<ChatDtos.ChatHistoryResponse> history() {
        UserAccount user = currentUserService.requireCurrentUser();

        return chatRepository
                .findTop50ByUserIdOrderByCreatedAtDesc(
                        user.getId())
                .stream()
                .map(
                        message -> new ChatDtos.ChatHistoryResponse(
                                message.getId(),
                                message.getUserMessage(),
                                message.getAssistantMessage(),
                                message.getLanguage(),
                                message.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void clearHistory() {
        chatRepository.deleteByUserId(
                currentUserService
                        .requireCurrentUser()
                        .getId());
    }

    private ChatOutcome createOutcome(
            ChatQueryPlan plan,
            String language) {
        return switch (plan.intent()) {
            case COUNT -> countOutcome(plan, language);

            case SEARCH ->
                searchOutcome(plan, language);

            case DETAILS ->
                detailsOutcome(plan, language);

            case COMPARE ->
                compareOutcome(plan, language);

            case LIST_CITIES ->
                listOutcome(
                        language,
                        "cities",
                        "cities",
                        queryService.cities());

            case LIST_INSURERS ->
                listOutcome(
                        language,
                        "insurance providers",
                        "insurance providers",
                        queryService.insurers());

            case LIST_SPECIALTIES ->
                listOutcome(
                        language,
                        "specialties",
                        "specialties",
                        queryService.specialties());

            case GREETING ->
                greetingOutcome(language);

            case HELP ->
                helpOutcome(language);

            case EMERGENCY ->
                emergencyOutcome(plan, language);

            case OUT_OF_SCOPE ->
                outOfScopeOutcome(language);
        };
    }

    private ChatOutcome countOutcome(
            ChatQueryPlan plan,
            String language) {
        HospitalChatQueryService.SearchResult result = queryService.search(plan);

        String scope = describeScope(plan);

        String answer;

        if (language.equals("hi")) {
            if (result.total() == 0) {
                answer = "CareFinder database me "
                        + scope
                        + " ke liye koi active hospital nahi mila.";
            } else {
                answer = "CareFinder database me "
                        + scope
                        + " ke liye total "
                        + result.total()
                        + " active hospital hain.";
            }
        } else {
            if (result.total() == 0) {
                answer = "I could not find any active hospital for "
                        + scope
                        + " in the CareFinder database.";
            } else {
                answer = "There are "
                        + result.total()
                        + " active hospitals for "
                        + scope
                        + " in the CareFinder database.";
            }
        }

        return new ChatOutcome(
                answer,
                List.of(),
                plan.emergencyOnly());
    }

    private ChatOutcome searchOutcome(
            ChatQueryPlan plan,
            String language) {
        HospitalChatQueryService.SearchResult result = queryService.search(plan);

        String scope = describeScope(plan);

        if (result.total() == 0) {
            String answer = language.equals("hi")
                    ? "Mujhe CareFinder database me "
                            + scope
                            + " ke saath koi hospital nahi mila. "
                            + "City, specialty ya insurance provider badal kar dekhiye."
                    : "I could not find a hospital matching "
                            + scope
                            + " in the CareFinder database. "
                            + "Try another city, specialty, or insurance provider.";

            return new ChatOutcome(
                    answer,
                    List.of(),
                    plan.emergencyOnly());
        }

        int displayed = result.hospitals().size();

        String answer;

        if (language.equals("hi")) {
            answer = "Mujhe "
                    + result.total()
                    + " hospital mile hain "
                    + scope
                    + " ke liye. Main best "
                    + displayed
                    + " result dikha raha hoon. "
                    + "Treatment aur cashless insurance ko "
                    + "hospital se directly confirm karein.";
        } else {
            answer = "I found "
                    + result.total()
                    + " hospitals for "
                    + scope
                    + ". I am showing the best "
                    + displayed
                    + " results. Confirm treatment availability "
                    + "and cashless insurance directly with the hospital.";
        }

        return new ChatOutcome(
                answer,
                result.hospitals(),
                plan.emergencyOnly());
    }

    private ChatOutcome detailsOutcome(
            ChatQueryPlan plan,
            String language) {
        HospitalChatQueryService.SearchResult result = queryService.search(plan);

        if (result.hospitals().isEmpty()) {
            String answer = language.equals("hi")
                    ? "Mujhe is naam ka hospital CareFinder database me nahi mila. Sahi hospital naam dobara likhiye."
                    : "I could not find that hospital in the CareFinder database. Check the hospital name and try again.";

            return new ChatOutcome(
                    answer,
                    List.of(),
                    false);
        }

        Hospital hospital = result.hospitals().getFirst();

        String specialties = hospital
                .getSpecialties()
                .isEmpty()
                        ? "not listed"
                        : String.join(
                                ", ",
                                hospital.getSpecialties());

        String insurers = hospital
                .getInsuranceProviders()
                .stream()
                .map(InsuranceProvider::getName)
                .sorted()
                .toList()
                .stream()
                .reduce(
                        (left, right) -> left + ", " + right)
                .orElse("not listed");

        String answer;

        if (language.equals("hi")) {
            answer = """
                    %s, %s, %s me hai.
                    Rating: %.1f/5 (%d reviews)
                    Type: %s
                    Beds: %d
                    Emergency: %s
                    24x7 open: %s
                    Accreditation: %s
                    Specialties: %s
                    Insurance: %s
                    Phone: %s

                    Appointment, treatment aur cashless eligibility hospital se directly confirm karein.
                    """.formatted(
                    hospital.getName(),
                    hospital.getCity(),
                    hospital.getState(),
                    hospital.getRating(),
                    hospital.getReviews(),
                    hospital.getType(),
                    hospital.getBeds(),
                    yesNoHindi(hospital.isEmergency()),
                    yesNoHindi(hospital.isOpen24x7()),
                    valueOrNotListed(
                            hospital.getAccreditation()),
                    specialties,
                    insurers,
                    valueOrNotListed(
                            hospital.getPhone()));
        } else {
            answer = """
                    %s is located in %s, %s.
                    Rating: %.1f/5 (%d reviews)
                    Type: %s
                    Beds: %d
                    Emergency: %s
                    Open 24x7: %s
                    Accreditation: %s
                    Specialties: %s
                    Insurance: %s
                    Phone: %s

                    Confirm appointments, treatment availability, and cashless eligibility directly with the hospital.
                    """.formatted(
                    hospital.getName(),
                    hospital.getCity(),
                    hospital.getState(),
                    hospital.getRating(),
                    hospital.getReviews(),
                    hospital.getType(),
                    hospital.getBeds(),
                    yesNoEnglish(
                            hospital.isEmergency()),
                    yesNoEnglish(
                            hospital.isOpen24x7()),
                    valueOrNotListed(
                            hospital.getAccreditation()),
                    specialties,
                    insurers,
                    valueOrNotListed(
                            hospital.getPhone()));
        }

        return new ChatOutcome(
                answer.trim(),
                List.of(hospital),
                hospital.isEmergency());
    }

    private ChatOutcome compareOutcome(
            ChatQueryPlan plan,
            String language) {
        HospitalChatQueryService.SearchResult result = queryService.search(plan);

        if (result.hospitals().size() < 2) {
            String answer = language.equals("hi")
                    ? "Comparison ke liye CareFinder database ke do hospital ke exact naam likhiye."
                    : "Please provide the exact names of two hospitals from the CareFinder database.";

            return new ChatOutcome(
                    answer,
                    result.hospitals(),
                    false);
        }

        Hospital first = result.hospitals().get(0);
        Hospital second = result.hospitals().get(1);

        String answer;

        if (language.equals("hi")) {
            answer = """
                    %s aur %s ka database comparison:

                    %s:
                    Rating %.1f/5, %d reviews, %d beds, emergency %s, 24x7 %s.

                    %s:
                    Rating %.1f/5, %d reviews, %d beds, emergency %s, 24x7 %s.

                    Final choice se pehle required specialty, doctor availability aur insurance hospital se confirm karein.
                    """
                    .formatted(
                            first.getName(),
                            second.getName(),
                            first.getName(),
                            first.getRating(),
                            first.getReviews(),
                            first.getBeds(),
                            yesNoHindi(first.isEmergency()),
                            yesNoHindi(first.isOpen24x7()),
                            second.getName(),
                            second.getRating(),
                            second.getReviews(),
                            second.getBeds(),
                            yesNoHindi(second.isEmergency()),
                            yesNoHindi(second.isOpen24x7()));
        } else {
            answer = """
                    Database comparison of %s and %s:

                    %s:
                    Rating %.1f/5, %d reviews, %d beds, emergency %s, open 24x7 %s.

                    %s:
                    Rating %.1f/5, %d reviews, %d beds, emergency %s, open 24x7 %s.

                    Before choosing, confirm the required specialty, doctor availability, and insurance directly with each hospital.
                    """
                    .formatted(
                            first.getName(),
                            second.getName(),
                            first.getName(),
                            first.getRating(),
                            first.getReviews(),
                            first.getBeds(),
                            yesNoEnglish(first.isEmergency()),
                            yesNoEnglish(first.isOpen24x7()),
                            second.getName(),
                            second.getRating(),
                            second.getReviews(),
                            second.getBeds(),
                            yesNoEnglish(second.isEmergency()),
                            yesNoEnglish(second.isOpen24x7()));
        }

        return new ChatOutcome(
                answer.trim(),
                List.of(first, second),
                first.isEmergency()
                        || second.isEmergency());
    }

    private ChatOutcome emergencyOutcome(
            ChatQueryPlan originalPlan,
            String language) {
        ChatQueryPlan emergencyPlan = forceEmergencyFilter(originalPlan);

        HospitalChatQueryService.SearchResult result = queryService.search(emergencyPlan);

        String answer;

        if (language.equals("hi")) {
            answer = "Agar abhi medical emergency hai to turant 112 par call karein "
                    + "ya nearest emergency department jaiye. "
                    + "CareFinder database me matching emergency hospitals: "
                    + result.total()
                    + ".";
        } else {
            answer = "If this is an immediate medical emergency, call 112 now "
                    + "or go to the nearest emergency department. "
                    + "Matching emergency hospitals in the CareFinder database: "
                    + result.total()
                    + ".";
        }

        return new ChatOutcome(
                answer,
                result.hospitals(),
                true);
    }

    private ChatOutcome listOutcome(
            String language,
            String englishLabel,
            String hindiLabel,
            List<String> values) {
        String answer;

        if (values.isEmpty()) {
            answer = language.equals("hi")
                    ? "CareFinder database me abhi "
                            + hindiLabel
                            + " available nahi hain."
                    : "No "
                            + englishLabel
                            + " are currently available in the CareFinder database.";
        } else if (language.equals("hi")) {
            answer = "CareFinder database me "
                    + values.size()
                    + " "
                    + hindiLabel
                    + " hain: "
                    + String.join(", ", values)
                    + ".";
        } else {
            answer = "The CareFinder database contains "
                    + values.size()
                    + " "
                    + englishLabel
                    + ": "
                    + String.join(", ", values)
                    + ".";
        }

        return new ChatOutcome(
                answer,
                List.of(),
                false);
    }

    private ChatOutcome greetingOutcome(String language) {
        String answer = language.equals("hi")
                ? "Namaste! Main CareFinder assistant hoon. Aap city, specialty, insurance, emergency, rating ya kisi hospital ki details pooch sakte hain."
                : "Hello! I am the CareFinder assistant. Ask me about hospitals by city, specialty, insurance, emergency availability, rating, or hospital name.";

        return new ChatOutcome(
                answer,
                List.of(),
                false);
    }

    private ChatOutcome helpOutcome(String language) {
        String answer = language.equals("hi")
                ? """
                        Aap mujhse aise questions pooch sakte hain:

                        - Delhi me kitne hospitals hain?
                        - Mumbai me best cardiac hospital dikhao.
                        - Star Health accept karne wale 24x7 hospitals kaun se hain?
                        - AIIMS Delhi ki details batao.
                        - Do hospitals ko compare karo.
                        """
                : """
                        You can ask questions such as:

                        - How many hospitals are in Delhi?
                        - Show the best cardiac hospitals in Mumbai.
                        - Which 24x7 hospitals accept Star Health?
                        - Give me details about AIIMS Delhi.
                        - Compare two hospitals.
                        """;

        return new ChatOutcome(
                answer.trim(),
                List.of(),
                false);
    }

    private ChatOutcome outOfScopeOutcome(String language) {
        String answer = language.equals("hi")
                ? "Main CareFinder database ke hospital, specialty, insurance aur emergency availability se jude questions ka answer de sakta hoon. Main medical diagnosis ya treatment prescribe nahi karta."
                : "I can answer questions about hospitals, specialties, insurance, and emergency availability from the CareFinder database. I cannot provide medical diagnoses or prescribe treatment.";

        return new ChatOutcome(
                answer,
                List.of(),
                false);
    }

    private ChatQueryPlan forceEmergencyFilter(
            ChatQueryPlan plan) {
        return new ChatQueryPlan(
                plan.intent(),
                plan.city(),
                plan.state(),
                plan.hospitalName(),
                plan.secondHospitalName(),
                plan.specialty(),
                plan.insuranceProvider(),
                plan.accreditation(),
                plan.hospitalType(),
                true,
                plan.open24x7Only(),
                plan.verifiedOnly(),
                plan.minimumRating(),
                plan.minimumBeds(),
                plan.sortBy(),
                plan.limit());
    }

    private String describeScope(ChatQueryPlan plan) {
        List<String> parts = new ArrayList<>();

        addPart(parts, plan.city());
        addPart(parts, plan.state());
        addPart(parts, plan.specialty());
        addPart(parts, plan.insuranceProvider());
        addPart(parts, plan.accreditation());
        addPart(parts, plan.hospitalType());

        if (plan.emergencyOnly()) {
            parts.add("emergency service");
        }

        if (plan.open24x7Only()) {
            parts.add("24x7 open");
        }

        if (plan.minimumRating() != null
                && plan.minimumRating() > 0) {
            parts.add(
                    plan.minimumRating()
                            + "+ rating");
        }

        if (plan.minimumBeds() != null
                && plan.minimumBeds() > 0) {
            parts.add(
                    plan.minimumBeds()
                            + "+ beds");
        }

        if (parts.isEmpty()) {
            return "all locations";
        }

        return String.join(", ", parts);
    }

    private void addPart(
            List<String> parts,
            String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }

    private String responseIntent(ChatQueryPlan plan) {
        if (plan.intent() != ChatQueryPlan.Intent.SEARCH) {
            return plan.intent().name();
        }

        if (plan.emergencyOnly()) {
            return "EMERGENCY_SEARCH";
        }

        if (plan.insuranceProvider() != null
                && !plan.insuranceProvider().isBlank()) {
            return "INSURANCE_SEARCH";
        }

        if ((plan.city() != null && !plan.city().isBlank())
                || (plan.state() != null
                        && !plan.state().isBlank())) {
            return "LOCATION_SEARCH";
        }

        if (plan.open24x7Only()) {
            return "OPEN_24X7_SEARCH";
        }

        return "HOSPITAL_HELP";
    }

    private String normalizeLanguage(String language) {
        return "hi".equalsIgnoreCase(language)
                ? "hi"
                : "en";
    }

    private String yesNoHindi(boolean value) {
        return value ? "haan" : "nahi";
    }

    private String yesNoEnglish(boolean value) {
        return value ? "yes" : "no";
    }

    private String valueOrNotListed(String value) {
        return value == null || value.isBlank()
                ? "not listed"
                : value;
    }

    private void saveForAuthenticatedUser(
            String userMessage,
            String answer,
            String language) {
        currentUserService
                .principal()
                .ifPresent(
                        principal -> {
                            UserAccount user = userRepository
                                    .getReferenceById(
                                            principal.userId());

                            chatRepository.save(
                                    new ChatMessage(
                                            user,
                                            userMessage,
                                            answer,
                                            language));
                        });
    }

    private record ChatOutcome(
            String answer,
            List<Hospital> hospitals,
            boolean emergencyDisclaimer) {
    }
}