package de.niktr.graves.storage;

import java.util.UUID;

public class ArchivedGraveData {
    private String archiveId;
    private String originalGraveId;
    private String playerUuid;
    private String playerName;
    private String location;
    private long deathTime;
    private long archivedTime;
    private String reason;
    private String items;
    private int xp;
    private boolean restored;

    public ArchivedGraveData() {
    }

    public ArchivedGraveData(String archiveId, String originalGraveId, String playerUuid, String playerName, String location, long deathTime, long archivedTime, String reason, String items, int xp, boolean restored) {
        this.archiveId = archiveId;
        this.originalGraveId = originalGraveId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.location = location;
        this.deathTime = deathTime;
        this.archivedTime = archivedTime;
        this.reason = reason;
        this.items = items;
        this.xp = xp;
        this.restored = restored;
    }

    public String getArchiveId() {
        return archiveId;
    }

    public void setArchiveId(String archiveId) {
        this.archiveId = archiveId;
    }

    public String getOriginalGraveId() {
        return originalGraveId;
    }

    public void setOriginalGraveId(String originalGraveId) {
        this.originalGraveId = originalGraveId;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getDeathTime() {
        return deathTime;
    }

    public void setDeathTime(long deathTime) {
        this.deathTime = deathTime;
    }

    public long getArchivedTime() {
        return archivedTime;
    }

    public void setArchivedTime(long archivedTime) {
        this.archivedTime = archivedTime;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public boolean isRestored() {
        return restored;
    }

    public void setRestored(boolean restored) {
        this.restored = restored;
    }
}

