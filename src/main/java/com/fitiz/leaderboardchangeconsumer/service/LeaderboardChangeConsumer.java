package com.fitiz.leaderboardchangeconsumer.service;

import com.fitiz.leaderboardchangeconsumer.model.LeaderboardCache;
import com.fitiz.leaderboardchangeconsumer.model.LeaderboardChangeData;
import com.fitiz.leaderboardchangeconsumer.model.LeaderboardData;
import com.fitiz.leaderboardchangeconsumer.model.LeaderboardUser;
import com.fitiz.leaderboardchangeconsumer.repository.LeaderboardRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaderboardChangeConsumer {
    public static final String LEADERBOARD_TOP_K_CACHE_KEY_PREFIX = "top-k-";
    public static final String LEADERBOARD_TOPIC = "leaderboard";
    public static final int THROTTLE_DURATION_IN_MS = 500;

    private final KafkaTemplate<String, LeaderboardCache> kafkaLeaderboardTemplate;
    private final LeaderboardRedisRepository leaderboardRedisRepository;

    @KafkaListener(
            topics = {"${prop.config.broker-properties.leaderboard-change-topic}"},
            groupId = "${prop.config.broker-properties.leaderboard-change-topic-consumer-group-id}",
            properties = {"spring.json.value.default.type=com.fitiz.leaderboardchangeconsumer.model.LeaderboardChangeData"}
    )
    public void leaderboardChangeConsumer(ConsumerRecord<String, LeaderboardChangeData> record) {
        LeaderboardChangeData data = record.value();
        var leaderboardKey = data.leaderboardKey();

        log.info("Leaderboard change consumed, for {} with timestamp {}", data.leaderboardKey(), data.changeTimestampMs());
        String leaderboardCacheKey = LEADERBOARD_TOP_K_CACHE_KEY_PREFIX + leaderboardKey;
        LeaderboardCache leaderboardCache = leaderboardRedisRepository.getLeaderboardCache(leaderboardCacheKey);
        long currentTimeMs = System.currentTimeMillis();

        if (!(Objects.isNull(leaderboardCache) || isThrottleDurationPassed(leaderboardCache.lastUpdatedTimeMs(), currentTimeMs))) {
            log.debug("Leaderboard cache update skipped...");
            return;
        }

        List<LeaderboardData> leaderboard = leaderboardRedisRepository.getLeaderboard(leaderboardKey);

        if (Objects.isNull(leaderboard) || leaderboard.isEmpty()) {
            log.debug("Leaderboard is empty for {}", leaderboardKey);
            return;
        }

        LeaderboardCache updatedLeaderboardCache = createLeaderboardCache(leaderboardKey,leaderboard, currentTimeMs);
        leaderboardRedisRepository.setLeaderboardCache(leaderboardCacheKey, updatedLeaderboardCache);
        log.info("Leaderboard cache updated in Redis");

        kafkaLeaderboardTemplate.send(LEADERBOARD_TOPIC, updatedLeaderboardCache);
        log.info("Leaderboard cache published to Kafka...");
    }

    private boolean isThrottleDurationPassed(long lastUpdatedTimeMs, long currentTimeMs) {
        return (currentTimeMs - lastUpdatedTimeMs) > THROTTLE_DURATION_IN_MS;
    }

    private LeaderboardCache createLeaderboardCache(String leaderboardKey, List<LeaderboardData> leaderboard, long currentTimeMs) {
        return new LeaderboardCache(leaderboardKey, assignRanks(leaderboard), currentTimeMs);
    }

    private List<LeaderboardUser> assignRanks(List<LeaderboardData> leaderboardDataList) {
        List<LeaderboardUser> leaderboardUsers = new ArrayList<>();
        int rank = 1;

        for (LeaderboardData leaderboardData : leaderboardDataList) {
            leaderboardUsers.add(new LeaderboardUser(leaderboardData.username(), rank++, leaderboardData.steps()));
        }
        return leaderboardUsers;
    }
}