package de.niktr.graves.commands;

import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.gui.GravesAdminMenu;
import de.niktr.graves.gui.GravesArchiveMenu;
import de.niktr.graves.manager.GraveManager;
import de.niktr.graves.model.Grave;
import de.niktr.graves.storage.ArchivedGraveData;
import de.niktr.graves.util.XGraveVersion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
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
        if (sub.equals("claim") || sub.equals("open") || sub.equals("pickup") || sub.equals("einsammeln")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(GravesConfig.parseColor(config.getCmdPlayerOnly()));
                return true;
            }
            Player player = (Player) sender;
            List<Grave> graves = graveManager.getGravesByPlayer(player.getUniqueId());
            if (graves.isEmpty()) {
                player.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#FF5555Du hast aktuell keine aktiven Gräber."));
                return true;
            }
            Grave g = graves.get(0);
            if (sub.equals("open")) {
                player.openInventory(g.getGui());
                XGraveVersion.playSound(player, "BLOCK_CHEST_OPEN", "CHEST_OPEN", 0.8f, 1.0f);
            } else {
                boolean ok = graveManager.restoreActiveGraveToPlayer(g, player);
                if (ok) {
                    player.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#33FF33Grab-Inventar erfolgreich wiederhergestellt!"));
                    XGraveVersion.playSound(player, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP", 1.0f, 1.0f);
                }
            }
            return true;
        } else if (sub.equals("admin") || sub.equals("gui") || sub.equals("menu")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(GravesConfig.parseColor(config.getCmdPlayerOnly()));
                return true;
            }
            Player player = (Player) sender;
            if (!hasPerm(player, "niktrick.graves.admin")) {
                player.sendMessage(GravesConfig.parseColor(config.getCmdNoPermission()));
                return true;
            }
            new GravesAdminMenu(plugin, player, graveManager, null).open();
            return true;
        } else if (sub.equals("history") || sub.equals("archive") || sub.equals("archiv")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(GravesConfig.parseColor(config.getCmdPlayerOnly()));
                return true;
            }
            Player player = (Player) sender;
            if (!hasPerm(player, "niktrick.graves.admin")) {
                player.sendMessage(GravesConfig.parseColor(config.getCmdNoPermission()));
                return true;
            }
            new GravesArchiveMenu(plugin, player, graveManager, null, 0).open();
            return true;
        } else if (sub.equals("restore") || sub.equals("wiederherstellen")) {
            handleRestore(sender, args);
            return true;
        } else if (sub.equals("reload")) {
            if (!hasPerm(sender, "niktrick.graves.admin", "niktrick.graves.reload")) {
                sender.sendMessage(GravesConfig.parseColor(config.getCmdNoPermission()));
                return true;
            }
            config.load();
            sender.sendMessage(GravesConfig.parseColor(config.getReloadSuccess()));
            return true;
        } else if (sub.equals("list")) {
            handleList(sender, args);
            return true;
        } else if (sub.equals("tp")) {
            handleTp(sender, args);
            return true;
        } else {
            sender.sendMessage(GravesConfig.parseColor(config.getCmdInvalidCommand()));
            return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(GravesConfig.parseColor("&6&lniktrick-graves &8• &eHilfe"));
        sender.sendMessage(GravesConfig.parseColor(" &e➤ &f/graves list &8| &bListet deine Gräber auf"));
        sender.sendMessage(GravesConfig.parseColor(" &e➤ &f/graves claim &8| &bSammelt dein Grab-Inventar ein"));
        sender.sendMessage(GravesConfig.parseColor(" &e➤ &f/graves open &8| &bÖffnet dein Grab-Inventar"));
        sender.sendMessage(GravesConfig.parseColor(" &e➤ &f/graves tp &8| &bTeleportiert zum letzten Grab"));
        if (hasPerm(sender, "niktrick.graves.admin")) {
            sender.sendMessage(GravesConfig.parseColor(" &e➤ &f/graves admin &8| &bAdmin-Menü & Gräber-Einstellungen"));
            sender.sendMessage(GravesConfig.parseColor(" &e➤ &f/graves history &8| &bArchiv aller abgelaufenen Gräber"));
            sender.sendMessage(GravesConfig.parseColor(" &e➤ &f/graves restore <spieler> &8| &bGrab-Inventar wiederherstellen"));
            sender.sendMessage(GravesConfig.parseColor(" &e➤ &f/graves reload &8| &bKonfiguration neu laden"));
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
                    XGraveVersion.playSound(targetPlayer, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP", 1.0f, 1.0f);
                    return;
                }
            }
        }

        // 2. Check archived graves
        ArchivedGraveData targetData = null;
        if (targetPlayer != null) {
            List<ArchivedGraveData> list = graveManager.getArchiveStorage().getArchivedGravesForPlayer(targetPlayer.getUniqueId());
            if (!list.isEmpty()) {
                targetData = list.get(0);
            }
        } else {
            targetData = graveManager.getArchiveStorage().getArchivedGrave(targetStr);
            if (targetData == null) {
                for (ArchivedGraveData d : graveManager.getArchiveStorage().getArchiveList()) {
                    if (d.getPlayerName().equalsIgnoreCase(targetStr)) {
                        targetData = d;
                        break;
                    }
                }
            }
        }

        if (targetData == null) {
            sender.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#FF5555Kein aktives oder archiviertes Grab für '" + targetStr + "' gefunden!"));
            return;
        }

        if (targetPlayer != null && targetPlayer.isOnline()) {
            boolean ok = graveManager.getArchiveStorage().restoreGraveToPlayer(targetData, targetPlayer);
            if (ok) {
                sender.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#33FF33Grab-Inventar erfolgreich an &f" + targetPlayer.getName() + " &#33FF33wiederhergestellt!"));
                targetPlayer.sendMessage(GravesConfig.parseColor(config.getPrefixRaw() + "&#33FF33Dein abgelaufenes Grab-Inventar wurde von einem Admin wiederhergestellt!"));
                XGraveVersion.playSound(targetPlayer, "ENTITY_PLAYER_LEVELUP", "LEVEL_UP", 1.0f, 1.0f);
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
            if (!(sender instanceof Player)) {
                sender.sendMessage(GravesConfig.parseColor(config.getCmdPlayerOnly()));
                return;
            }
            Player player = (Player) sender;
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

            String lineText = config.getGraveListFormat()
                    .replace("[TP] ", "")
                    .replace("[TP]", "")
                    .replace("%player%", targetName)
                    .replace("%world%", worldDisplay)
                    .replace("%x%", String.valueOf(x))
                    .replace("%y%", String.valueOf(y))
                    .replace("%z%", String.valueOf(z))
                    .replace("%time%", timeStr);

            sender.sendMessage(GravesConfig.parseColor("&a[Claim] &6[Öffnen] &c[TP: " + tpCmd + "] &f" + lineText));
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(GravesConfig.parseColor(config.getCmdPlayerOnly()));
            return;
        }
        Player player = (Player) sender;

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
                player.teleport(target);
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
        player.teleport(loc);
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
        if (sender.isOp() || sender.hasPermission("bordermc.admin") || sender.hasPermission("niktrick.admin")) return true;
        for (String p : perms) {
            if (sender.hasPermission(p)) return true;
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<String>(Arrays.asList("help", "list", "claim", "open", "tp"));
            if (hasPerm(sender, "niktrick.graves.admin")) {
                list.add("admin");
                list.add("history");
                list.add("restore");
                list.add("reload");
            }
            return filterPrefix(list, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("restore"))) {
            List<String> names = new ArrayList<String>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return filterPrefix(names, args[1]);
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(Collection<String> list, String prefix) {
        List<String> result = new ArrayList<String>();
        String lower = prefix.toLowerCase(Locale.ROOT);
        for (String s : list) {
            if (s.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(s);
            }
        }
        return result;
    }
}
