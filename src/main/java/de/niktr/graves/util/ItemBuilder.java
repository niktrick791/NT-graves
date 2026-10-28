package de.niktr.graves.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.components.EquippableComponent;
import org.bukkit.persistence.PersistentDataType;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;
    private final List<Component> lore = new ArrayList<>();

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, Math.max(1, amount));
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item;
        this.meta = item.getItemMeta();
    }

    public ItemBuilder model(NamespacedKey model) {
        if (meta != null) meta.setItemModel(model);
        return this;
    }

    private static final net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer LEGACY_BUILDER =
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();

    public ItemBuilder name(String text) {
        if (text == null) return this;
        Component c = text.contains("§") || text.contains("&")
                ? LEGACY_BUILDER.deserialize(text.replace('&', '§'))
                : Component.text(text);
        return name(c);
    }

    public ItemBuilder name(String text, NamedTextColor color) {
        return name(Component.text(text, color));
    }

    public ItemBuilder name(Component component) {
        if (meta != null) meta.displayName(component.decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder lore(String text) {
        if (text == null) return this;
        Component c = text.contains("§") || text.contains("&")
                ? LEGACY_BUILDER.deserialize(text.replace('&', '§'))
                : Component.text(text);
        return lore(c);
    }

    public ItemBuilder lore(String text, NamedTextColor color) {
        return lore(Component.text(text, color));
    }

    public ItemBuilder lore(Component component) {
        lore.add(component.decoration(TextDecoration.ITALIC, false));
        return this;
    }

    public ItemBuilder lore(List<Component> components) {
        if (components != null) {
            for (Component c : components) {
                lore.add(c.decoration(TextDecoration.ITALIC, false));
            }
        }
        return this;
    }

    public ItemBuilder blank() {
        lore.add(Component.empty());
        return this;
    }

    public ItemBuilder glow(boolean glowing) {
        if (meta != null && glowing) meta.setEnchantmentGlintOverride(Boolean.TRUE);
        return this;
    }

    public ItemBuilder skull(OfflinePlayer owner) {
        if (meta instanceof SkullMeta skullMeta) skullMeta.setOwningPlayer(owner);
        return this;
    }

    public ItemBuilder skull(String ownerName) {
        if (meta instanceof SkullMeta skullMeta && ownerName != null && !ownerName.isBlank()) {
            try {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(ownerName));
            } catch (Throwable ignored) {}
        }
        return this;
    }

    public ItemBuilder skullTexture(String base64Texture, String ownerName) {
        if (meta instanceof SkullMeta skullMeta && base64Texture != null && !base64Texture.isBlank()) {
            try {
                PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID(), ownerName != null && !ownerName.isBlank() ? ownerName : "CustomHead");
                profile.setProperty(new ProfileProperty("textures", base64Texture));
                skullMeta.setPlayerProfile(profile);
            } catch (Throwable ignored) {}
        }
        return this;
    }

    public ItemBuilder notWearable() {
        if (meta == null) return this;

        EquippableComponent equippable = meta.getEquippable();
        if (equippable == null) return this;
        equippable.setAllowedEntities(EntityType.ARMOR_STAND);
        equippable.setDispensable(false);
        equippable.setSwappable(false);
        try { equippable.getClass().getMethod("setEquipOnInteract", boolean.class).invoke(equippable, false); } catch (Throwable ignored) {}
        meta.setEquippable(equippable);
        return this;
    }

    public <T, Z> ItemBuilder tag(NamespacedKey key, PersistentDataType<T, Z> type, Z value) {
        if (meta != null) meta.getPersistentDataContainer().set(key, type, value);
        return this;
    }

    public ItemBuilder maxStackSize(int size) {
        if (meta != null) {
            if (item.getType().getMaxDurability() > 0 && size > 1) {
                return this;
            }
            try {
                meta.setMaxStackSize(size);
            } catch (Throwable t) {
                try {
                    meta.setMaxStackSize(Math.max(1, Math.min(99, size)));
                } catch (Throwable ignored) {}
            }
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            if (!lore.isEmpty()) meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
