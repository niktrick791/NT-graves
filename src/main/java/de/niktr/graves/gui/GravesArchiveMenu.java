package de.niktr.graves.gui;

import de.niktr.graves.util.Menu;
import de.niktr.graves.util.ItemBuilder;
import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.manager.GraveManager;
import de.niktr.graves.storage.ArchivedGraveData;
import de.niktr.graves.storage.GraveStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GravesArchiveMenu extends Menu<Plugin> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    private final GraveManager graveManager;
    private final int page;

    public GravesArchiveMenu(Plugin plugin, Player player, GraveManager graveManager, Menu<?> parent, int page) {
        super(plugin, player, parent);
        this.graveManager = graveManager;
        this.page = page;
    }

    @Override
    public Component title() {
        return Component.text("Grab-Archiv \u2022 Seite " + (page + 1), NamedTextColor.GOLD);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    protected void build() {
        buildFrame();

        List<ArchivedGraveData> archive = graveManager.getArchiveStorage().getArchiveList();

        int pageSize = 36;
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, archive.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            ArchivedGraveData data = archive.get(i);
            String dateStr = DATE_FORMAT.format(new Date(data.getDeathTime()));
            ItemStack[] items = GraveStorage.itemsFromBase64(data.getItems());
            int itemCount = 0;
            for (ItemStack it : items) {
                if (it != null && !it.getType().isAir()) itemCount += it.getAmount();
            }

            Material icon = data.isRestored() ? Material.MINECART : Material.CHEST_MINECART;

            ItemStack displayItem = new ItemBuilder(icon)
                    .name(Component.text(data.getPlayerName(), NamedTextColor.GOLD)
                            .append(Component.text(" \u2022 " + dateStr, NamedTextColor.GRAY)))
                    .lore(List.of(
                            Component.text("Grund: ", NamedTextColor.GRAY)
                                    .append(Component.text(data.getReason(), NamedTextColor.YELLOW)),
                            Component.text("Ort: ", NamedTextColor.GRAY)
                                    .append(Component.text(data.getLocation(), NamedTextColor.WHITE)),
                            Component.text("Items: ", NamedTextColor.GRAY)
                                    .append(Component.text(itemCount + " Gegenst\u00e4nde", NamedTextColor.GREEN)),
                            Component.text("Erfahrung: ", NamedTextColor.GRAY)
                                    .append(Component.text(data.getXp() + " XP", NamedTextColor.AQUA)),
                            Component.text("Status: ", NamedTextColor.GRAY)
                                    .append(data.isRestored()
                                            ? Component.text("Wiederhergestellt", NamedTextColor.GREEN)
                                            : Component.text("Nicht wiederhergestellt", NamedTextColor.RED)),
                            Component.empty(),
                            Component.text("\u25b6 Linksklick: ", NamedTextColor.GREEN)
                                    .append(Component.text("Dem Spieler wiederherstellen", NamedTextColor.GREEN)),
                            Component.text("\u25b6 Rechtsklick: ", NamedTextColor.GOLD)
                                    .append(Component.text("Inventar ansehen & Items nehmen", NamedTextColor.YELLOW))
                    ))
                    .build();

            set(slot, displayItem, e -> {
                if (e.isRightClick()) {
                    openArchivedInventory(data);
                } else {
                    Player target = Bukkit.getPlayer(data.getPlayerName());
                    if (target != null && target.isOnline()) {
                        boolean ok = graveManager.getArchiveStorage().restoreGraveToPlayer(data, target);
                        if (ok) {
                            player.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#33FF33Inventar erfolgreich an &f" + target.getName() + " &#33FF33wiederhergestellt!"));
                            target.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#33FF33Ein Admin hat dein abgelaufenes Grab-Inventar wiederhergestellt!"));
                            try {
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                                target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                            } catch (Throwable ignored) {}
                            refresh();
                        } else {
                            player.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#FF3333Fehler bei der Wiederherstellung!"));
                        }
                    } else {
                        player.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#FFAA00Spieler &f" + data.getPlayerName() + " &#FFAA00ist aktuell offline! Klicke mit Rechtsklick, um die Items anzusehen."));
                    }
                }
            });

            slot++;
            if ((slot % 9) == 8) slot += 2;
            if (slot >= 44) break;
        }

        // Navigation
        if (page > 0) {
            set(45, new ItemBuilder(Material.ARROW)
                    .name(Component.text("\u00ab Vorherige Seite", NamedTextColor.YELLOW))
                    .build(), e -> openLater(new GravesArchiveMenu(plugin, player, graveManager, parent, page - 1)));
        }

        set(49, new ItemBuilder(Material.BARRIER)
                .name(Component.text("Zur\u00fcck zum Men\u00fc", NamedTextColor.RED))
                .build(), e -> {
            if (parent != null) parent.open();
            else player.closeInventory();
        });

        if (endIndex < archive.size()) {
            set(53, new ItemBuilder(Material.ARROW)
                    .name(Component.text("N\u00e4chste Seite \u00bb", NamedTextColor.YELLOW))
                    .build(), e -> openLater(new GravesArchiveMenu(plugin, player, graveManager, parent, page + 1)));
        }
    }

    private void openArchivedInventory(ArchivedGraveData data) {
        String title = "Archiv \u2022 " + data.getPlayerName();
        if (title.length() > 32) title = title.substring(0, 32);

        Inventory inv = Bukkit.createInventory(null, 54, Component.text(title, NamedTextColor.GOLD));
        ItemStack[] items = GraveStorage.itemsFromBase64(data.getItems());
        for (int i = 0; i < Math.min(items.length, 54); i++) {
            if (items[i] != null && !items[i].getType().isAir()) {
                inv.setItem(i, items[i].clone());
            }
        }
        player.openInventory(inv);
        try { player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f); } catch (Throwable ignored) {}
    }

    private void buildFrame() {
        ItemStack frame = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < 9; i++) set(i, frame);
        for (int i = 45; i < 54; i++) set(i, frame);
        for (int row = 1; row < 5; row++) {
            set(row * 9, frame);
            set(row * 9 + 8, frame);
        }
    }
}

