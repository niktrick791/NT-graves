package de.niktr.graves.gui;

import de.niktr.graves.util.Menu;
import de.niktr.graves.util.ItemBuilder;
import de.niktr.graves.manager.GraveManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class GravesAdminMenu extends Menu<Plugin> {

    private final GraveManager graveManager;

    public GravesAdminMenu(Plugin plugin, Player player, GraveManager graveManager, Menu<?> parent) {
        super(plugin, player, parent);
        this.graveManager = graveManager;
    }

    @Override
    public Component title() {
        return Component.text("BorderMC \u2022 Gr\u00e4ber Administration", NamedTextColor.GOLD);
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

        // Slot 11: Aktive Grï¿½ber
        set(11, new ItemBuilder(Material.CHEST)
                .name(Component.text("Aktive Gr\u00e4ber", NamedTextColor.YELLOW))
                .lore(List.of(
                        Component.text("Derzeit aktiv: ", NamedTextColor.GRAY)
                                .append(Component.text(activeCount + " Gr\u00e4ber", NamedTextColor.GREEN)),
                        Component.empty(),
                        Component.text("\u25b6 Klicke hier, um alle Gr\u00e4ber", NamedTextColor.GRAY),
                        Component.text("  zu inspizieren oder zu teleportieren.", NamedTextColor.GRAY)
                ))
                .build(), e -> {
            try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f); } catch (Throwable ignored) {}
            openLater(new GravesActiveMenu(plugin, player, graveManager, this, 0));
        });

        // Slot 13: Grab-Archiv & Wiederherstellung
        set(13, new ItemBuilder(Material.ENDER_CHEST)
                .name(Component.text("Grab-Archiv & Wiederherstellung", NamedTextColor.GOLD))
                .lore(List.of(
                        Component.text("Im Archiv: ", NamedTextColor.GRAY)
                                .append(Component.text(archiveCount + " abgelaufene Gr\u00e4ber", NamedTextColor.YELLOW)),
                        Component.empty(),
                        Component.text("\u25b6 Klicke hier, um abgelaufene", NamedTextColor.GRAY),
                        Component.text("  Inventare einzusehen oder Spielern", NamedTextColor.GRAY),
                        Component.text("  wiederherzustellen.", NamedTextColor.GRAY)
                ))
                .build(), e -> {
            try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f); } catch (Throwable ignored) {}
            openLater(new GravesArchiveMenu(plugin, player, graveManager, this, 0));
        });

        // Slot 15: Modul-Einstellungen & Ablaufzeit
        set(15, new ItemBuilder(Material.REPEATER)
                .name(Component.text("Einstellungen & Ablaufzeit", NamedTextColor.AQUA))
                .lore(List.of(
                        Component.text("Ablaufzeit: ", NamedTextColor.GRAY)
                                .append(Component.text(timeDisplay, NamedTextColor.WHITE)),
                        Component.text("Instant-Pickup: ", NamedTextColor.GRAY)
                                .append(Component.text(graveManager.getConfig().isEnableInstantPickup() ? "Aktiviert" : "Deaktiviert", graveManager.getConfig().isEnableInstantPickup() ? NamedTextColor.GREEN : NamedTextColor.RED)),
                        Component.text("Items bei Ablauf: ", NamedTextColor.GRAY)
                                .append(Component.text(graveManager.getConfig().isDropItems() ? "Droppen" : "L\u00f6schen", graveManager.getConfig().isDropItems() ? NamedTextColor.GREEN : NamedTextColor.RED)),
                        Component.empty(),
                        Component.text("\u25b6 Klicke hier, um Einstellungen", NamedTextColor.GRAY),
                        Component.text("  live im Spiel anzupassen.", NamedTextColor.GRAY)
                ))
                .build(), e -> {
            try { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f); } catch (Throwable ignored) {}
            openLater(new GravesSettingsMenu(plugin, player, graveManager, this));
        });
    }

    private void buildFrame() {
        ItemStack frame = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
        for (int i = 0; i < 9; i++) set(i, frame);
        for (int i = 18; i < 27; i++) set(i, frame);
        set(9, frame);
        set(17, frame);
    }
}

