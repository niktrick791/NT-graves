package de.niktr.graves.listeners;

import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.manager.GraveManager;
import de.niktr.graves.model.Grave;
import de.niktr.graves.util.XGraveVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

public class GraveDeathListener implements Listener {

    private final GraveManager graveManager;
    private final GravesConfig config;

    public GraveDeathListener(GraveManager graveManager) {
        this.graveManager = graveManager;
        this.config = graveManager.getConfig();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        World world = player.getWorld();

        // 1. Check disabled worlds
        if (config.getDisabledWorlds().contains(world.getName())) {
            return;
        }

        // 2. Check permission
        if (!player.hasPermission("axgraves.allowgraves") && !player.hasPermission("niktrick.graves.use")) {
            return;
        }

        // 3. Check damage cause
        if (player.getLastDamageCause() != null) {
            String cause = player.getLastDamageCause().getCause().name();
            if (config.getBlacklistedDeathCauses().contains(cause)) {
                return;
            }
        }

        // 4. Calculate safe location
        Location loc = findSafeLocation(player);

        // 5. Collect items
        List<ItemStack> graveItems = new ArrayList<>();
        boolean storeItems = config.isStoreItems();
        boolean keepInventory = event.getKeepInventory();
        boolean shouldCollectItems = storeItems && (!keepInventory || config.isOverrideKeepInventory());

        if (shouldCollectItems) {
            PlayerInventory inv = player.getInventory();
            List<String> order = config.getGraveItemOrder();
            ItemStack[] contents = inv.getContents();
            boolean[] consumed = new boolean[contents.length];

            // 1. Process preferred ordering
            for (String part : order) {
                if ("ARMOR".equalsIgnoreCase(part)) {
                    // Armor slots in player inventory: 36 (boots), 37 (leggings), 38 (chestplate), 39 (helmet)
                    for (int s = 36; s <= 39 && s < contents.length; s++) {
                        if (!consumed[s] && isValidItem(contents[s])) {
                            graveItems.add(contents[s].clone());
                            consumed[s] = true;
                        }
                    }
                } else if ("HAND".equalsIgnoreCase(part)) {
                    int handSlot = inv.getHeldItemSlot();
                    if (handSlot >= 0 && handSlot < contents.length && !consumed[handSlot] && isValidItem(contents[handSlot])) {
                        graveItems.add(contents[handSlot].clone());
                        consumed[handSlot] = true;
                    }
                } else if ("OFFHAND".equalsIgnoreCase(part)) {
                    int offSlot = 40;
                    if (offSlot < contents.length && !consumed[offSlot] && isValidItem(contents[offSlot])) {
                        graveItems.add(contents[offSlot].clone());
                        consumed[offSlot] = true;
                    }
                }
            }

            // 2. Add all remaining items from inventory (visited strictly once)
            for (int s = 0; s < contents.length; s++) {
                if (!consumed[s] && isValidItem(contents[s])) {
                    graveItems.add(contents[s].clone());
                    consumed[s] = true;
                }
            }

            // 3. Fallback to event.getDrops() only if inventory contents were completely empty
            if (graveItems.isEmpty()) {
                for (ItemStack is : event.getDrops()) {
                    if (isValidItem(is)) {
                        graveItems.add(is.clone());
                    }
                }
            }

            // Filter out any accidentally picked up grave skull / shulker head
            graveItems.removeIf(is -> is.getType() == XGraveVersion.SKULL_MAT &&
                    is.hasItemMeta() &&
                    is.getItemMeta().hasDisplayName() &&
                    is.getItemMeta().getDisplayName().contains("Shulker Head"));

            event.getDrops().clear();
            inv.clear();
        }

        // 6. Collect XP
        int storedXP = 0;
        boolean storeXP = config.isStoreXP();
        boolean keepLevel = event.getKeepLevel();
        boolean shouldCollectXP = storeXP && (!keepLevel || config.isOverrideKeepLevel());

        if (shouldCollectXP) {
            int totalExp = calculateTotalExperience(player);
            storedXP = Math.max(0, Math.round(totalExp * config.getXpKeepPercentage()));
            event.setDroppedExp(0);

            if (keepLevel && config.isOverrideKeepLevel()) {
                player.setLevel(0);
                player.setExp(0);
                player.setTotalExperience(0);
            }
        }

        // 7. If no items and no XP, do not create grave if despawnWhenEmpty is true
        if (graveItems.isEmpty() && storedXP <= 0 && config.isDespawnWhenEmpty()) {
            return;
        }

        // 8. Spawn grave
        Grave grave = graveManager.createGrave(player, loc, graveItems, storedXP);

        // 9. Death message
        if (config.isDeathMessageEnabled()) {
            String msg = config.getDeathMessage()
                    .replace("%world%", config.getWorldDisplayName(world.getName()))
                    .replace("%x%", String.valueOf(loc.getBlockX()))
                    .replace("%y%", String.valueOf(loc.getBlockY()))
                    .replace("%z%", String.valueOf(loc.getBlockZ()));
            player.sendMessage(GravesConfig.parseColor(msg));
        }
    }

    private void addArmorItems(PlayerInventory inv, List<ItemStack> graveItems, Set<ItemStack> added) {
        ItemStack helmet = inv.getHelmet();
        if (isValidItem(helmet) && added.add(helmet)) graveItems.add(helmet.clone());

        ItemStack chest = inv.getChestplate();
        if (isValidItem(chest) && added.add(chest)) graveItems.add(chest.clone());

        ItemStack leg = inv.getLeggings();
        if (isValidItem(leg) && added.add(leg)) graveItems.add(leg.clone());

        ItemStack boot = inv.getBoots();
        if (isValidItem(boot) && added.add(boot)) graveItems.add(boot.clone());
    }

    private boolean isValidItem(ItemStack item) {
        return !de.niktr.graves.util.XGraveVersion.isAir(item);
    }

    private boolean containsSimilar(List<ItemStack> list, ItemStack item) {
        for (ItemStack is : list) {
            if (is != null && is.isSimilar(item) && is.getAmount() == item.getAmount()) {
                return true;
            }
        }
        return false;
    }

    private Location findSafeLocation(Player player) {
        Location loc = player.getLocation().clone();
        World world = loc.getWorld();
        if (world == null) return loc;

        int minHeight = de.niktr.graves.util.XGraveVersion.getMinHeight(world);
        int maxHeight = world.getMaxHeight();

        // If fell below void
        if (loc.getY() < minHeight + 1) {
            loc.setY(minHeight + 1);
        } else if (loc.getY() > maxHeight - 2) {
            loc.setY(maxHeight - 2);
        }

        // Trace down to ground if suspended in air (up to 15 blocks)
        Location checkLoc = loc.clone();
        for (int i = 0; i < 15; i++) {
            Block block = checkLoc.getBlock();
            Block below = block.getRelative(org.bukkit.block.BlockFace.DOWN);
            if (!block.getType().isSolid() && below.getType().isSolid()) {
                // Found ground!
                loc.setY(checkLoc.getY());
                break;
            }
            if (checkLoc.getY() <= minHeight + 1) break;
            checkLoc.add(0, -1, 0);
        }

        return loc;
    }

    public static int calculateTotalExperience(Player player) {
        try {
            java.lang.reflect.Method m = player.getClass().getMethod("calculateTotalExperiencePoints");
            return (Integer) m.invoke(player);
        } catch (Throwable t) {
            int level = player.getLevel();
            int exp = Math.round(player.getExpToLevel() * player.getExp());
            if (level <= 16) {
                return (level * level + 6 * level) + exp;
            } else if (level <= 31) {
                return (int) (2.5 * level * level - 40.5 * level + 360) + exp;
            } else {
                return (int) (4.5 * level * level - 162.5 * level + 2220) + exp;
            }
        }
    }
}

