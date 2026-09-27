package com.carefinder.backend.chat;

public record ChatQueryPlan(
    Intent intent,
    String city,
    String state,
    String hospitalName,
    String secondHospitalName,
    String specialty,
    String insuranceProvider,
    String accreditation,
    String hospitalType,
    boolean emergencyOnly,
    boolean open24x7Only,
    boolean verifiedOnly,
    Double minimumRating,
    Integer minimumBeds,
    SortBy sortBy,
    Integer limit) {

  public ChatQueryPlan {
    intent = intent == null ? Intent.SEARCH : intent;

    city = clean(city);
    state = clean(state);
    hospitalName = clean(hospitalName);
    secondHospitalName = clean(secondHospitalName);
    specialty = clean(specialty);
    insuranceProvider = clean(insuranceProvider);
    accreditation = clean(accreditation);
    hospitalType = clean(hospitalType);

    if (minimumRating != null) {
      minimumRating = Math.max(0.0, Math.min(minimumRating, 5.0));
    }

    if (minimumBeds != null && minimumBeds < 0) {
      minimumBeds = 0;
    }

    sortBy = sortBy == null ? SortBy.RELEVANCE : sortBy;
    limit = limit == null ? 5 : Math.max(1, Math.min(limit, 8));
  }

  public static ChatQueryPlan help() {
    return new ChatQueryPlan(
        Intent.HELP,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        false,
        false,
        false,
        null,
        null,
        SortBy.RELEVANCE,
        5);
  }

  private static String clean(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
  }

  public enum Intent {
    COUNT,
    SEARCH,
    DETAILS,
    COMPARE,
    LIST_CITIES,
    LIST_INSURERS,
    LIST_SPECIALTIES,
    GREETING,
    HELP,
    EMERGENCY,
    OUT_OF_SCOPE
  }

  public enum SortBy {
    RELEVANCE,
    RATING,
    REVIEWS,
    NAME,
    BEDS
  }
}