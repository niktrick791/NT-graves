package de.niktr.graves.gui;

import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.manager.GraveManager;
import de.niktr.graves.util.ItemBuilder;
import de.niktr.graves.util.Menu;
import de.niktr.graves.util.XGraveVersion;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

public class GravesSettingsMenu extends Menu<Plugin> {

    private final GraveManager graveManager;

    public GravesSettingsMenu(Plugin plugin, Player player, GraveManager graveManager, Menu<?> parent) {
        super(plugin, player, parent);
        this.graveManager = graveManager;
    }

    @Override
    public String title() {
        return "&bniktrick-graves &8• &3Einstellungen";
    }

    @Override
    public int size() {
        return 36;
    }

    @Override
    protected void build() {
        buildFrame();
        final GravesConfig cfg = graveManager.getConfig();

        int sec = cfg.getDespawnTimeSeconds();
        String timeDisplay = sec == -1 ? "∞ (Unbegrenzt)" : (sec / 60) + "m " + (sec % 60) + "s (" + sec + "s)";

        // Slot 10: Ablaufzeit
        set(10, new ItemBuilder(XGraveVersion.CLOCK_MAT)
                .name("&6Ablaufzeit (Despawn-Timer)")
                .lore(Arrays.asList(
                        "&7Aktuell eingestellt: &e" + timeDisplay,
                        "",
                        "&a▶ Linksklick: &f+30 Sekunden",
                        "&c▶ Rechtsklick: &f-30 Sekunden",
                        "&b▶ Shift-Linksklick: &f+5 Minuten",
                        "&4▶ Shift-Rechtsklick: &f-5 Minuten",
                        "&d▶ Mittlerer Klick: &fAuf Unbegrenzt (-1) umschalten"
                ))
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                int current = cfg.getDespawnTimeSeconds();
                if (e.getClick() == ClickType.MIDDLE) {
                    cfg.setDespawnTimeSeconds(current == -1 ? 1200 : -1);
                } else if (e.isShiftClick()) {
                    if (e.isLeftClick()) {
                        cfg.setDespawnTimeSeconds(current == -1 ? 300 : current + 300);
                    } else if (e.isRightClick()) {
                        cfg.setDespawnTimeSeconds(Math.max(10, (current == -1 ? 1200 : current) - 300));
                    }
                } else if (e.isLeftClick()) {
                    cfg.setDespawnTimeSeconds(current == -1 ? 30 : current + 30);
                } else if (e.isRightClick()) {
                    cfg.setDespawnTimeSeconds(Math.max(10, (current == -1 ? 1200 : current) - 30));
                }
                cfg.saveChanges();
                playClick();
                refresh();
            }
        });

        // Slot 12: Instant Pickup
        final boolean instant = cfg.isEnableInstantPickup();
        set(12, new ItemBuilder(Material.HOPPER)
                .name("&eInstant-Pickup (Shift-Klick)")
                .lore(Arrays.asList(
                        "&7Status: " + (instant ? "&aAktiviert" : "&cDeaktiviert"),
                        "&7Erlaubt Spielern durch Sneaken + Rechtsklick",
                        "&7das gesamte Grab sofort aufzusammeln.",
                        "",
                        "&6▶ Klicke zum Umschalten"
                ))
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                cfg.setEnableInstantPickup(!instant);
                cfg.saveChanges();
                playClick();
                refresh();
            }
        });

        // Slot 14: Auto-Equip Armor
        final boolean equip = cfg.isAutoEquipArmor();
        set(14, new ItemBuilder(Material.DIAMOND_CHESTPLATE)
                .name("&eRüstung auto-ausrüsten")
                .lore(Arrays.asList(
                        "&7Status: " + (equip ? "&aAktiviert" : "&cDeaktiviert"),
                        "&7Zieht Rüstungsteile bei freiem Slot",
                        "&7beim Einsammeln direkt automatisch an.",
                        "",
                        "&6▶ Klicke zum Umschalten"
                ))
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                cfg.setAutoEquipArmor(!equip);
                cfg.saveChanges();
                playClick();
                refresh();
            }
        });

        // Slot 16: Drop Items on expire
        final boolean drop = cfg.isDropItems();
        set(16, new ItemBuilder(Material.DISPENSER)
                .name("&eItems bei Ablauf droppen")
                .lore(Arrays.asList(
                        "&7Status: " + (drop ? "&aItems droppen" : "&cItems entfernen"),
                        "&7Wenn das Grab abläuft, fallen Items",
                        "&7auf den Boden (werden trotzdem archiviert).",
                        "",
                        "&6▶ Klicke zum Umschalten"
                ))
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                cfg.setDropItems(!drop);
                cfg.saveChanges();
                playClick();
                refresh();
            }
        });

        // Slot 20: Interact only own
        final boolean ownOnly = cfg.isInteractOnlyOwn();
        Material shieldMat = XGraveVersion.safeMaterial("SHIELD", "IRON_CHESTPLATE", Material.IRON_CHESTPLATE);
        set(20, new ItemBuilder(shieldMat)
                .name("&eNur eigenes Grab öffnen")
                .lore(Arrays.asList(
                        "&7Status: " + (ownOnly ? "&aNur Besitzer" : "&bJeder Spieler"),
                        "&7Verhindert, dass fremde Spieler das",
                        "&7Grab vor Ablauf looten können.",
                        "",
                        "&6▶ Klicke zum Umschalten"
                ))
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                cfg.setInteractOnlyOwn(!ownOnly);
                cfg.saveChanges();
                playClick();
                refresh();
            }
        });

        // Slot 22: Auto Rotation
        final boolean rot = cfg.isAutoRotationEnabled();
        set(22, new ItemBuilder(Material.COMPASS)
                .name("&eKopfdrehung (Auto-Rotation)")
                .lore(Arrays.asList(
                        "&7Status: " + (rot ? "&aAktiviert" : "&cDeaktiviert"),
                        "&7Lässt den Totenkopf kontinuierlich",
                        "&7in der Welt rotieren.",
                        "",
                        "&6▶ Klicke zum Umschalten"
                ))
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                cfg.setAutoRotationEnabled(!rot);
                cfg.saveChanges();
                playClick();
                refresh();
            }
        });

        // Slot 24: Despawn when empty
        final boolean emptyDespawn = cfg.isDespawnWhenEmpty();
        Material barrierMat = XGraveVersion.safeMaterial("BARRIER", "REDSTONE_BLOCK", Material.REDSTONE_BLOCK);
        set(24, new ItemBuilder(barrierMat)
                .name("&eDespawn bei leerem Grab")
                .lore(Arrays.asList(
                        "&7Status: " + (emptyDespawn ? "&aAktiviert" : "&cDeaktiviert"),
                        "&7Löscht das Grab sofort, sobald alle",
                        "&7Items herausgenommen wurden.",
                        "",
                        "&6▶ Klicke zum Umschalten"
                ))
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                cfg.setDespawnWhenEmpty(!emptyDespawn);
                cfg.saveChanges();
                playClick();
                refresh();
            }
        });

        // Slot 30: Zurück
        set(30, new ItemBuilder(Material.ARROW)
                .name("&cZurück zum Menü")
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                if (parent != null) parent.open();
                else player.closeInventory();
            }
        });

        // Slot 32: Speichern Bestätigung
        set(32, new ItemBuilder(Material.EMERALD)
                .name("&aEinstellungen gespeichert")
                .lore(Arrays.asList(
                        "&7Alle Änderungen wurden bereits",
                        "&7in der config.yml gesichert."
                ))
                .build(), new ClickAction() {
            @Override
            public void execute(InventoryClickEvent e) {
                cfg.saveChanges();
                XGraveVersion.playSound(player, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP", 1.0f, 1.2f);
                player.sendMessage(GravesConfig.parseColor(cfg.getPrefixRaw() + "&#33FF33Alle Gräber-Einstellungen erfolgreich gespeichert!"));
            }
        });
    }

    private void playClick() {
        XGraveVersion.playSound(player, "UI_BUTTON_CLICK", "CLICK", 0.7f, 1.2f);
    }

    @Override
    protected void buildFrame() {
        ItemStack frame = XGraveVersion.createPane(7, "GRAY_STAINED_GLASS_PANE");
        ItemStack pane = new ItemBuilder(frame).name(" ").build();
        for (int i = 0; i < 9; i++) set(i, pane);
        for (int i = 27; i < 36; i++) set(i, pane);
        for (int row = 1; row < 3; row++) {
            set(row * 9, pane);
            set(row * 9 + 8, pane);
        }
    }
}
