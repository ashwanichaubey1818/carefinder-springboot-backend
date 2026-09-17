package com.carefinder.backend.admin;

import com.carefinder.backend.audit.AuditLogRepository;
import com.carefinder.backend.chat.ChatMessageRepository;
import com.carefinder.backend.hospital.HospitalRepository;
import com.carefinder.backend.insurance.InsuranceProviderRepository;
import com.carefinder.backend.personal.FavoriteRepository;
import com.carefinder.backend.personal.RecentlyViewedRepository;
import com.carefinder.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private final HospitalRepository hospitals;
    private final InsuranceProviderRepository insurers;
    private final UserRepository users;
    private final FavoriteRepository favorites;
    private final RecentlyViewedRepository recentlyViewed;
    private final ChatMessageRepository chats;
    private final AuditLogRepository auditLogs;

    public AnalyticsService(
            HospitalRepository hospitals,
            InsuranceProviderRepository insurers,
            UserRepository users,
            FavoriteRepository favorites,
            RecentlyViewedRepository recentlyViewed,
            ChatMessageRepository chats,
            AuditLogRepository auditLogs
    ) {
        this.hospitals = hospitals;
        this.insurers = insurers;
        this.users = users;
        this.favorites = favorites;
        this.recentlyViewed = recentlyViewed;
        this.chats = chats;
        this.auditLogs = auditLogs;
    }

    @Transactional(readOnly = true)
    public PublicStats publicStats() {
        return new PublicStats(
                hospitals.countByActiveTrue(),
                hospitals.countActiveStates(),
                hospitals.countActiveCities(),
                insurers.findAllByActiveTrueOrderByNameAsc().size()
        );
    }

    @Transactional(readOnly = true)
    public AdminStats adminStats() {
        PublicStats publicStats = publicStats();
        return new AdminStats(
                publicStats,
                users.count(),
                favorites.count(),
                recentlyViewed.count(),
                chats.count(),
                auditLogs.count()
        );
    }

    public record PublicStats(long hospitals, long statesAndUnionTerritories, long cities, long insurers) {
    }

    public record AdminStats(
            PublicStats directory,
            long users,
            long favorites,
            long recentlyViewed,
            long chatbotMessages,
            long auditEvents
    ) {
    }
}
