package com.fitiz.leaderboardchangeconsumer.model;

public record LeaderboardChangeData(String leaderboardKey, long changeTimestampMs) {
}
