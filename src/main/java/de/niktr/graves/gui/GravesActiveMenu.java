package de.niktr.graves.gui;

import de.niktr.graves.util.Menu;
import de.niktr.graves.util.ItemBuilder;
import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.manager.GraveManager;
import de.niktr.graves.model.Grave;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class GravesActiveMenu extends Menu<Plugin> {

    private final GraveManager graveManager;
    private final int page;

    public GravesActiveMenu(Plugin plugin, Player player, GraveManager graveManager, Menu<?> parent, int page) {
        super(plugin, player, parent);
        this.graveManager = graveManager;
        this.page = page;
    }

    @Override
    public Component title() {
        return Component.text("Aktive Gr\u00e4ber \u2022 Seite " + (page + 1), NamedTextColor.DARK_AQUA);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    protected void build() {
        buildFrame();

        List<Grave> all = new ArrayList<>(graveManager.getAllGraves());
        all.sort((a, b) -> Long.compare(b.getSpawnedTime(), a.getSpawnedTime()));

        int pageSize = 36;
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, all.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            Grave grave = all.get(i);
            Location loc = grave.getLocation();
            String worldDisplay = loc.getWorld() != null ? graveManager.getConfig().getWorldDisplayName(loc.getWorld().getName()) : "Welt";

            long elapsedSec = Math.max(0, (System.currentTimeMillis() - grave.getSpawnedTime()) / 1000L);
            int despawnSec = graveManager.getConfig().getDespawnTimeSeconds();
            String timeDisplay;
            if (despawnSec == -1) {
                timeDisplay = "\u221e";
            } else {
                long left = Math.max(0, despawnSec - elapsedSec);
                timeDisplay = (left / 60) + "m " + (left % 60) + "s";
            }

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(grave.getOwnerUUID());
                meta.setOwningPlayer(op);
                head.setItemMeta(meta);
            }

            ItemStack displayItem = new ItemBuilder(head)
                    .name(Component.text(grave.getPlayerName(), NamedTextColor.YELLOW)
                            .append(Component.text(" (" + timeDisplay + " verbleibend)", NamedTextColor.GRAY)))
                    .lore(List.of(
                            Component.text("Ort: ", NamedTextColor.GRAY)
                                    .append(Component.text(worldDisplay + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(), NamedTextColor.WHITE)),
                            Component.text("Items: ", NamedTextColor.GRAY)
                                    .append(Component.text(grave.countItems() + " Gegenst\u00e4nde", NamedTextColor.GREEN)),
                            Component.text("Erfahrung: ", NamedTextColor.GRAY)
                                    .append(Component.text(grave.getStoredXP() + " XP", NamedTextColor.AQUA)),
                            Component.empty(),
                            Component.text("\u25b6 Linksklick: ", NamedTextColor.GOLD)
                                    .append(Component.text("Teleportieren", NamedTextColor.GREEN)),
                            Component.text("\u25b6 Rechtsklick: ", NamedTextColor.GOLD)
                                    .append(Component.text("Inventar ansehen", NamedTextColor.YELLOW)),
                            Component.text("\u25b6 Shift-Rechtsklick: ", NamedTextColor.RED)
                                    .append(Component.text("L\u00f6schen & Archivieren", NamedTextColor.RED))
                    ))
                    .build();

            set(slot, displayItem, e -> {
                if (e.isShiftClick() && e.isRightClick()) {
                    graveManager.removeGrave(grave, false, "Admin-L\u00f6schung via GUI");
                    player.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#FF5555Grab von &f" + grave.getPlayerName() + " &#FF5555gel\u00f6scht und archiviert."));
                    try { player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 1.0f); } catch (Throwable ignored) {}
                    refresh();
                } else if (e.isRightClick()) {
                    try { player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f); } catch (Throwable ignored) {}
                    player.openInventory(grave.getGui());
                } else {
                    Location target = loc.clone().add(0.5, 0.1, 0.5);
                    player.teleportAsync(target);
                    player.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#FFAAFFTeleportiert zum Grab von &f" + grave.getPlayerName() + "&7!"));
                    try { player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f); } catch (Throwable ignored) {}
                    player.closeInventory();
                }
            });

            slot++;
            if ((slot % 9) == 8) slot += 2; // Rand freilassen
            if (slot >= 44) break;
        }

        // Navigation
        if (page > 0) {
            set(45, new ItemBuilder(Material.ARROW)
                    .name(Component.text("\u00ab Vorherige Seite", NamedTextColor.YELLOW))
                    .build(), e -> openLater(new GravesActiveMenu(plugin, player, graveManager, parent, page - 1)));
        }

        set(49, new ItemBuilder(Material.BARRIER)
                .name(Component.text("Zur\u00fcck zum Men\u00fc", NamedTextColor.RED))
                .build(), e -> {
            if (parent != null) parent.open();
            else player.closeInventory();
        });

        if (endIndex < all.size()) {
            set(53, new ItemBuilder(Material.ARROW)
                    .name(Component.text("N\u00e4chste Seite \u00bb", NamedTextColor.YELLOW))
                    .build(), e -> openLater(new GravesActiveMenu(plugin, player, graveManager, parent, page + 1)));
        }
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

