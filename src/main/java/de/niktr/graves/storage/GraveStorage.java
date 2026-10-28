package de.niktr.graves.storage;

import de.niktr.graves.model.Grave;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class GraveStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<SavedGraveData>>() {}.getType();

    private final File dataFile;
    private final File legacyAxGravesFile;
    private final Logger logger;

    public GraveStorage(File dataFolder, Logger logger) {
        this.dataFile = new File(dataFolder, "data.json");
        this.logger = logger;

        File pluginsDir = dataFolder.getParentFile();
        this.legacyAxGravesFile = pluginsDir != null ? new File(new File(pluginsDir, "AxGraves"), "data.json") : null;
    }

    public synchronized void save(Collection<Grave> graves) {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }

            List<SavedGraveData> list = new ArrayList<>();
            for (Grave grave : graves) {
                if (grave.isRemoved()) continue;
                String locStr = serializeLocation(grave.getLocation());
                String itemsBase64 = itemsToBase64(grave.getGui().getContents());
                list.add(new SavedGraveData(
                        locStr,
                        grave.getOwnerUUID().toString(),
                        grave.getPlayerName(),
                        grave.getTexture(),
                        itemsBase64,
                        grave.getStoredXP(),
                        grave.getSpawnedTime()
                ));
            }

            File tempFile = new File(dataFile.getParentFile(), "data.json.tmp");
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                GSON.toJson(list, LIST_TYPE, writer);
            }

            if (dataFile.exists()) {
                dataFile.delete();
            }
            tempFile.renameTo(dataFile);
        } catch (Exception e) {
            logger.severe("[BorderMC-Graves] Failed to save graves to data.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized List<Grave> load() {
        File targetFile = dataFile;
        if (!targetFile.exists() && legacyAxGravesFile != null && legacyAxGravesFile.exists()) {
            logger.info("[BorderMC-Graves] Importing legacy AxGraves data from " + legacyAxGravesFile.getPath());
            targetFile = legacyAxGravesFile;
        }

        if (!targetFile.exists()) {
            return new ArrayList<>();
        }

        List<Grave> result = new ArrayList<>();
        try (Reader reader = new InputStreamReader(new FileInputStream(targetFile), StandardCharsets.UTF_8)) {
            List<SavedGraveData> list = GSON.fromJson(reader, LIST_TYPE);
            if (list == null || list.isEmpty()) {
                return result;
            }

            for (SavedGraveData data : list) {
                try {
                    Location loc = deserializeLocation(data.getLocation());
                    if (loc == null || loc.getWorld() == null) {
                        logger.warning("[BorderMC-Graves] Skipping grave with invalid world: " + data.getLocation());
                        continue;
                    }

                    UUID ownerUuid;
                    try {
                        ownerUuid = UUID.fromString(data.getOwner());
                    } catch (Exception ex) {
                        logger.warning("[BorderMC-Graves] Skipping grave with invalid owner UUID: " + data.getOwner());
                        continue;
                    }

                    String name = data.getPlayerName();
                    if (name == null || name.isEmpty()) {
                        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(ownerUuid);
                        name = op.getName() != null ? op.getName() : "Unknown";
                    }

                    ItemStack[] items = itemsFromBase64(data.getItems());
                    List<ItemStack> itemList = items != null ? Arrays.asList(items) : Collections.emptyList();

                    Grave grave = new Grave(
                            UUID.randomUUID(),
                            loc,
                            ownerUuid,
                            name,
                            itemList,
                            data.getXp(),
                            data.getDate(),
                            data.getTexture(),
                            loc.getYaw()
                    );
                    result.add(grave);
                } catch (Exception e) {
                    logger.severe("[BorderMC-Graves] Error restoring grave: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.severe("[BorderMC-Graves] Failed to load data.json: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public static String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return "world;0;0;0;0;0";
        return loc.getWorld().getName() + ";" +
                loc.getX() + ";" +
                loc.getY() + ";" +
                loc.getZ() + ";" +
                loc.getYaw() + ";" +
                loc.getPitch();
    }

    public static Location deserializeLocation(String str) {
        if (str == null || str.isEmpty()) return null;
        String[] parts = str.split(";");
        if (parts.length < 4) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
        float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;

        return new Location(world, x, y, z, yaw, pitch);
    }

    public static String itemsToBase64(ItemStack[] items) {
        if (items == null || items.length == 0) return "";
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.flush();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static ItemStack[] itemsFromBase64(String data) {
        if (data == null || data.isEmpty()) return new ItemStack[0];
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                 BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                int length = dataInput.readInt();
                ItemStack[] items = new ItemStack[length];
                for (int i = 0; i < length; i++) {
                    items[i] = (ItemStack) dataInput.readObject();
                }
                return items;
            }
        } catch (Exception e) {
            // Silently handle empty or unparseable item data
            return new ItemStack[0];
        }
    }
}

