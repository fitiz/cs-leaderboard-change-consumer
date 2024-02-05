package com.fitiz.leaderboardchangeconsumer.model;

import java.io.Serializable;

public record LeaderboardUser(String username, Integer rank, Integer steps) implements Serializable {
}
