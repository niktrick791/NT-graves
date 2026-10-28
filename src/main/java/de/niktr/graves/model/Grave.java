package de.niktr.graves.model;

import de.niktr.graves.config.GravesConfig;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class Grave {

    public static final NamespacedKey KEY_GRAVE_ID = new NamespacedKey("bordermc", "grave_id");
    public static final NamespacedKey KEY_GRAVE_PART = new NamespacedKey("bordermc", "grave_part");

    private final UUID id;
    private final Location location;
    private final UUID ownerUUID;
    private final String playerName;
    private final Inventory gui;
    private int storedXP;
    private final long spawnedTime;
    private final String texture;

    private UUID headEntityUuid;
    private UUID interactionEntityUuid;
    private final List<UUID> hologramEntityUuids = new ArrayList<>();
    private transient ArmorStand headEntity;
    private transient Interaction interactionEntity;
    private transient TextDisplay hologramEntity;
    private float currentRotation;
    private boolean removed = false;
    private volatile boolean pickingUp = false; // Anti-dupe: atomic pickup lock

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
        this.gui = Bukkit.createInventory(null, 54, GravesConfig.parseColor(title));
        if (items != null) {
            for (int i = 0; i < Math.min(items.size(), 54); i++) {
                ItemStack it = items.get(i);
                if (it != null && !it.getType().isAir()) {
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
                Entity found = world.getEntity(headEntityUuid);
                if (found instanceof ArmorStand stand && stand.isValid()) {
                    headEntity = stand;
                }
            }
        }

        if (headEntity == null || !headEntity.isValid()) {
            Location headLoc = location.clone().add(0.5, config.getHeadHeight(), 0.5);
            headLoc.setYaw(currentRotation);

            ArmorStand stand = world.spawn(headLoc, ArmorStand.class, s -> {
                s.setInvisible(true);
                s.setMarker(true); // Marker ArmorStands have no hitbox, cannot be clicked, cannot be looted
                s.setGravity(false);
                s.setSmall(true);
                s.setBasePlate(false);
                s.setArms(false);
                s.setInvulnerable(true);
                s.setCollidable(false);
                s.setPersistent(false);
                s.setCanPickupItems(false);
                for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
                    s.addEquipmentLock(slot, ArmorStand.LockType.REMOVING_OR_CHANGING);
                    s.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING);
                }
                s.getPersistentDataContainer().set(KEY_GRAVE_ID, PersistentDataType.STRING, id.toString());
                s.getPersistentDataContainer().set(KEY_GRAVE_PART, PersistentDataType.STRING, "head");

                ItemStack headItem = createPlayerSkull(config);
                s.getEquipment().setHelmet(headItem);
            });
            headEntity = stand;
            headEntityUuid = stand.getUniqueId();
        }

        // 2. Clickable Interaction Hitbox (Mojang native interaction entity)
        if (interactionEntity == null || !interactionEntity.isValid()) {
            if (interactionEntityUuid != null) {
                Entity found = world.getEntity(interactionEntityUuid);
                if (found instanceof Interaction it && it.isValid()) {
                    interactionEntity = it;
                }
            }
        }

        if (interactionEntity == null || !interactionEntity.isValid()) {
            Location interactLoc = location.clone().add(0.5, 0, 0.5);
            Interaction it = world.spawn(interactLoc, Interaction.class, i -> {
                i.setInteractionWidth(1.2f);
                i.setInteractionHeight(1.8f);
                i.setResponsive(true);
                i.setPersistent(false);
                i.getPersistentDataContainer().set(KEY_GRAVE_ID, PersistentDataType.STRING, id.toString());
                i.getPersistentDataContainer().set(KEY_GRAVE_PART, PersistentDataType.STRING, "interaction");
            });
            interactionEntity = it;
            interactionEntityUuid = it.getUniqueId();
        }

        // 3. TextDisplay hologram
        if (hologramEntity == null || !hologramEntity.isValid()) {
            if (!hologramEntityUuids.isEmpty()) {
                Entity found = world.getEntity(hologramEntityUuids.get(0));
                if (found instanceof TextDisplay td && td.isValid()) {
                    hologramEntity = td;
                }
            }
        }

        if (hologramEntity == null || !hologramEntity.isValid()) {
            hologramEntityUuids.clear();
            Location holoLoc = location.clone().add(0.5, config.getHologramHeight(), 0.5);

            TextDisplay textDisplay = world.spawn(holoLoc, TextDisplay.class, td -> {
                td.setPersistent(false);
                td.getPersistentDataContainer().set(KEY_GRAVE_ID, PersistentDataType.STRING, id.toString());
                td.getPersistentDataContainer().set(KEY_GRAVE_PART, PersistentDataType.STRING, "hologram");

                td.setBillboard(parseBillboard(config.getHologramBillboard()));
                td.setShadowed(config.isHologramShadow());
                td.setSeeThrough(config.isHologramSeeThrough());
                td.setAlignment(TextDisplay.TextAlignment.CENTER);
                td.setViewRange(2.5f);
                td.setTextOpacity((byte) -1);

                td.setDefaultBackground(false);
                td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));

                td.text(buildHologramComponent(config));
            });
            hologramEntity = textDisplay;
            hologramEntityUuids.add(textDisplay.getUniqueId());
        }
    }

    public void updateHologram(GravesConfig config) {
        if (removed) return;
        World world = location.getWorld();
        if (world == null || !world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;

        if (hologramEntity == null || !hologramEntity.isValid()) {
            if (!hologramEntityUuids.isEmpty()) {
                Entity found = world.getEntity(hologramEntityUuids.get(0));
                if (found instanceof TextDisplay td && td.isValid()) {
                    hologramEntity = td;
                }
            }
        }

        if (hologramEntity != null && hologramEntity.isValid()) {
            hologramEntity.text(buildHologramComponent(config));
        } else {
            hologramEntityUuids.clear();
            spawnEntities(config);
        }
    }

    public void tick(GravesConfig config) {
        if (removed) return;

        // Auto rotation
        if (config.isAutoRotationEnabled()) {
            currentRotation = (float) ((currentRotation + config.getAutoRotationSpeed()) % 360.0);
            if (headEntity != null && headEntity.isValid()) {
                headEntity.setRotation(currentRotation, 0);
            } else if (headEntityUuid != null && location.getWorld() != null) {
                Entity entity = location.getWorld().getEntity(headEntityUuid);
                if (entity instanceof ArmorStand stand && stand.isValid()) {
                    headEntity = stand;
                    stand.setRotation(currentRotation, 0);
                }
            }
        }

        updateHologram(config);
    }

    public Component buildHologramComponent(GravesConfig config) {
        long elapsedSec = Math.max(0, (System.currentTimeMillis() - spawnedTime) / 1000L);
        int despawnSec = config.getDespawnTimeSeconds();
        String timeStr;
        if (despawnSec == -1) {
            timeStr = "\u221e"; // Infinity symbol âˆž
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

        Component combined = Component.empty();
        List<String> lines = config.getHologramLines();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i)
                    .replace("%player%", playerName)
                    .replace("%despawn-time%", timeStr)
                    .replace("%item%", String.valueOf(items))
                    .replace("%xp%", String.valueOf(storedXP))
                    .replace("%world%", worldName)
                    .replace("%x%", String.valueOf(location.getBlockX()))
                    .replace("%y%", String.valueOf(location.getBlockY()))
                    .replace("%z%", String.valueOf(location.getBlockZ()));

            Component parsed = GravesConfig.parseColor(line);
            if (i > 0) {
                combined = combined.append(Component.newline());
            }
            combined = combined.append(parsed);
        }
        return combined;
    }

    private Display.Billboard parseBillboard(String val) {
        if (val == null) return Display.Billboard.CENTER;
        return switch (val.toLowerCase(Locale.ROOT)) {
            case "horizontal" -> Display.Billboard.HORIZONTAL;
            case "vertical" -> Display.Billboard.VERTICAL;
            case "fixed" -> Display.Billboard.FIXED;
            default -> Display.Billboard.CENTER;
        };
    }

    private ItemStack createPlayerSkull(GravesConfig config) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        String base64 = texture;
        if ((base64 == null || base64.isEmpty()) && config.isCustomSkullEnabled()) {
            base64 = config.getCustomSkullBase64();
        }

        if (base64 != null && !base64.isEmpty()) {
            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
                profile.setProperty(new ProfileProperty("textures", base64));
                meta.setPlayerProfile(profile);
                head.setItemMeta(meta);
                return head;
            } catch (Throwable ignored) {}
        }

        // Direct online player profile for immediate high-res skin
        org.bukkit.entity.Player online = Bukkit.getPlayer(ownerUUID);
        if (online != null) {
            try {
                meta.setPlayerProfile(online.getPlayerProfile());
                head.setItemMeta(meta);
                return head;
            } catch (Throwable ignored) {}
        }

        OfflinePlayer op = Bukkit.getOfflinePlayer(ownerUUID);
        meta.setOwningPlayer(op);
        head.setItemMeta(meta);
        return head;
    }

    public int countItems() {
        int count = 0;
        for (ItemStack is : gui.getContents()) {
            if (is != null && !is.getType().isAir()) {
                count += is.getAmount();
            }
        }
        return count;
    }

    public boolean isExpired(int despawnTimeSeconds) {
        if (despawnTimeSeconds == -1) return false;
        long elapsedSec = (System.currentTimeMillis() - spawnedTime) / 1000L;
        return elapsedSec >= despawnTimeSeconds;
    }

    public void removeEntities() {
        if (headEntity != null && headEntity.isValid()) {
            try { headEntity.getEquipment().clear(); } catch (Throwable ignored) {}
            headEntity.remove();
        } else if (headEntityUuid != null && location.getWorld() != null) {
            Entity stand = location.getWorld().getEntity(headEntityUuid);
            if (stand instanceof ArmorStand as) {
                try { as.getEquipment().clear(); } catch (Throwable ignored) {}
                as.remove();
            } else if (stand != null) {
                stand.remove();
            }
        }
        headEntity = null;
        headEntityUuid = null;

        if (interactionEntity != null && interactionEntity.isValid()) {
            interactionEntity.remove();
        } else if (interactionEntityUuid != null && location.getWorld() != null) {
            Entity it = location.getWorld().getEntity(interactionEntityUuid);
            if (it != null) it.remove();
        }
        interactionEntity = null;
        interactionEntityUuid = null;

        if (hologramEntity != null && hologramEntity.isValid()) {
            hologramEntity.remove();
        }
        for (UUID uuid : hologramEntityUuids) {
            if (location.getWorld() != null) {
                Entity text = location.getWorld().getEntity(uuid);
                if (text != null) text.remove();
            }
        }
        hologramEntity = null;
        hologramEntityUuids.clear();
    }

    public void closeInventoryForAll() {
        List<HumanEntity> viewers = new ArrayList<>(gui.getViewers());
        for (HumanEntity he : viewers) {
            he.closeInventory();
        }
    }

    public void markRemoved() {
        if (this.removed) return;
        this.removed = true;
        closeInventoryForAll();
        removeEntities();
    }

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Inventory getGui() {
        return gui;
    }

    public int getStoredXP() {
        return storedXP;
    }

    public void setStoredXP(int storedXP) {
        this.storedXP = storedXP;
    }

    public long getSpawnedTime() {
        return spawnedTime;
    }

    public String getTexture() {
        return texture;
    }

    public UUID getHeadEntityUuid() {
        return headEntityUuid;
    }

    public UUID getInteractionEntityUuid() {
        return interactionEntityUuid;
    }

    public List<UUID> getHologramEntityUuids() {
        return Collections.unmodifiableList(hologramEntityUuids);
    }

    public float getCurrentRotation() {
        return currentRotation;
    }

    /**
     * Atomically claims the pickup lock for this grave.
     * Returns true if the lock was acquired (this thread may proceed with pickup),
     * false if another pickup is already in progress (dupe protection).
     */
    public synchronized boolean tryLockPickup() {
        if (removed || pickingUp) return false;
        pickingUp = true;
        return true;
    }

    public synchronized void unlockPickup() {
        pickingUp = false;
    }

    public boolean isRemoved() {
        return removed;
    }
}

