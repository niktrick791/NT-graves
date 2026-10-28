package de.niktr.graves.storage;

import de.niktr.graves.model.Grave;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

public class GraveArchiveStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<ArchivedGraveData>>() {}.getType();

    private final File archiveFile;
    private final Logger logger;
    private final List<ArchivedGraveData> archiveList = new CopyOnWriteArrayList<>();

    public GraveArchiveStorage(File dataFolder, Logger logger) {
        this.archiveFile = new File(dataFolder, "archive.json");
        this.logger = logger;
    }

    public synchronized void load() {
        archiveList.clear();
        if (!archiveFile.exists()) {
            return;
        }

        try (Reader reader = new InputStreamReader(new FileInputStream(archiveFile), StandardCharsets.UTF_8)) {
            List<ArchivedGraveData> list = GSON.fromJson(reader, LIST_TYPE);
            if (list != null) {
                archiveList.addAll(list);
            }
            logger.info("[BorderMC-Graves] " + archiveList.size() + " archivierte Graeber geladen.");
        } catch (Exception e) {
            logger.severe("[BorderMC-Graves] Fehler beim Laden von archive.json: " + e.getMessage());
        }
    }

    public synchronized void save() {
        try {
            if (!archiveFile.getParentFile().exists()) {
                archiveFile.getParentFile().mkdirs();
            }

            File tempFile = new File(archiveFile.getParentFile(), "archive.json.tmp");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                GSON.toJson(archiveList, LIST_TYPE, writer);
            }

            if (archiveFile.exists()) {
                archiveFile.delete();
            }
            tempFile.renameTo(archiveFile);
        } catch (Exception e) {
            logger.severe("[BorderMC-Graves] Fehler beim Speichern von archive.json: " + e.getMessage());
        }
    }

    public void archiveGrave(Grave grave, String reason) {
        if (grave == null) return;
        if (grave.countItems() == 0 && grave.getStoredXP() == 0) {
            return; // Nichts zu archivieren, wenn die Kiste komplett leer war
        }

        String locStr = GraveStorage.serializeLocation(grave.getLocation());
        String itemsBase64 = GraveStorage.itemsToBase64(grave.getGui().getContents());

        ArchivedGraveData data = new ArchivedGraveData(
                UUID.randomUUID().toString(),
                grave.getId().toString(),
                grave.getOwnerUUID().toString(),
                grave.getPlayerName(),
                locStr,
                grave.getSpawnedTime(),
                System.currentTimeMillis(),
                reason != null ? reason : "Abgelaufen",
                itemsBase64,
                grave.getStoredXP(),
                false
        );

        archiveList.add(0, data);

        // Begrenze Archiv auf max. 500 Eintrï¿½ge
        while (archiveList.size() > 500) {
            archiveList.remove(archiveList.size() - 1);
        }

        save();
    }

    public List<ArchivedGraveData> getArchiveList() {
        return Collections.unmodifiableList(archiveList);
    }

    public ArchivedGraveData getArchivedGrave(String archiveId) {
        for (ArchivedGraveData d : archiveList) {
            if (d.getArchiveId().equalsIgnoreCase(archiveId)) {
                return d;
            }
        }
        return null;
    }

    public List<ArchivedGraveData> getArchivedGravesForPlayer(UUID playerUuid) {
        String uuidStr = playerUuid.toString();
        List<ArchivedGraveData> res = new ArrayList<>();
        for (ArchivedGraveData d : archiveList) {
            if (d.getPlayerUuid().equalsIgnoreCase(uuidStr)) {
                res.add(d);
            }
        }
        return res;
    }

    public boolean restoreGraveToPlayer(ArchivedGraveData data, Player player) {
        if (data == null || player == null || !player.isOnline()) {
            return false;
        }

        ItemStack[] items = GraveStorage.itemsFromBase64(data.getItems());
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();

        // 0. Remove any Shulker Head on player
        if (de.niktr.graves.manager.GraveManager.isShulkerHead(inv.getHelmet())) {
            inv.setHelmet(null);
        }

        // 1. Auto-equip armor
        for (int i = 0; i < items.length; i++) {
            ItemStack is = items[i];
            if (de.niktr.graves.util.XGraveVersion.isAir(is)) continue;

            if (de.niktr.graves.manager.GraveManager.isShulkerHead(is)) {
                items[i] = null;
                continue;
            }

            String typeName = is.getType().name();
            if ((inv.getHelmet() == null || de.niktr.graves.manager.GraveManager.isShulkerHead(inv.getHelmet())) && typeName.endsWith("_HELMET")) {
                inv.setHelmet(is);
                items[i] = null;
            } else if (inv.getChestplate() == null && (typeName.endsWith("_CHESTPLATE") || is.getType() == org.bukkit.Material.ELYTRA)) {
                inv.setChestplate(is);
                items[i] = null;
            } else if (inv.getLeggings() == null && typeName.endsWith("_LEGGINGS")) {
                inv.setLeggings(is);
                items[i] = null;
            } else if (inv.getBoots() == null && typeName.endsWith("_BOOTS")) {
                inv.setBoots(is);
                items[i] = null;
            }
        }

        // 2. Add remaining items
        for (int i = 0; i < items.length; i++) {
            ItemStack is = items[i];
            if (de.niktr.graves.util.XGraveVersion.isAir(is)) continue;
            if (de.niktr.graves.manager.GraveManager.isShulkerHead(is)) continue;

            HashMap<Integer, ItemStack> leftover = inv.addItem(is);
            if (!leftover.isEmpty()) {
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
            items[i] = null;
        }

        if (data.getXp() > 0) {
            player.giveExp(data.getXp());
        }

        data.setRestored(true);
        save();
        return true;
    }
}

