package de.niktr.graves.model;

import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.util.XGraveVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.*;

public class Grave {

    public static final NamespacedKey KEY_GRAVE_ID;
    public static final NamespacedKey KEY_GRAVE_PART;

    static {
        NamespacedKey idKey = null;
        NamespacedKey partKey = null;
        try {
            Class.forName("org.bukkit.NamespacedKey");
            idKey = new NamespacedKey("niktrick_graves", "grave_id");
            partKey = new NamespacedKey("niktrick_graves", "grave_part");
        } catch (Throwable ignored) {}
        KEY_GRAVE_ID = idKey;
        KEY_GRAVE_PART = partKey;
    }

    private final UUID id;
    private final Location location;
    private final UUID ownerUUID;
    private final String playerName;
    private final Inventory gui;
    private int storedXP;
    private final long spawnedTime;
    private final String texture;

    private UUID headEntityUuid;
    private final List<UUID> hologramEntityUuids = new ArrayList<UUID>();
    private transient ArmorStand headEntity;
    private final transient List<ArmorStand> hologramEntities = new ArrayList<ArmorStand>();
    private float currentRotation;
    private boolean removed = false;
    private volatile boolean pickingUp = false;

    public Grave(UUID id, Location location, UUID ownerUUID, String playerName, List<ItemStack> items, int storedXP, long spawnedTime, String texture, float initialYaw) {
        this.id = id != null ? id : UUID.randomUUID();
        this.location = location.clone();
        this.ownerUUID = ownerUUID;
        this.playerName = playerName != null ? playerName : "Unknown";
        this.storedXP = storedXP;
        this.spawnedTime = spawnedTime;
        this.texture = texture;
        this.currentRotation = initialYaw;

        String title = "&0" + this.playerName + "'s Grave";
        this.gui = Bukkit.createInventory(null, 54, XGraveVersion.colorize(title));
        if (items != null) {
            for (int i = 0; i < Math.min(items.size(), 54); i++) {
                ItemStack it = items.get(i);
                if (it != null && !XGraveVersion.isAir(it)) {
                    this.gui.setItem(i, it.clone());
                }
            }
        }
    }

    public void spawnEntities(GravesConfig config) {
        if (removed || location.getWorld() == null || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return;
        }

        World world = location.getWorld();

        // 1. Head ArmorStand
        if (headEntity == null || !headEntity.isValid()) {
            if (headEntityUuid != null) {
                Entity found = getEntityByUuid(world, headEntityUuid);
                if (found instanceof ArmorStand && found.isValid()) {
                    headEntity = (ArmorStand) found;
                }
            }
        }

        if (headEntity == null || !headEntity.isValid()) {
            Location headLoc = location.clone().add(0.5, config.getHeadHeight(), 0.5);
            headLoc.setYaw(currentRotation);

            ArmorStand stand = world.spawn(headLoc, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setBasePlate(false);
            stand.setArms(false);

            try { stand.setMarker(true); } catch (Throwable ignored) {}
            try { stand.setInvulnerable(true); } catch (Throwable ignored) {}

            if (XGraveVersion.HAS_PDC && KEY_GRAVE_ID != null) {
                try {
                    stand.getPersistentDataContainer().set(KEY_GRAVE_ID, PersistentDataType.STRING, id.toString());
                    stand.getPersistentDataContainer().set(KEY_GRAVE_PART, PersistentDataType.STRING, "head");
                } catch (Throwable ignored) {}
            }

            ItemStack headItem = XGraveVersion.createPlayerSkull(playerName, ownerUUID, texture != null ? texture : config.getCustomSkullBase64());
            stand.setHelmet(headItem);

            headEntity = stand;
            headEntityUuid = stand.getUniqueId();
        }

        // 2. Hologram ArmorStands (1 per line)
        spawnOrUpdateHologramStands(config);
    }

    private void spawnOrUpdateHologramStands(GravesConfig config) {
        if (removed || location.getWorld() == null) return;
        World world = location.getWorld();

        List<String> lines = getFormattedHologramLines(config);
        // Reverse so top line is displayed at the top
        Collections.reverse(lines);

        // Clear invalid entities
        Iterator<ArmorStand> it = hologramEntities.iterator();
        while (it.hasNext()) {
            ArmorStand s = it.next();
            if (s == null || !s.isValid()) it.remove();
        }

        if (hologramEntities.size() != lines.size()) {
            // Remove existing and respawn
            for (ArmorStand s : hologramEntities) {
                if (s != null && s.isValid()) s.remove();
            }
            hologramEntities.clear();
            hologramEntityUuids.clear();

            for (int i = 0; i < lines.size(); i++) {
                double yOffset = config.getHologramHeight() + (i * 0.28);
                Location lineLoc = location.clone().add(0.5, yOffset, 0.5);
                ArmorStand lineStand = XGraveVersion.spawnHologramStand(lineLoc, lines.get(i), KEY_GRAVE_ID, id.toString());
                if (lineStand != null) {
                    hologramEntities.add(lineStand);
                    hologramEntityUuids.add(lineStand.getUniqueId());
                }
            }
        } else {
            // Update custom name of each existing stand
            for (int i = 0; i < lines.size(); i++) {
                ArmorStand s = hologramEntities.get(i);
                if (s != null && s.isValid()) {
                    s.setCustomName(lines.get(i));
                }
            }
        }
    }

    public List<String> getFormattedHologramLines(GravesConfig config) {
        long elapsedSec = Math.max(0, (System.currentTimeMillis() - spawnedTime) / 1000L);
        int despawnSec = config.getDespawnTimeSeconds();
        String timeStr;
        if (despawnSec == -1) {
            timeStr = "∞";
        } else {
            long remaining = Math.max(0, despawnSec - elapsedSec);
            long hours = remaining / 3600;
            long minutes = (remaining % 3600) / 60;
            long seconds = remaining % 60;
            if (hours > 0) {
                timeStr = String.format("%dh %02dm %02ds", hours, minutes, seconds);
            } else if (minutes > 0) {
                timeStr = String.format("%dm %02ds", minutes, seconds);
            } else {
                timeStr = remaining + "s";
            }
        }

        int items = countItems();
        String worldName = location.getWorld() != null ? config.getWorldDisplayName(location.getWorld().getName()) : "World";

        List<String> rawLines = config.getHologramLines();
        List<String> formatted = new ArrayList<String>();
        for (String line : rawLines) {
            String f = line.replace("%player%", playerName)
                    .replace("%despawn-time%", timeStr)
                    .replace("%item%", String.valueOf(items))
                    .replace("%xp%", String.valueOf(storedXP))
                    .replace("%world%", worldName)
                    .replace("%x%", String.valueOf(location.getBlockX()))
                    .replace("%y%", String.valueOf(location.getBlockY()))
                    .replace("%z%", String.valueOf(location.getBlockZ()));
            formatted.add(XGraveVersion.colorize(f));
        }
        return formatted;
    }

    public void updateHologram(GravesConfig config) {
        if (removed) return;
        World world = location.getWorld();
        if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;

        spawnOrUpdateHologramStands(config);
    }

    public void tick(GravesConfig config) {
        if (removed) return;

        // Auto rotation of head
        if (config.isAutoRotationEnabled()) {
            currentRotation = (float) ((currentRotation + config.getAutoRotationSpeed()) % 360.0);
            if (headEntity != null && headEntity.isValid()) {
                headEntity.teleport(headEntity.getLocation().setDirection(new org.bukkit.util.Vector(Math.cos(Math.toRadians(currentRotation)), 0, Math.sin(Math.toRadians(currentRotation)))));
            }
        }

        updateHologram(config);
    }

    public void removeEntities() {
        if (headEntity != null && headEntity.isValid()) {
            headEntity.remove();
            headEntity = null;
        } else if (headEntityUuid != null && location.getWorld() != null) {
            Entity e = getEntityByUuid(location.getWorld(), headEntityUuid);
            if (e != null) e.remove();
        }

        for (ArmorStand s : hologramEntities) {
            if (s != null && s.isValid()) s.remove();
        }
        hologramEntities.clear();

        if (location.getWorld() != null) {
            for (UUID uuid : hologramEntityUuids) {
                Entity e = getEntityByUuid(location.getWorld(), uuid);
                if (e != null) e.remove();
            }
        }
        hologramEntityUuids.clear();
    }

    private Entity getEntityByUuid(World world, UUID uuid) {
        if (world == null || uuid == null) return null;
        try {
            Method m = world.getClass().getMethod("getEntity", UUID.class);
            return (Entity) m.invoke(world, uuid);
        } catch (Throwable t) {
            for (Entity e : world.getEntities()) {
                if (e.getUniqueId().equals(uuid)) return e;
            }
            return null;
        }
    }

    public void markRemoved() {
        this.removed = true;
        closeAllViewers();
        removeEntities();
    }

    public void closeAllViewers() {
        if (gui == null) return;
        List<HumanEntity> viewers = new ArrayList<HumanEntity>(gui.getViewers());
        for (HumanEntity he : viewers) {
            he.closeInventory();
        }
    }

    public boolean isExpired(int despawnSec) {
        if (despawnSec <= 0) return false;
        long elapsedSec = (System.currentTimeMillis() - spawnedTime) / 1000L;
        return elapsedSec >= despawnSec;
    }

    public int countItems() {
        int count = 0;
        for (ItemStack is : gui.getContents()) {
            if (!XGraveVersion.isAir(is)) {
                count += is.getAmount();
            }
        }
        return count;
    }

    public UUID getId() { return id; }
    public Location getLocation() { return location; }
    public UUID getOwnerUUID() { return ownerUUID; }
    public String getPlayerName() { return playerName; }
    public Inventory getGui() { return gui; }
    public int getStoredXP() { return storedXP; }
    public void setStoredXP(int storedXP) { this.storedXP = storedXP; }
    public long getSpawnedTime() { return spawnedTime; }
    public String getTexture() { return texture; }
    public float getCurrentRotation() { return currentRotation; }
    public boolean isRemoved() { return removed; }
    public UUID getHeadEntityUuid() { return headEntityUuid; }
    public void setHeadEntityUuid(UUID headEntityUuid) { this.headEntityUuid = headEntityUuid; }
    public UUID getInteractionEntityUuid() { return null; }
    public void setInteractionEntityUuid(UUID interactionEntityUuid) {}
    public List<UUID> getHologramEntityUuids() { return hologramEntityUuids; }
    public boolean isPickingUp() { return pickingUp; }
    public void setPickingUp(boolean pickingUp) { this.pickingUp = pickingUp; }

    public synchronized boolean tryLockPickup() {
        if (pickingUp) return false;
        pickingUp = true;
        return true;
    }

    public synchronized void unlockPickup() {
        pickingUp = false;
    }
}
