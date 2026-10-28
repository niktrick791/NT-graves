package de.niktr.graves.manager;

import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.model.Grave;
import de.niktr.graves.storage.ArchivedGraveData;
import de.niktr.graves.storage.GraveArchiveStorage;
import de.niktr.graves.storage.GraveStorage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class GraveManager {

    private final Plugin plugin;
    private final GravesConfig config;
    private final GraveStorage storage;
    private final GraveArchiveStorage archiveStorage;
    private final Logger logger;

    private final Map<UUID, Grave> graves = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> entityToGrave = new ConcurrentHashMap<>();

    public GraveManager(Plugin plugin, GravesConfig config, GraveStorage storage, GraveArchiveStorage archiveStorage, Logger logger) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
        this.archiveStorage = archiveStorage;
        this.logger = logger;
    }

    public void loadAll() {
        graves.clear();
        entityToGrave.clear();

        archiveStorage.load();

        List<Grave> loaded = storage.load();
        for (Grave grave : loaded) {
            graves.put(grave.getId(), grave);
            registerGraveEntities(grave);
        }
        logger.info("[BorderMC-Graves] " + graves.size() + " Graeber erfolgreich geladen.");

        cleanupOrphanedEntities();

        for (Grave grave : graves.values()) {
            grave.spawnEntities(config);
            registerGraveEntities(grave);
        }
    }

    public void saveAll() {
        storage.save(graves.values());
        archiveStorage.save();
    }

    public Grave createGrave(Player player, Location deathLocation, List<ItemStack> items, int storedXP) {
        int limit = getPlayerGraveLimit(player);
        if (limit > 0) {
            List<Grave> playerGraves = getGravesByPlayer(player.getUniqueId());
            while (playerGraves.size() >= limit && !playerGraves.isEmpty()) {
                Grave oldest = playerGraves.get(playerGraves.size() - 1);
                removeGrave(oldest, config.isDropItems(), "Grave-Limit erreicht");
                playerGraves.remove(playerGraves.size() - 1);
            }
        }

        float yaw = player.getLocation().getYaw();
        if (!config.isRotateHead360()) {
            yaw = Math.round(yaw / 90.0f) * 90.0f;
        }

        UUID graveId = UUID.randomUUID();
        Grave grave = new Grave(
                graveId,
                deathLocation,
                player.getUniqueId(),
                player.getName(),
                items,
                storedXP,
                System.currentTimeMillis(),
                null,
                yaw
        );

        graves.put(graveId, grave);
        grave.spawnEntities(config);
        registerGraveEntities(grave);

        if (config.isSaveGravesEnabled()) {
            saveAll();
        }

        return grave;
    }

    public void removeGrave(Grave grave, boolean dropItems) {
        removeGrave(grave, dropItems, "Manuell/Abgeholt");
    }

    public void removeGrave(Grave grave, boolean dropItems, String archiveReason) {
        if (grave == null || grave.isRemoved()) return;

        // Anti-dupe & anti-recursion: unregister from active collections first
        graves.remove(grave.getId());
        unregisterGraveEntities(grave);

        // Snapshot items before clearing
        ItemStack[] itemsSnapshot = grave.getGui().getContents().clone();

        // Close viewers and remove entities cleanly
        grave.markRemoved();

        // Archivieren, wenn noch Items oder XP vorhanden sind
        if (grave.countItems() > 0 || grave.getStoredXP() > 0) {
            archiveStorage.archiveGrave(grave, archiveReason);
        }

        if (dropItems) {
            Location dropLoc = grave.getLocation().clone().add(0.5, 0.5, 0.5);
            World world = dropLoc.getWorld();
            if (world != null) {
                for (ItemStack is : itemsSnapshot) {
                    if (!de.niktr.graves.util.XGraveVersion.isAir(is)) {
                        Item itemDrop = world.dropItem(dropLoc, is);
                        if (config.isDroppedItemVelocity()) {
                            Vector vel = new Vector(
                                    (Math.random() - 0.5) * 0.25,
                                    0.2 + Math.random() * 0.15,
                                    (Math.random() - 0.5) * 0.25
                            );
                            itemDrop.setVelocity(vel);
                        } else {
                            itemDrop.setVelocity(new Vector(0, 0, 0));
                        }
                    }
                }

                if (grave.getStoredXP() > 0) {
                    world.spawn(dropLoc, ExperienceOrb.class, orb -> orb.setExperience(grave.getStoredXP()));
                    grave.setStoredXP(0);
                }
            }
        }

        grave.getGui().clear();

        if (config.isSaveGravesEnabled()) {
            saveAll();
        }
    }

    public void registerGraveEntities(Grave grave) {
        if (grave.getHeadEntityUuid() != null) {
            entityToGrave.put(grave.getHeadEntityUuid(), grave.getId());
        }
        if (grave.getInteractionEntityUuid() != null) {
            entityToGrave.put(grave.getInteractionEntityUuid(), grave.getId());
        }
        for (UUID holoUuid : grave.getHologramEntityUuids()) {
            entityToGrave.put(holoUuid, grave.getId());
        }
    }

    public void unregisterGraveEntities(Grave grave) {
        if (grave.getHeadEntityUuid() != null) {
            entityToGrave.remove(grave.getHeadEntityUuid());
        }
        if (grave.getInteractionEntityUuid() != null) {
            entityToGrave.remove(grave.getInteractionEntityUuid());
        }
        for (UUID holoUuid : grave.getHologramEntityUuids()) {
            entityToGrave.remove(holoUuid);
        }
    }

    public Grave getGraveByEntity(Entity entity) {
        if (entity == null) return null;

        UUID graveId = entityToGrave.get(entity.getUniqueId());
        if (graveId != null) {
            Grave g = graves.get(graveId);
            if (g != null && !g.isRemoved()) return g;
        }

        if (entity.getPersistentDataContainer().has(Grave.KEY_GRAVE_ID, PersistentDataType.STRING)) {
            String idStr = entity.getPersistentDataContainer().get(Grave.KEY_GRAVE_ID, PersistentDataType.STRING);
            try {
                UUID id = UUID.fromString(idStr);
                Grave g = graves.get(id);
                if (g != null && !g.isRemoved()) {
                    entityToGrave.put(entity.getUniqueId(), id);
                    return g;
                }
            } catch (Exception ignored) {}
        }

        return null;
    }

    public Grave getGraveAt(Location location) {
        if (location == null || location.getWorld() == null) return null;
        for (Grave g : graves.values()) {
            if (g.isRemoved()) continue;
            Location gl = g.getLocation();
            if (gl.getWorld() != null && gl.getWorld().equals(location.getWorld())) {
                if (Math.abs(gl.getBlockX() - location.getBlockX()) <= 1 &&
                        Math.abs(gl.getBlockZ() - location.getBlockZ()) <= 1 &&
                        Math.abs(gl.getY() - location.getY()) <= 2.5) {
                    return g;
                }
            }
        }
        return null;
    }

    public boolean restoreActiveGraveToPlayer(Grave grave, Player player) {
        if (grave == null || grave.isRemoved() || player == null || !player.isOnline()) {
            return false;
        }

        ItemStack[] contents = grave.getGui().getContents();
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();

        // 0. Remove any Shulker Head on player
        if (isShulkerHead(inv.getHelmet())) {
            inv.setHelmet(null);
        }

        // 1. Auto equip armor if slots are free
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (de.niktr.graves.util.XGraveVersion.isAir(item)) continue;

            if (isShulkerHead(item)) {
                contents[i] = null;
                continue;
            }

            String typeName = item.getType().name();
            if ((inv.getHelmet() == null || isShulkerHead(inv.getHelmet())) && typeName.endsWith("_HELMET")) {
                inv.setHelmet(item);
                contents[i] = null;
            } else if (inv.getChestplate() == null && (typeName.endsWith("_CHESTPLATE") || item.getType() == org.bukkit.Material.ELYTRA)) {
                inv.setChestplate(item);
                contents[i] = null;
            } else if (inv.getLeggings() == null && typeName.endsWith("_LEGGINGS")) {
                inv.setLeggings(item);
                contents[i] = null;
            } else if (inv.getBoots() == null && typeName.endsWith("_BOOTS")) {
                inv.setBoots(item);
                contents[i] = null;
            }
        }

        // 2. Add remaining items
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (de.niktr.graves.util.XGraveVersion.isAir(item)) continue;
            if (isShulkerHead(item)) continue;

            HashMap<Integer, ItemStack> leftover = inv.addItem(item);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            contents[i] = null;
        }

        // 3. Restore XP
        if (grave.getStoredXP() > 0) {
            player.giveExp(grave.getStoredXP());
            grave.setStoredXP(0);
        }

        removeGrave(grave, false, "Direkt wiederhergestellt an " + player.getName());
        return true;
    }

    public static boolean isShulkerHead(ItemStack is) {
        if (is == null || is.getType() != de.niktr.graves.util.XGraveVersion.SKULL_MAT) return false;
        if (is.hasItemMeta() && is.getItemMeta().hasDisplayName()) {
            String name = is.getItemMeta().getDisplayName();
            if (name != null && name.contains("Shulker Head")) return true;
        }
        return false;
    }

    public Grave getGrave(UUID graveId) {
        return graves.get(graveId);
    }

    public List<Grave> getGravesByPlayer(UUID playerUuid) {
        List<Grave> list = new ArrayList<>();
        for (Grave g : graves.values()) {
            if (!g.isRemoved() && g.getOwnerUUID().equals(playerUuid)) {
                list.add(g);
            }
        }
        list.sort((a, b) -> Long.compare(b.getSpawnedTime(), a.getSpawnedTime()));
        return list;
    }

    public Collection<Grave> getAllGraves() {
        return Collections.unmodifiableCollection(graves.values());
    }

    public void tick() {
        Iterator<Grave> it = graves.values().iterator();
        while (it.hasNext()) {
            Grave grave = it.next();
            if (grave.isRemoved()) {
                it.remove();
                continue;
            }

            Location loc = grave.getLocation();
            if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                grave.tick(config);
                registerGraveEntities(grave);
            }

            if (grave.isExpired(config.getDespawnTimeSeconds())) {
                removeGrave(grave, config.isDropItems(), "Abgelaufen (Zeitlimit erreicht)");
            }
        }
    }

    public void cleanupOrphanedEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getPersistentDataContainer().has(Grave.KEY_GRAVE_ID, PersistentDataType.STRING)) {
                    String idStr = entity.getPersistentDataContainer().get(Grave.KEY_GRAVE_ID, PersistentDataType.STRING);
                    try {
                        UUID id = UUID.fromString(idStr);
                        if (!graves.containsKey(id)) {
                            entity.remove();
                        }
                    } catch (Exception e) {
                        entity.remove();
                    }
                }
            }
        }
    }

    public void onChunkLoad(org.bukkit.Chunk chunk) {
        for (Grave grave : graves.values()) {
            if (grave.isRemoved()) continue;
            Location loc = grave.getLocation();
            if (loc.getWorld() != null && loc.getWorld().equals(chunk.getWorld())) {
                if ((loc.getBlockX() >> 4) == chunk.getX() && (loc.getBlockZ() >> 4) == chunk.getZ()) {
                    grave.spawnEntities(config);
                    registerGraveEntities(grave);
                }
            }
        }
    }

    private int getPlayerGraveLimit(Player player) {
        if (player.hasPermission("niktrick.graves.bypass")) {
            return -1;
        }
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("niktrick.graves.limit." + i)) {
                return i;
            }
        }
        return config.getGraveLimit();
    }

    public GravesConfig getConfig() {
        return config;
    }

    public GraveArchiveStorage getArchiveStorage() {
        return archiveStorage;
    }
}

