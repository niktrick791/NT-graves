package de.niktr.graves.gui;

import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.manager.GraveManager;
import de.niktr.graves.storage.ArchivedGraveData;
import de.niktr.graves.storage.GraveStorage;
import de.niktr.graves.util.ItemBuilder;
import de.niktr.graves.util.Menu;
import de.niktr.graves.util.XGraveVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.Arrays;
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
    public String title() {
        return "&6Grab-Archiv &8• &7Seite " + (page + 1);
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
            final ArchivedGraveData data = archive.get(i);
            String dateStr = DATE_FORMAT.format(new Date(data.getDeathTime()));
            ItemStack[] items = GraveStorage.itemsFromBase64(data.getItems());
            int itemCount = 0;
            for (ItemStack it : items) {
                if (!XGraveVersion.isAir(it)) itemCount += it.getAmount();
            }

            Material icon = data.isRestored()
                    ? XGraveVersion.safeMaterial("MINECART", "MINECART", Material.MINECART)
                    : XGraveVersion.safeMaterial("CHEST_MINECART", "STORAGE_MINECART", Material.MINECART);

            ItemStack displayItem = new ItemBuilder(icon)
                    .name("&6" + data.getPlayerName() + " &8• &7" + dateStr)
                    .lore(Arrays.asList(
                            "&7Grund: &e" + data.getReason(),
                            "&7Ort: &f" + data.getLocation(),
                            "&7Items: &a" + itemCount + " Gegenstände",
                            "&7Erfahrung: &b" + data.getXp() + " XP",
                            "&7Status: " + (data.isRestored() ? "&aWiederhergestellt" : "&cNicht wiederhergestellt"),
                            "",
                            "&a▶ Linksklick: &aDem Spieler wiederherstellen",
                            "&e▶ Rechtsklick: &eInventar ansehen & Items nehmen"
                    ))
                    .build();

            set(slot, displayItem, new ClickAction() {
                @Override
                public void execute(InventoryClickEvent e) {
                    if (e.isRightClick()) {
                        openArchivedInventory(data);
                    } else {
                        Player target = Bukkit.getPlayer(data.getPlayerName());
                        if (target != null && target.isOnline()) {
                            boolean ok = graveManager.getArchiveStorage().restoreGraveToPlayer(data, target);
                            if (ok) {
                                player.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#33FF33Inventar erfolgreich an &f" + target.getName() + " &#33FF33wiederhergestellt!"));
                                target.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#33FF33Ein Admin hat dein abgelaufenes Grab-Inventar wiederhergestellt!"));
                                XGraveVersion.playSound(player, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP", 1.0f, 1.0f);
                                XGraveVersion.playSound(target, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP", 1.0f, 1.0f);
                                refresh();
                            } else {
                                player.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#FF3333Fehler bei der Wiederherstellung!"));
                            }
                        } else {
                            player.sendMessage(GravesConfig.parseColor(graveManager.getConfig().getPrefixRaw() + "&#FFAA00Spieler &f" + data.getPlayerName() + " &#FFAA00ist aktuell offline! Klicke mit Rechtsklick, um die Items anzusehen."));
                        }
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
                    openLater(new GravesArchiveMenu(plugin, player, graveManager, parent, page - 1));
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

        if (endIndex < archive.size()) {
            set(53, new ItemBuilder(Material.ARROW)
                    .name("&eNächste Seite »")
                    .build(), new ClickAction() {
                @Override
                public void execute(InventoryClickEvent e) {
                    openLater(new GravesArchiveMenu(plugin, player, graveManager, parent, page + 1));
                }
            });
        }
    }

    private void openArchivedInventory(ArchivedGraveData data) {
        String title = "Archiv • " + data.getPlayerName();
        if (title.length() > 32) title = title.substring(0, 32);

        Inventory inv = Bukkit.createInventory(null, 54, XGraveVersion.colorize("&6" + title));
        ItemStack[] items = GraveStorage.itemsFromBase64(data.getItems());
        for (int i = 0; i < Math.min(items.length, 54); i++) {
            if (!XGraveVersion.isAir(items[i])) {
                inv.setItem(i, items[i].clone());
            }
        }
        player.openInventory(inv);
        XGraveVersion.playSound(player, "BLOCK_CHEST_OPEN", "CHEST_OPEN", 0.8f, 1.0f);
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
