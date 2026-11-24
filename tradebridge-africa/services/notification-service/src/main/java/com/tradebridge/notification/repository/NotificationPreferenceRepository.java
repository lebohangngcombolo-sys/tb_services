package com.tradebridge.notification.repository;

import com.tradebridge.common.enums.Channel;
import com.tradebridge.notification.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUserId(UUID userId);

    Optional<NotificationPreference> findByUserIdAndChannel(UUID userId, Channel channel);

    @Modifying
    @Query("DELETE FROM NotificationPreference p WHERE p.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}