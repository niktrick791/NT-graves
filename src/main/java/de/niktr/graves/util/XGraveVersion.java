package de.niktr.graves.util;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XGraveVersion {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    public static final boolean HAS_PDC;
    public static final Material SKULL_MAT;
    public static final Material CLOCK_MAT;

    static {
        boolean pdc = false;
        try {
            Class.forName("org.bukkit.persistence.PersistentDataContainer");
            pdc = true;
        } catch (Throwable ignored) {}
        HAS_PDC = pdc;

        Material s = Material.matchMaterial("PLAYER_HEAD");
        if (s == null) s = Material.matchMaterial("SKULL_ITEM");
        SKULL_MAT = s != null ? s : Material.PUMPKIN;

        Material c = Material.matchMaterial("CLOCK");
        if (c == null) c = Material.matchMaterial("WATCH");
        CLOCK_MAT = c != null ? c : Material.COMPASS;
    }

    private XGraveVersion() {}

    public static Material safeMaterial(String modernName, String legacyName, Material fallback) {
        Material m = Material.matchMaterial(modernName);
        if (m != null) return m;
        if (legacyName != null) {
            m = Material.matchMaterial(legacyName);
            if (m != null) return m;
        }
        return fallback;
    }

    public static ItemStack createPane(int colorData, String modernName) {
        Material m = Material.matchMaterial(modernName);
        if (m != null) {
            return new ItemStack(m, 1);
        }
        Material glassPane = Material.matchMaterial("STAINED_GLASS_PANE");
        if (glassPane != null) {
            return new ItemStack(glassPane, 1, (short) colorData);
        }
        Material plainPane = Material.matchMaterial("THIN_GLASS");
        if (plainPane != null) {
            return new ItemStack(plainPane, 1);
        }
        return new ItemStack(Material.GLASS, 1);
    }

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return "";
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (char c : hex.toCharArray()) {
                replacement.append('&').append(c);
            }
            matcher.appendReplacement(sb, replacement.toString());
        }
        matcher.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    public static boolean isAir(ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getType().name().endsWith("AIR");
    }

    public static int getMinHeight(World world) {
        if (world == null) return 0;
        try {
            Method m = world.getClass().getMethod("getMinHeight");
            return (Integer) m.invoke(world);
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static void playSound(Player player, String primarySound, String fallbackSound, float volume, float pitch) {
        if (player == null) return;
        try {
            Sound s = Sound.valueOf(primarySound);
            player.playSound(player.getLocation(), s, volume, pitch);
            return;
        } catch (Throwable ignored) {}

        if (fallbackSound != null) {
            try {
                Sound s = Sound.valueOf(fallbackSound);
                player.playSound(player.getLocation(), s, volume, pitch);
            } catch (Throwable ignored) {}
        }
    }

    public static void setGlint(ItemMeta meta) {
        if (meta == null) return;
        try {
            Method m = meta.getClass().getMethod("setEnchantmentGlintOverride", boolean.class);
            m.invoke(meta, true);
            return;
        } catch (Throwable ignored) {}

        try {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } catch (Throwable ignored) {}
    }

    public static ItemStack createPlayerSkull(String playerName, UUID playerUUID, String base64Texture) {
        ItemStack item;
        if (SKULL_MAT.name().equals("SKULL_ITEM")) {
            item = new ItemStack(SKULL_MAT, 1, (short) 3); // 3 = Human skull in 1.8-1.12
        } else {
            item = new ItemStack(SKULL_MAT, 1);
        }

        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta)) return item;
        SkullMeta meta = (SkullMeta) rawMeta;

        if (base64Texture != null && !base64Texture.trim().isEmpty()) {
            applyBase64Texture(meta, base64Texture);
        } else if (playerName != null && !playerName.isEmpty()) {
            try {
                meta.setOwner(playerName);
            } catch (Throwable ignored) {}
        }

        item.setItemMeta(meta);
        return item;
    }

    private static void applyBase64Texture(SkullMeta meta, String base64) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Constructor<?> profileCtor = gameProfileClass.getConstructor(UUID.class, String.class);
            Constructor<?> propCtor = propertyClass.getConstructor(String.class, String.class);

            Object profile = profileCtor.newInstance(UUID.randomUUID(), "GraveHead");
            Method getProperties = gameProfileClass.getMethod("getProperties");
            Object properties = getProperties.invoke(profile);
            Method putMethod = properties.getClass().getMethod("put", Object.class, Object.class);
            putMethod.invoke(properties, "textures", propCtor.newInstance("textures", base64));

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Throwable ignored) {}
    }

    public static ArmorStand spawnHologramStand(Location loc, String line, NamespacedKey idKey, String idStr) {
        if (loc == null || loc.getWorld() == null) return null;
        World w = loc.getWorld();
        ArmorStand stand = w.spawn(loc, ArmorStand.class);
        stand.setVisible(false);
        stand.setGravity(false);
        stand.setSmall(true);
        stand.setBasePlate(false);
        stand.setArms(false);
        stand.setCustomName(colorize(line));
        stand.setCustomNameVisible(true);

        try {
            stand.setMarker(true);
        } catch (Throwable ignored) {}

        try {
            stand.setInvulnerable(true);
        } catch (Throwable ignored) {}

        if (HAS_PDC && idKey != null) {
            try {
                PersistentDataContainer pdc = stand.getPersistentDataContainer();
                pdc.set(idKey, PersistentDataType.STRING, idStr);
            } catch (Throwable ignored) {}
        }

        return stand;
    }
}
