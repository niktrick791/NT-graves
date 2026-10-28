package de.niktr.graves.commands;

import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.gui.GravesAdminMenu;
import de.niktr.graves.gui.GravesArchiveMenu;
import de.niktr.graves.manager.GraveManager;
import de.niktr.graves.model.Grave;
import de.niktr.graves.storage.ArchivedGraveData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GravesCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final GraveManager graveManager;
    private final GravesConfig config;

    public GravesCommand(Plugin plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
        this.config = graveManager.getConfig();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                List<Grave> graves = graveManager.getGravesByPlayer(player.getUniqueId());
                if (!graves.isEmpty()) {
                    handleList(sender, new String[]{"list"});
                    return true;
                }
            }
            sendHelp(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "claim", "open", "pickup", "einsammeln" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(GravesConfig.parseColor(config.getCmdPlayerOnly()));
                    return true;
                }
                List<Grave> graves = graveManager.getGravesByPlayer(player.getUniqueId());
                if (graves.isEmpty()) {
                    player.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#FF5555Du hast aktuell keine aktiven GrÃ¤ber."));
                    return true;
                }
                Grave g = graves.get(0);
                if (sub.equals("open")) {
                    player.openInventory(g.getGui());
                    try { player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f); } catch (Throwable ignored) {}
                } else {
                    boolean ok = graveManager.restoreActiveGraveToPlayer(g, player);
                    if (ok) {
                        player.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#33FF33Grab-Inventar erfolgreich wiederhergestellt!"));
                        try { player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f); } catch (Throwable ignored) {}
                    }
                }
                return true;
            }
            case "admin", "gui", "menu" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(GravesConfig.parseColor(config.getCmdPlayerOnly()));
                    return true;
                }
                if (!hasPerm(player, "niktrick.graves.admin")) {
                    player.sendMessage(GravesConfig.parseColor(config.getCmdNoPermission()));
                    return true;
                }
                new GravesAdminMenu(plugin, player, graveManager, null).open();
                return true;
            }
            case "history", "archive", "archiv" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(GravesConfig.parseColor(config.getCmdPlayerOnly()));
                    return true;
                }
                if (!hasPerm(player, "niktrick.graves.admin")) {
                    player.sendMessage(GravesConfig.parseColor(config.getCmdNoPermission()));
                    return true;
                }
                new GravesArchiveMenu(plugin, player, graveManager, null, 0).open();
                return true;
            }
            case "restore", "wiederherstellen" -> {
                handleRestore(sender, args);
                return true;
            }
            case "reload" -> {
                if (!hasPerm(sender, "niktrick.graves.admin", "niktrick.graves.reload")) {
                    sender.sendMessage(GravesConfig.parseColor(config.getCmdNoPermission()));
                    return true;
                }
                config.load();
                sender.sendMessage(GravesConfig.parseColor(config.getReloadSuccess()));
                return true;
            }
            case "list" -> {
                handleList(sender, args);
                return true;
            }
            case "tp" -> {
                handleTp(sender, args);
                return true;
            }
            default -> {
                sender.sendMessage(GravesConfig.parseColor(config.getCmdInvalidCommand()));
                return true;
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(GravesConfig.parseColor("<gradient:#FF5555:#FFAA00><b>BorderMC GrÃ¤ber</b></gradient> <#FFAA00>Hilfe"));
        sender.sendMessage(GravesConfig.parseColor(" <#FFAA00>âž¤ <white>/graves list <#DDDDDD>| <#FFAAFF>Listet deine GrÃ¤ber auf"));
        sender.sendMessage(GravesConfig.parseColor(" <#FFAA00>âž¤ <white>/graves claim <#DDDDDD>| <#FFAAFF>Sammelt dein Grab-Inventar ein"));
        sender.sendMessage(GravesConfig.parseColor(" <#FFAA00>âž¤ <white>/graves open <#DDDDDD>| <#FFAAFF>Ã–ffnet dein Grab-Inventar"));
        sender.sendMessage(GravesConfig.parseColor(" <#FFAA00>âž¤ <white>/graves tp <#DDDDDD>| <#FFAAFF>Teleportiert zum letzten Grab"));
        if (hasPerm(sender, "niktrick.graves.admin")) {
            sender.sendMessage(GravesConfig.parseColor(" <#FFAA00>âž¤ <white>/graves admin <#DDDDDD>| <#FFAAFF>Admin-MenÃ¼ & GrÃ¤ber-Einstellungen"));
            sender.sendMessage(GravesConfig.parseColor(" <#FFAA00>âž¤ <white>/graves history <#DDDDDD>| <#FFAAFF>Archiv aller abgelaufenen GrÃ¤ber"));
            sender.sendMessage(GravesConfig.parseColor(" <#FFAA00>âž¤ <white>/graves restore <spieler> <#DDDDDD>| <#FFAAFF>Grab-Inventar wiederherstellen"));
            sender.sendMessage(GravesConfig.parseColor(" <#FFAA00>âž¤ <white>/graves reload <#DDDDDD>| <#FFAAFF>Konfiguration neu laden"));
        }
    }

    private void handleRestore(CommandSender sender, String[] args) {
        if (!hasPerm(sender, "niktrick.graves.admin", "niktrick.graves.restore")) {
            sender.sendMessage(GravesConfig.parseColor(config.getCmdNoPermission()));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#FF5555Benutzung: /graves restore <spieler|archivId>"));
            return;
        }

        String targetStr = args[1];
        Player targetPlayer = Bukkit.getPlayer(targetStr);

        // 1. Check if target player has an ACTIVE grave!
        if (targetPlayer != null && targetPlayer.isOnline()) {
            List<Grave> activeGraves = graveManager.getGravesByPlayer(targetPlayer.getUniqueId());
            if (!activeGraves.isEmpty()) {
                Grave activeGrave = activeGraves.get(0);
                boolean ok = graveManager.restoreActiveGraveToPlayer(activeGrave, targetPlayer);
                if (ok) {
                    sender.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#33FF33Aktives Grab-Inventar erfolgreich an &f" + targetPlayer.getName() + " &#33FF33wiederhergestellt!"));
                    targetPlayer.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#33FF33Dein Grab-Inventar wurde von einem Admin wiederhergestellt!"));
                    try {
                        targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    } catch (Throwable ignored) {}
                    return;
                }
            }
        }

        // 2. Check archived graves
        ArchivedGraveData targetData = null;
        if (targetPlayer != null) {
            List<ArchivedGraveData> list = graveManager.getArchiveStorage().getArchivedGravesForPlayer(targetPlayer.getUniqueId());
            if (!list.isEmpty()) {
                targetData = list.get(0); // Neuestes Grab
            }
        } else {
            targetData = graveManager.getArchiveStorage().getArchivedGrave(targetStr);
            if (targetData == null) {
                // Suche nach Offline-Spieler Namen
                for (ArchivedGraveData d : graveManager.getArchiveStorage().getArchiveList()) {
                    if (d.getPlayerName().equalsIgnoreCase(targetStr)) {
                        targetData = d;
                        break;
                    }
                }
            }
        }

        if (targetData == null) {
            sender.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#FF5555Kein aktives oder archiviertes Grab fÃ¼r '" + targetStr + "' gefunden!"));
            return;
        }

        if (targetPlayer != null && targetPlayer.isOnline()) {
            boolean ok = graveManager.getArchiveStorage().restoreGraveToPlayer(targetData, targetPlayer);
            if (ok) {
                sender.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#33FF33Grab-Inventar erfolgreich an &f" + targetPlayer.getName() + " &#33FF33wiederhergestellt!"));
                targetPlayer.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#33FF33Dein abgelaufenes Grab-Inventar wurde von einem Admin wiederhergestellt!"));
                try {
                    targetPlayer.playSound(targetPlayer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                } catch (Throwable ignored) {}
            } else {
                sender.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#FF3333Fehler bei der Wiederherstellung!"));
            }
        } else {
            sender.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#FFAA00Spieler &f" + targetData.getPlayerName() + " &#FFAA00ist offline! Nutze &e/graves history&#FFAA00, um das Inventar einzusehen."));
        }
    }

    private void handleList(CommandSender sender, String[] args) {
        UUID targetUuid;
        String targetName;

        if (args.length > 1) {
            if (!hasPerm(sender, "niktrick.graves.admin")) {
                sender.sendMessage(GravesConfig.parseColor(config.getCmdNoPermission()));
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target != null) {
                targetUuid = target.getUniqueId();
                targetName = target.getName();
            } else {
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                if (op.hasPlayedBefore() || op.isOnline()) {
                    targetUuid = op.getUniqueId();
                    targetName = op.getName() != null ? op.getName() : args[1];
                } else {
                    sender.sendMessage(GravesConfig.parseColor(config.getCmdInvalidPlayer().replace("%player%", args[1])));
                    return;
                }
            }
        } else {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(GravesConfig.parseColor(config.getCmdPlayerOnly()));
                return;
            }
            if (!hasPerm(sender, "niktrick.graves.list")) {
                sender.sendMessage(GravesConfig.parseColor(config.getCmdNoPermission()));
                return;
            }
            targetUuid = player.getUniqueId();
            targetName = player.getName();
        }

        List<Grave> graves = graveManager.getGravesByPlayer(targetUuid);
        if (graves.isEmpty()) {
            sender.sendMessage(GravesConfig.parseColor(config.getGraveListNoGraves()));
            return;
        }

        sender.sendMessage(GravesConfig.parseColor(config.getGraveListHeader()));
        for (Grave g : graves) {
            Location loc = g.getLocation();
            String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "world";
            String worldDisplay = config.getWorldDisplayName(worldName);
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            long elapsedSec = (System.currentTimeMillis() - g.getSpawnedTime()) / 1000L;
            String timeStr = formatElapsed(elapsedSec);

            String tpCmd = "/graves tp " + worldName + " " + x + " " + y + " " + z;

            Component claimBtn = GravesConfig.parseColor("&#33FF33[Claim] ")
                    .clickEvent(ClickEvent.runCommand("/graves claim"))
                    .hoverEvent(HoverEvent.showText(Component.text("Klicke hier, um alle GegenstÃ¤nde sofort einzusammeln", NamedTextColor.GREEN)));

            Component openBtn = GravesConfig.parseColor("&#FFAA00[Ã–ffnen] ")
                    .clickEvent(ClickEvent.runCommand("/graves open"))
                    .hoverEvent(HoverEvent.showText(Component.text("Klicke hier, um das Grab-Inventar zu Ã¶ffnen", NamedTextColor.GOLD)));

            Component tpBtn = GravesConfig.parseColor("&#FF5555[TP] ")
                    .clickEvent(ClickEvent.runCommand(tpCmd))
                    .hoverEvent(HoverEvent.showText(Component.text("Klicke hier, um dich zum Grab zu teleportieren (" + worldDisplay + " " + x + ", " + y + ", " + z + ")", NamedTextColor.LIGHT_PURPLE)));

            String lineText = config.getGraveListFormat()
                    .replace("[TP] ", "")
                    .replace("[TP]", "")
                    .replace("%player%", targetName)
                    .replace("%world%", worldDisplay)
                    .replace("%x%", String.valueOf(x))
                    .replace("%y%", String.valueOf(y))
                    .replace("%z%", String.valueOf(z))
                    .replace("%time%", timeStr);

            Component textComp = GravesConfig.parseColor(lineText);
            sender.sendMessage(claimBtn.append(openBtn).append(tpBtn).append(textComp));
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GravesConfig.parseColor(config.getCmdPlayerOnly()));
            return;
        }

        if (!hasPerm(player, "niktrick.graves.tp")) {
            player.sendMessage(GravesConfig.parseColor(config.getCmdNoPermission()));
            return;
        }

        if (args.length >= 5) {
            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                player.sendMessage(GravesConfig.parseColor(config.getCmdInvalidValue().replace("%value%", args[1])));
                return;
            }
            try {
                double x = Double.parseDouble(args[2]);
                double y = Double.parseDouble(args[3]);
                double z = Double.parseDouble(args[4]);
                Location target = new Location(world, x + 0.5, y + 0.1, z + 0.5, player.getLocation().getYaw(), player.getLocation().getPitch());
                player.teleportAsync(target);
                player.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#FFAAFFTeleportiert zu &f" + config.getWorldDisplayName(world.getName()) + " " + (int) x + ", " + (int) y + ", " + (int) z));
            } catch (Exception ex) {
                player.sendMessage(GravesConfig.parseColor(config.getCmdInvalidCommand()));
            }
            return;
        }

        List<Grave> graves = graveManager.getGravesByPlayer(player.getUniqueId());
        if (graves.isEmpty()) {
            player.sendMessage(GravesConfig.parseColor(config.getGraveListNoGraves()));
            return;
        }

        Grave latest = graves.get(0);
        Location loc = latest.getLocation().clone().add(0.5, 0.1, 0.5);
        player.teleportAsync(loc);
        player.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#FFAAFFTeleportiert zu deinem letzten Grab!"));
    }

    private String formatElapsed(long sec) {
        if (sec < 60) return sec + "s ago";
        long min = sec / 60;
        if (min < 60) return min + "m ago";
        long h = min / 60;
        return h + "h ago";
    }

    private boolean hasPerm(CommandSender sender, String... perms) {
        if (sender.isOp() || sender.hasPermission("bordermc.admin")) return true;
        for (String p : perms) {
            if (sender.hasPermission(p)) return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(List.of("help", "list", "claim", "open", "tp"));
            if (hasPerm(sender, "niktrick.graves.admin")) {
                list.add("admin");
                list.add("history");
                list.add("restore");
                list.add("reload");
            }
            return filterPrefix(list, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("restore"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return filterPrefix(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(Collection<String> list, String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String s : list) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(s);
            }
        }
        return result;
    }
}

