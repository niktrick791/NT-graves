package de.niktr.graves.storage;

public class SavedGraveData {
    private String location;
    private String owner;
    private String playerName;
    private String texture;
    private String items;
    private int xp;
    private long date;

    public SavedGraveData() {
    }

    public SavedGraveData(String location, String owner, String playerName, String texture, String items, int xp, long date) {
        this.location = location;
        this.owner = owner;
        this.playerName = playerName;
        this.texture = texture;
        this.items = items;
        this.xp = xp;
        this.date = date;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
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

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }
}

