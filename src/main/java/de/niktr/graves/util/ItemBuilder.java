package de.niktr.graves.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;
    private final List<String> lore = new ArrayList<String>();

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, Math.max(1, amount));
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item != null ? item.clone() : new ItemStack(Material.DIRT);
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder name(String text) {
        if (meta != null && text != null) {
            meta.setDisplayName(XGraveVersion.colorize(text));
        }
        return this;
    }

    public ItemBuilder lore(String text) {
        if (text != null) {
            lore.add(XGraveVersion.colorize(text));
        }
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (lines != null) {
            for (String line : lines) {
                lore.add(XGraveVersion.colorize(line));
            }
        }
        return this;
    }

    public ItemBuilder blank() {
        lore.add("");
        return this;
    }

    public ItemBuilder glow(boolean glowing) {
        if (meta != null && glowing) {
            XGraveVersion.setGlint(meta);
        }
        return this;
    }

    public ItemBuilder skull(String playerName, UUID playerUUID, String base64) {
        if (meta instanceof SkullMeta) {
            ItemStack skull = XGraveVersion.createPlayerSkull(playerName, playerUUID, base64);
            if (skull.hasItemMeta()) {
                ItemMeta skullMeta = skull.getItemMeta();
                if (meta.hasDisplayName()) skullMeta.setDisplayName(meta.getDisplayName());
                if (!lore.isEmpty()) skullMeta.setLore(lore);
                return new ItemBuilder(skull);
            }
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            if (!lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
