package com.fitiz.leaderboardchangeconsumer.provider;

import com.fitiz.leaderboardchangeconsumer.model.LeaderboardCache;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class LeaderboardRedisProvider {

  private StatefulRedisConnection<String, String> connection;

  @Autowired
  private RedisTemplate<String, Object> leaderboardCacheTemplate;

  public LeaderboardRedisProvider(RedisStandaloneConfiguration redisStandaloneConfiguration) {
      RedisURI redisURI = RedisURI.builder()
                                .withHost(redisStandaloneConfiguration.getHostName())
                                .withPort(redisStandaloneConfiguration.getPort()).build();
    RedisClient redisClient = RedisClient.create(redisURI);
    this.connection = redisClient.connect();

  }

  public LeaderboardCache getLeaderboardCache(String leaderboardCacheKey) {
    return (LeaderboardCache) leaderboardCacheTemplate.opsForValue().get(leaderboardCacheKey);
  }

  public void setLeaderboardCache(String leaderboardCacheKey, LeaderboardCache leaderboardCache) {
      leaderboardCacheTemplate.opsForValue().set(leaderboardCacheKey, leaderboardCache);
  }

  public List<ScoredValue<String>> getLeaderboard(String leaderboardKey) {
    return connection.sync().zrevrangeWithScores(leaderboardKey, 0, 9);
  }
}
