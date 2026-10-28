package de.niktr.graves.gui;

import de.niktr.graves.util.Menu;
import de.niktr.graves.util.ItemBuilder;
import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.manager.GraveManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class GravesSettingsMenu extends Menu<Plugin> {

    private final GraveManager graveManager;

    public GravesSettingsMenu(Plugin plugin, Player player, GraveManager graveManager, Menu<?> parent) {
        super(plugin, player, parent);
        this.graveManager = graveManager;
    }

    @Override
    public Component title() {
        return Component.text("BorderMC \u2022 Gr\u00e4ber Einstellungen", NamedTextColor.AQUA);
    }

    @Override
    public int size() {
        return 36;
    }

    @Override
    protected void build() {
        buildFrame();
        GravesConfig cfg = graveManager.getConfig();

        int sec = cfg.getDespawnTimeSeconds();
        String timeDisplay = sec == -1 ? "\u221e (Unbegrenzt)" : (sec / 60) + "m " + (sec % 60) + "s (" + sec + "s)";

        // Slot 10: Ablaufzeit
        set(10, new ItemBuilder(Material.CLOCK)
                .name(Component.text("Ablaufzeit (Despawn-Timer)", NamedTextColor.GOLD))
                .lore(List.of(
                        Component.text("Aktuell eingestellt: ", NamedTextColor.GRAY)
                                .append(Component.text(timeDisplay, NamedTextColor.YELLOW)),
                        Component.empty(),
                        Component.text("\u25b6 Linksklick: ", NamedTextColor.GREEN)
                                .append(Component.text("+30 Sekunden", NamedTextColor.WHITE)),
                        Component.text("\u25b6 Rechtsklick: ", NamedTextColor.RED)
                                .append(Component.text("-30 Sekunden", NamedTextColor.WHITE)),
                        Component.text("\u25b6 Shift-Linksklick: ", NamedTextColor.AQUA)
                                .append(Component.text("+5 Minuten", NamedTextColor.WHITE)),
                        Component.text("\u25b6 Shift-Rechtsklick: ", NamedTextColor.DARK_RED)
                                .append(Component.text("-5 Minuten", NamedTextColor.WHITE)),
                        Component.text("\u25b6 Mittlerer Klick: ", NamedTextColor.LIGHT_PURPLE)
                                .append(Component.text("Auf Unbegrenzt (-1) umschalten", NamedTextColor.WHITE))
                ))
                .build(), e -> {
            int current = cfg.getDespawnTimeSeconds();
            if (e.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
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
        });

        // Slot 12: Instant Pickup
        boolean instant = cfg.isEnableInstantPickup();
        set(12, new ItemBuilder(Material.HOPPER)
                .name(Component.text("Instant-Pickup (Shift-Klick)", NamedTextColor.YELLOW))
                .lore(List.of(
                        Component.text("Status: ", NamedTextColor.GRAY)
                                .append(instant ? Component.text("Aktiviert", NamedTextColor.GREEN) : Component.text("Deaktiviert", NamedTextColor.RED)),
                        Component.text("Erlaubt Spielern durch Sneaken + Rechtsklick", NamedTextColor.GRAY),
                        Component.text("das gesamte Grab sofort aufzusammeln.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("\u25b6 Klicke zum Umschalten", NamedTextColor.GOLD)
                ))
                .build(), e -> {
            cfg.setEnableInstantPickup(!instant);
            cfg.saveChanges();
            playClick();
            refresh();
        });

        // Slot 14: Auto-Equip Armor
        boolean equip = cfg.isAutoEquipArmor();
        set(14, new ItemBuilder(Material.DIAMOND_CHESTPLATE)
                .name(Component.text("R\u00fcstung auto-ausr\u00fcsten", NamedTextColor.YELLOW))
                .lore(List.of(
                        Component.text("Status: ", NamedTextColor.GRAY)
                                .append(equip ? Component.text("Aktiviert", NamedTextColor.GREEN) : Component.text("Deaktiviert", NamedTextColor.RED)),
                        Component.text("Zieht R\u00fcstungsteile bei freiem Slot", NamedTextColor.GRAY),
                        Component.text("beim Einsammeln direkt automatisch an.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("\u25b6 Klicke zum Umschalten", NamedTextColor.GOLD)
                ))
                .build(), e -> {
            cfg.setAutoEquipArmor(!equip);
            cfg.saveChanges();
            playClick();
            refresh();
        });

        // Slot 16: Drop Items on expire
        boolean drop = cfg.isDropItems();
        set(16, new ItemBuilder(Material.DISPENSER)
                .name(Component.text("Items bei Ablauf droppen", NamedTextColor.YELLOW))
                .lore(List.of(
                        Component.text("Status: ", NamedTextColor.GRAY)
                                .append(drop ? Component.text("Items droppen", NamedTextColor.GREEN) : Component.text("Items entfernen", NamedTextColor.RED)),
                        Component.text("Wenn das Grab abl\u00e4uft, fallen Items", NamedTextColor.GRAY),
                        Component.text("auf den Boden (werden trotzdem archiviert).", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("\u25b6 Klicke zum Umschalten", NamedTextColor.GOLD)
                ))
                .build(), e -> {
            cfg.setDropItems(!drop);
            cfg.saveChanges();
            playClick();
            refresh();
        });

        // Slot 20: Interact only own
        boolean ownOnly = cfg.isInteractOnlyOwn();
        set(20, new ItemBuilder(Material.SHIELD)
                .name(Component.text("Nur eigenes Grab \u00f6ffnen", NamedTextColor.YELLOW))
                .lore(List.of(
                        Component.text("Status: ", NamedTextColor.GRAY)
                                .append(ownOnly ? Component.text("Nur Besitzer", NamedTextColor.GREEN) : Component.text("Jeder Spieler", NamedTextColor.AQUA)),
                        Component.text("Verhindert, dass fremde Spieler das", NamedTextColor.GRAY),
                        Component.text("Grab vor Ablauf looten k\u00f6nnen.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("\u25b6 Klicke zum Umschalten", NamedTextColor.GOLD)
                ))
                .build(), e -> {
            cfg.setInteractOnlyOwn(!ownOnly);
            cfg.saveChanges();
            playClick();
            refresh();
        });

        // Slot 22: Auto Rotation
        boolean rot = cfg.isAutoRotationEnabled();
        set(22, new ItemBuilder(Material.COMPASS)
                .name(Component.text("Kopfdrehung (Auto-Rotation)", NamedTextColor.YELLOW))
                .lore(List.of(
                        Component.text("Status: ", NamedTextColor.GRAY)
                                .append(rot ? Component.text("Aktiviert", NamedTextColor.GREEN) : Component.text("Deaktiviert", NamedTextColor.RED)),
                        Component.text("L\u00e4sst den Totenkopf kontinuierlich", NamedTextColor.GRAY),
                        Component.text("in der Welt rotieren.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("\u25b6 Klicke zum Umschalten", NamedTextColor.GOLD)
                ))
                .build(), e -> {
            cfg.setAutoRotationEnabled(!rot);
            cfg.saveChanges();
            playClick();
            refresh();
        });

        // Slot 24: Despawn when empty
        boolean emptyDespawn = cfg.isDespawnWhenEmpty();
        set(24, new ItemBuilder(Material.BARRIER)
                .name(Component.text("Despawn bei leerem Grab", NamedTextColor.YELLOW))
                .lore(List.of(
                        Component.text("Status: ", NamedTextColor.GRAY)
                                .append(emptyDespawn ? Component.text("Aktiviert", NamedTextColor.GREEN) : Component.text("Deaktiviert", NamedTextColor.RED)),
                        Component.text("L\u00f6scht das Grab sofort, sobald alle", NamedTextColor.GRAY),
                        Component.text("Items herausgenommen wurden.", NamedTextColor.GRAY),
                        Component.empty(),
                        Component.text("\u25b6 Klicke zum Umschalten", NamedTextColor.GOLD)
                ))
                .build(), e -> {
            cfg.setDespawnWhenEmpty(!emptyDespawn);
            cfg.saveChanges();
            playClick();
            refresh();
        });

        // Slot 30: Zurï¿½ck
        set(30, new ItemBuilder(Material.ARROW)
                .name(Component.text("Zur\u00fcck zum Men\u00fc", NamedTextColor.RED))
                .build(), e -> {
            if (parent != null) parent.open();
            else player.closeInventory();
        });

        // Slot 32: Speichern Bestï¿½tigung
        set(32, new ItemBuilder(Material.EMERALD)
                .name(Component.text("Einstellungen gespeichert", NamedTextColor.GREEN))
                .lore(List.of(
                        Component.text("Alle \u00c4nderungen wurden bereits", NamedTextColor.GRAY),
                        Component.text("in der config.yml gesichert.", NamedTextColor.GRAY)
                ))
                .build(), e -> {
            cfg.saveChanges();
            try { player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f); } catch (Throwable ignored) {}
            player.sendMessage(GravesConfig.parseColor(cfg.getPrefixRaw() + "&#33FF33Alle Gr\u00e4ber-Einstellungen erfolgreich gespeichert!"));
        });
    }

    private void playClick() {
        try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f); } catch (Throwable ignored) {}
    }

    private void buildFrame() {
        ItemStack frame = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < 9; i++) set(i, frame);
        for (int i = 27; i < 36; i++) set(i, frame);
        for (int row = 1; row < 3; row++) {
            set(row * 9, frame);
            set(row * 9 + 8, frame);
        }
    }
}

