package com.fitiz.leaderboardchangeconsumer.repository;

import com.fitiz.leaderboardchangeconsumer.model.LeaderboardCache;
import com.fitiz.leaderboardchangeconsumer.model.LeaderboardData;
import com.fitiz.leaderboardchangeconsumer.provider.LeaderboardRedisProvider;
import io.lettuce.core.ScoredValue;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LeaderboardRedisRepository {

    private final LeaderboardRedisProvider leaderboardRedisProvider;

    public LeaderboardRedisRepository(LeaderboardRedisProvider leaderboardRedisProvider) {
        this.leaderboardRedisProvider = leaderboardRedisProvider;
    }

    public LeaderboardCache getLeaderboardCache(String leaderboardCacheKey) {
        return leaderboardRedisProvider.getLeaderboardCache(leaderboardCacheKey);
    }

    public void setLeaderboardCache(String leaderboardCacheKey, LeaderboardCache leaderboardCache) {
        leaderboardRedisProvider.setLeaderboardCache(leaderboardCacheKey, leaderboardCache);
    }

    public List<LeaderboardData> getLeaderboard(String leaderboardKey) {
        List<ScoredValue<String>> leaderboard = leaderboardRedisProvider.getLeaderboard(leaderboardKey);
        return leaderboard.stream()
                .map(data -> new LeaderboardData(
                        data.getValue(),
                        (int) data.getScore()))
                .collect(Collectors.toList());
    }
}
