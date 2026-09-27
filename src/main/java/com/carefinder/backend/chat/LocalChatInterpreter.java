package com.carefinder.backend.chat;

import com.carefinder.backend.hospital.Hospital;
import com.carefinder.backend.hospital.HospitalRepository;
import com.carefinder.backend.insurance.InsuranceProvider;
import com.carefinder.backend.insurance.InsuranceProviderRepository;
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
public class LocalChatInterpreter {

  private static final Pattern RATING_PATTERN = Pattern.compile(
      "\\b(?:rating|rated|star|above|over|at least)\\s*(?:of\\s*)?([1-5](?:\\.\\d+)?)");

  private static final Pattern REVERSE_RATING_PATTERN = Pattern.compile(
      "\\b([1-5](?:\\.\\d+)?)\\s*\\+?\\s*(?:star|rating)");

  private static final Pattern BED_PATTERN = Pattern.compile("\\b(\\d{1,5})\\s*\\+?\\s*beds?\\b");

  private static final Pattern TOP_PATTERN = Pattern.compile("\\btop\\s+(\\d{1,2})\\b");

  private final HospitalRepository hospitalRepository;
  private final InsuranceProviderRepository insuranceRepository;

  public LocalChatInterpreter(
      HospitalRepository hospitalRepository,
      InsuranceProviderRepository insuranceRepository) {
    this.hospitalRepository = hospitalRepository;
    this.insuranceRepository = insuranceRepository;
  }

  @Transactional(readOnly = true)
  public ChatQueryPlan interpret(String userMessage) {
    String message = normalize(userMessage);

    List<Hospital> hospitals = hospitalRepository
        .findAll()
        .stream()
        .filter(Hospital::isActive)
        .toList();

    List<String> hospitalNames = hospitals
        .stream()
        .map(Hospital::getName)
        .toList();

    List<String> mentionedHospitals = detectAllValues(message, hospitalNames);

    String hospitalName = mentionedHospitals
        .stream()
        .findFirst()
        .orElse(null);

    String secondHospitalName = mentionedHospitals.size() > 1
        ? mentionedHospitals.get(1)
        : null;

    String city = detectValue(
        message,
        hospitals.stream()
            .map(Hospital::getCity)
            .distinct()
            .toList());

    String state = detectValue(
        message,
        hospitals.stream()
            .map(Hospital::getState)
            .distinct()
            .toList());

    Set<String> specialtyValues = new LinkedHashSet<>();

    hospitals.forEach(
        hospital -> specialtyValues.addAll(
            hospital.getSpecialties()));

    String specialty = detectValue(
        message,
        specialtyValues.stream().toList());

    if (specialty == null) {
      specialty = detectSpecialtySynonym(
          message,
          specialtyValues);
    }

    String insurer = detectValue(
        message,
        insuranceRepository
            .findAllByActiveTrueOrderByNameAsc()
            .stream()
            .map(InsuranceProvider::getName)
            .toList());

    String accreditation = detectValue(
        message,
        hospitals.stream()
            .map(Hospital::getAccreditation)
            .distinct()
            .toList());

    String hospitalType = detectValue(
        message,
        hospitals.stream()
            .map(Hospital::getType)
            .distinct()
            .toList());

    boolean emergencyOnly = containsAny(
        message,
        "emergency",
        "urgent care",
        "trauma",
        "ambulance",
        "accident hospital");

    boolean open24x7Only = containsAny(
        message,
        "24x7",
        "24/7",
        "open now",
        "always open",
        "raat bhar",
        "night hospital");

    boolean verifiedOnly = containsAny(
        message,
        "verified",
        "trusted hospital");

    Double minimumRating = detectRating(message);
    Integer minimumBeds = detectBeds(message);
    Integer limit = detectLimit(message);

    ChatQueryPlan.SortBy sortBy = detectSort(message);

    ChatQueryPlan.Intent intent = detectIntent(
        message,
        hospitalName,
        secondHospitalName,
        city,
        state,
        specialty,
        insurer,
        emergencyOnly);

    return new ChatQueryPlan(
        intent,
        city,
        state,
        hospitalName,
        secondHospitalName,
        specialty,
        insurer,
        accreditation,
        hospitalType,
        emergencyOnly,
        open24x7Only,
        verifiedOnly,
        minimumRating,
        minimumBeds,
        sortBy,
        limit);
  }

  private ChatQueryPlan.Intent detectIntent(
      String message,
      String hospitalName,
      String secondHospitalName,
      String city,
      String state,
      String specialty,
      String insurer,
      boolean emergencyOnly) {
    if (containsAny(
        message,
        "list cities",
        "which cities",
        "available cities",
        "cities available")) {
      return ChatQueryPlan.Intent.LIST_CITIES;
    }

    if (containsAny(
        message,
        "list insurance",
        "which insurance",
        "insurance providers",
        "available insurance",
        "insurers")) {
      return ChatQueryPlan.Intent.LIST_INSURERS;
    }

    if (containsAny(
        message,
        "list specialties",
        "which specialties",
        "available specialties",
        "departments available")) {
      return ChatQueryPlan.Intent.LIST_SPECIALTIES;
    }

    if (containsAny(
        message,
        "compare",
        "comparison",
        "difference between",
        "better between",
        "versus",
        " vs ") && hospitalName != null
        && secondHospitalName != null) {

      return ChatQueryPlan.Intent.COMPARE;
    }

    if (containsAny(
        message,
        "how many",
        "total hospital",
        "number of hospital",
        "count hospital",
        "kitne hospital",
        "kitni hospital")) {
      return ChatQueryPlan.Intent.COUNT;
    }

    if (containsAny(
        message,
        "heart attack",
        "heavy bleeding",
        "not breathing",
        "unconscious",
        "medical emergency",
        "need ambulance")) {
      return ChatQueryPlan.Intent.EMERGENCY;
    }

    if (hospitalName != null
        && containsAny(
            message,
            "detail",
            "details",
            "about",
            "address",
            "phone",
            "website",
            "beds",
            "rating",
            "tell me")) {

      return ChatQueryPlan.Intent.DETAILS;
    }

    if (isShortGreeting(message)) {
      return ChatQueryPlan.Intent.GREETING;
    }

    boolean hasDirectoryContext = hospitalName != null
        || city != null
        || state != null
        || specialty != null
        || insurer != null
        || emergencyOnly
        || containsAny(
            message,
            "hospital",
            "doctor",
            "treatment",
            "clinic",
            "carefinder");

    if (hasDirectoryContext) {
      return ChatQueryPlan.Intent.SEARCH;
    }

    if (containsAny(
        message,
        "help",
        "what can you do",
        "how to use")) {
      return ChatQueryPlan.Intent.HELP;
    }

    return ChatQueryPlan.Intent.OUT_OF_SCOPE;
  }

  private String detectSpecialtySynonym(
      String message,
      Set<String> availableSpecialties) {
    String requested = null;

    if (containsAny(message, "heart", "cardiac")) {
      requested = "cardio";
    } else if (containsAny(message, "kidney", "renal")) {
      requested = "nephro";
    } else if (containsAny(message, "brain", "neuro")) {
      requested = "neuro";
    } else if (containsAny(
        message,
        "cancer",
        "tumor",
        "oncology")) {
      requested = "onco";
    } else if (containsAny(
        message,
        "child",
        "children",
        "pediatric")) {
      requested = "pediatric";
    } else if (containsAny(
        message,
        "bone",
        "joint",
        "orthopedic")) {
      requested = "ortho";
    } else if (containsAny(message, "skin", "dermatology")) {
      requested = "derma";
    } else if (containsAny(message, "eye", "ophthalmology")) {
      requested = "ophthal";
    } else if (containsAny(
        message,
        "women",
        "pregnancy",
        "gynaecology",
        "gynecology")) {
      requested = "gyn";
    }

    if (requested == null) {
      return null;
    }

    String finalRequested = requested;

    return availableSpecialties
        .stream()
        .filter(
            value -> normalize(value)
                .contains(finalRequested))
        .findFirst()
        .orElse(null);
  }

  private ChatQueryPlan.SortBy detectSort(String message) {
    if (containsAny(
        message,
        "most beds",
        "largest hospital",
        "highest capacity")) {
      return ChatQueryPlan.SortBy.BEDS;
    }

    if (containsAny(
        message,
        "most reviewed",
        "maximum reviews")) {
      return ChatQueryPlan.SortBy.REVIEWS;
    }

    if (containsAny(
        message,
        "alphabetical",
        "name order",
        "a to z")) {
      return ChatQueryPlan.SortBy.NAME;
    }

    if (containsAny(
        message,
        "best",
        "top rated",
        "highest rated",
        "rating")) {
      return ChatQueryPlan.SortBy.RATING;
    }

    return ChatQueryPlan.SortBy.RELEVANCE;
  }

  private Double detectRating(String message) {
    Matcher matcher = RATING_PATTERN.matcher(message);

    if (matcher.find()) {
      return Double.parseDouble(matcher.group(1));
    }

    matcher = REVERSE_RATING_PATTERN.matcher(message);

    if (matcher.find()) {
      return Double.parseDouble(matcher.group(1));
    }

    return null;
  }

  private Integer detectBeds(String message) {
    Matcher matcher = BED_PATTERN.matcher(message);

    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }

    return null;
  }

  private Integer detectLimit(String message) {
    Matcher matcher = TOP_PATTERN.matcher(message);

    if (matcher.find()) {
      return Math.min(
          Integer.parseInt(matcher.group(1)),
          8);
    }

    return 5;
  }

  private String detectValue(
      String message,
      List<String> values) {
    return detectAllValues(message, values)
        .stream()
        .findFirst()
        .orElse(null);
  }

  private List<String> detectAllValues(
      String message,
      List<String> values) {
    return values
        .stream()
        .filter(this::hasText)
        .distinct()
        .filter(
            value -> message.contains(
                normalize(value)))
        .sorted(
            Comparator.comparingInt(
                String::length).reversed())
        .toList();
  }

  private boolean isShortGreeting(String message) {
    return message.equals("hi")
        || message.equals("hello")
        || message.equals("hey")
        || message.equals("namaste")
        || message.equals("good morning")
        || message.equals("good evening");
  }

  private boolean containsAny(
      String value,
      String... terms) {
    for (String term : terms) {
      if (value.contains(term)) {
        return true;
      }
    }

    return false;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String normalize(String value) {
    return value
        .toLowerCase(Locale.ROOT)
        .replaceAll(
            "[^\\p{L}\\p{N}+/. ]",
            " ")
        .replaceAll("\\s+", " ")
        .trim();
  }
}