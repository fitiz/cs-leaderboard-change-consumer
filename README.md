### Challenge service Kafka consumer for leaderboard changes

- Kafka consumer for updating the leaderboard cache in Redis
- It simply compares the last change timestamp of the updated leaderboard with the current timestamp, if it is greater than 500ms (throttle duration), it fetches the top-10 user from Redis with **ZREVRANGE** and publishes it as a "leaderboard" topic. It also puts this cache in Redis.

```java
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
```
