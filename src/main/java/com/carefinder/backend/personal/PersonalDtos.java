package com.carefinder.backend.personal;

import com.carefinder.backend.hospital.HospitalDtos;

import java.time.Instant;

public final class PersonalDtos {

    private PersonalDtos() {
    }

    public record FavoriteResponse(HospitalDtos.HospitalResponse hospital, Instant savedAt) {
    }

    public record RecentResponse(
            HospitalDtos.HospitalResponse hospital,
            Instant lastViewedAt,
            int viewCount
    ) {
    }
}
