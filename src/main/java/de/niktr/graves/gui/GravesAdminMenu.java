package de.niktr.graves.gui;

import de.niktr.graves.util.Menu;
import de.niktr.graves.util.ItemBuilder;
import de.niktr.graves.util.XGraveVersion;
import de.niktr.graves.manager.GraveManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

public class GravesAdminMenu extends Menu<Plugin> {

    private final GraveManager graveManager;

    public GravesAdminMenu(Plugin plugin, Player player, GraveManager graveManager, Menu<?> parent) {
        super(plugin, player, parent);
        this.graveManager = graveManager;
    }

    @Override
    public String title() {
        return "&6niktrick-graves &8• &eAdministration";
    }

    @Override
    public int size() {
        return 27;
    }

    @Override
    protected void build() {
        buildFrame();

        int activeCount = graveManager.getAllGraves().size();
        int archiveCount = graveManager.getArchiveStorage().getArchiveList().size();
        int despawnSec = graveManager.getConfig().getDespawnTimeSeconds();
        String timeDisplay = despawnSec == -1 ? "Unbegrenzt" : (despawnSec / 60) + "m " + (despawnSec % 60) + "s";

        // Slot 11: Aktive Gräber
        set(11, new ItemBuilder(Material.CHEST)
                .name("&eAktive Gräber")
                .lore(Arrays.asList(
                        "&7Derzeit aktiv: &a" + activeCount + " Gräber",
                        "",
                        "&7▶ Klicke hier, um alle Gräber",
                        "&7  zu inspizieren oder zu teleportieren."
                ))
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                XGraveVersion.playSound(player, "UI_BUTTON_CLICK", "CLICK", 0.7f, 1.2f);
                openLater(new GravesActiveMenu(plugin, player, graveManager, GravesAdminMenu.this, 0));
            }
        });

        // Slot 13: Grab-Archiv & Wiederherstellung
        set(13, new ItemBuilder(Material.ENDER_CHEST)
                .name("&6Grab-Archiv & Wiederherstellung")
                .lore(Arrays.asList(
                        "&7Im Archiv: &e" + archiveCount + " abgelaufene Gräber",
                        "",
                        "&7▶ Klicke hier, um abgelaufene",
                        "&7  Inventare einzusehen oder Spielern",
                        "&7  wiederherzustellen."
                ))
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                XGraveVersion.playSound(player, "UI_BUTTON_CLICK", "CLICK", 0.7f, 1.2f);
                openLater(new GravesArchiveMenu(plugin, player, graveManager, GravesAdminMenu.this, 0));
            }
        });

        // Slot 15: Modul-Einstellungen & Ablaufzeit
        Material repeaterMat = XGraveVersion.safeMaterial("REPEATER", "DIODE", Material.REDSTONE);
        set(15, new ItemBuilder(repeaterMat)
                .name("&bEinstellungen & Ablaufzeit")
                .lore(Arrays.asList(
                        "&7Ablaufzeit: &f" + timeDisplay,
                        "&7Instant-Pickup: " + (graveManager.getConfig().isEnableInstantPickup() ? "&aAktiviert" : "&cDeaktiviert"),
                        "&7Items bei Ablauf: " + (graveManager.getConfig().isDropItems() ? "&aDroppen" : "&cLöschen"),
                        "",
                        "&7▶ Klicke hier, um Einstellungen",
                        "&7  live im Spiel anzupassen."
                ))
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                XGraveVersion.playSound(player, "UI_BUTTON_CLICK", "CLICK", 0.7f, 1.2f);
                openLater(new GravesSettingsMenu(plugin, player, graveManager, GravesAdminMenu.this));
            }
        });
    }

    @Override
    protected void buildFrame() {
        ItemStack frame = XGraveVersion.createPane(7, "GRAY_STAINED_GLASS_PANE");
        ItemStack pane = new ItemBuilder(frame).name(" ").build();
        for (int i = 0; i < 9; i++) set(i, pane);
        for (int i = 18; i < 27; i++) set(i, pane);
        set(9, pane);
        set(17, pane);
    }
}
