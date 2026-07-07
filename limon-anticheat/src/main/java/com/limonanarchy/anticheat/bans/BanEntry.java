package com.limonanarchy.anticheat.bans;

public class BanEntry {

    private final String playerName;
    private final String reason;
    private final String bannedBy;
    private final long bannedAt;
    private final long expiresAt; // -1 = навсегда

    public BanEntry(String playerName, String reason, String bannedBy, long bannedAt, long expiresAt) {
        this.playerName = playerName;
        this.reason = reason;
        this.bannedBy = bannedBy;
        this.bannedAt = bannedAt;
        this.expiresAt = expiresAt;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getReason() {
        return reason;
    }

    public String getBannedBy() {
        return bannedBy;
    }

    public long getBannedAt() {
        return bannedAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public boolean isPermanent() {
        return expiresAt == -1;
    }

    public boolean isExpired() {
        return !isPermanent() && System.currentTimeMillis() >= expiresAt;
    }

    public long getRemainingMillis() {
        if (isPermanent()) return -1;
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }
}
