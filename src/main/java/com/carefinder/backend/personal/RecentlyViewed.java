package com.carefinder.backend.personal;

import com.carefinder.backend.hospital.Hospital;
import com.carefinder.backend.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "recently_viewed",
        uniqueConstraints = @UniqueConstraint(name = "uk_recent_user_hospital", columnNames = {"user_id", "hospital_id"})
)
public class RecentlyViewed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(name = "last_viewed_at", nullable = false)
    private Instant lastViewedAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    protected RecentlyViewed() {
    }

    public RecentlyViewed(UserAccount user, Hospital hospital) {
        this.user = user;
        this.hospital = hospital;
        touch();
    }

    public void touch() {
        this.lastViewedAt = Instant.now();
        this.viewCount++;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public Instant getLastViewedAt() {
        return lastViewedAt;
    }

    public int getViewCount() {
        return viewCount;
    }
}
