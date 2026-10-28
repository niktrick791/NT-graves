package de.niktr.graves.gui;

import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.manager.GraveManager;
import de.niktr.graves.model.Grave;
import de.niktr.graves.util.ItemBuilder;
import de.niktr.graves.util.Menu;
import de.niktr.graves.util.XGraveVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
    public String title() {
        return "&3Aktive Gräber &8• &7Seite " + (page + 1);
    }

    @Override
    public int size() {
        return 54;
    }

    @Override
    protected void build() {
        buildFrame();

        List<Grave> all = new ArrayList<Grave>(graveManager.getAllGraves());
        Collections.sort(all, new Comparator<Grave>() {
            @Override
            public int compare(Grave a, Grave b) {
                return Long.compare(b.getSpawnedTime(), a.getSpawnedTime());
            }
        });

        int pageSize = 36;
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, all.size());

        int slot = 9;
        for (int i = startIndex; i < endIndex; i++) {
            final Grave grave = all.get(i);
            final Location loc = grave.getLocation();
            String worldDisplay = loc.getWorld() != null ? graveManager.getConfig().getWorldDisplayName(loc.getWorld().getName()) : "Welt";

            long elapsedSec = Math.max(0, (System.currentTimeMillis() - grave.getSpawnedTime()) / 1000L);
            int despawnSec = graveManager.getConfig().getDespawnTimeSeconds();
            String timeDisplay;
            if (despawnSec == -1) {
                timeDisplay = "∞";
            } else {
                long left = Math.max(0, despawnSec - elapsedSec);
                timeDisplay = (left / 60) + "m " + (left % 60) + "s";
            }

            ItemStack head = XGraveVersion.createPlayerSkull(grave.getPlayerName(), grave.getOwnerUUID(), grave.getTexture());
            ItemStack displayItem = new ItemBuilder(head)
                    .name("&e" + grave.getPlayerName() + " &7(" + timeDisplay + " verbleibend)")
                    .lore(Arrays.asList(
                            "&7Ort: &f" + worldDisplay + " " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                            "&7Items: &a" + grave.countItems() + " Gegenstände",
                            "&7Erfahrung: &b" + grave.getStoredXP() + " XP",
                            "",
                            "&6▶ Linksklick: &aTeleportieren",
                            "&6▶ Rechtsklick: &eInventar ansehen",
                            "&c▶ Shift-Rechtsklick: &4Löschen & Archivieren"
                    ))
                    .build();

            set(slot, displayItem, new ClickAction() {
                @Override
                public void execute(InventoryClickEvent e) {
                    if (e.isShiftClick() && e.isRightClick()) {
                        graveManager.removeGrave(grave, false, "Admin-Löschung via GUI");
                        player.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#FF5555Grab von &f" + grave.getPlayerName() + " &#FF5555gelöscht und archiviert."));
                        XGraveVersion.playSound(player, "ENTITY_ITEM_BREAK", "ITEM_BREAK", 0.8f, 1.0f);
                        refresh();
                    } else if (e.isRightClick()) {
                        XGraveVersion.playSound(player, "BLOCK_CHEST_OPEN", "CHEST_OPEN", 0.8f, 1.0f);
                        player.openInventory(grave.getGui());
                    } else {
                        Location target = loc.clone().add(0.5, 0.1, 0.5);
                        player.teleport(target);
                        player.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#FFAAFFTeleportiert zum Grab von &f" + grave.getPlayerName() + "&7!"));
                        XGraveVersion.playSound(player, "ENTITY_ENDERMAN_TELEPORT", "ENDERMAN_TELEPORT", 0.8f, 1.0f);
                        player.closeInventory();
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
                    .name("&e« Vorherige Seite")
                    .build(), new ClickAction() {
                @Override
                public void execute(InventoryClickEvent e) {
                    openLater(new GravesActiveMenu(plugin, player, graveManager, parent, page - 1));
                }
            });
        }

        Material barrierMat = XGraveVersion.safeMaterial("BARRIER", "REDSTONE_BLOCK", Material.REDSTONE_BLOCK);
        set(49, new ItemBuilder(barrierMat)
                .name("&cZurück zum Menü")
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                if (parent != null) parent.open();
                else player.closeInventory();
            }
        });

        if (endIndex < all.size()) {
            set(53, new ItemBuilder(Material.ARROW)
                    .name("&eNächste Seite »")
                    .build(), new ClickAction() {
                @Override
                public void execute(InventoryClickEvent e) {
                    openLater(new GravesActiveMenu(plugin, player, graveManager, parent, page + 1));
                }
            });
        }
    }

    @Override
    protected void buildFrame() {
        ItemStack frame = XGraveVersion.createPane(7, "GRAY_STAINED_GLASS_PANE");
        ItemStack pane = new ItemBuilder(frame).name(" ").build();
        for (int i = 0; i < 9; i++) set(i, pane);
        for (int i = 45; i < 54; i++) set(i, pane);
        for (int row = 1; row < 5; row++) {
            set(row * 9, pane);
            set(row * 9 + 8, pane);
        }
    }
}
