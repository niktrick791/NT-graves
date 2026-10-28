package de.niktr.graves.listeners;

import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.manager.GraveManager;
import de.niktr.graves.model.Grave;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GraveInteractListener implements Listener {

    private final GraveManager graveManager;
    private final GravesConfig config;
    private final Map<UUID, Long> lastInteract = new HashMap<>();

    public GraveInteractListener(GraveManager graveManager) {
        this.graveManager = graveManager;
        this.config = graveManager.getConfig();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Grave grave = graveManager.getGraveByEntity(event.getRightClicked());
        if (grave != null) {
            event.setCancelled(true);
            handleInteraction(event.getPlayer(), grave, event.getPlayer().isSneaking());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Grave grave = graveManager.getGraveByEntity(event.getRightClicked());
        if (grave != null) {
            event.setCancelled(true);
            handleInteraction(event.getPlayer(), grave, event.getPlayer().isSneaking());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // 1. Direct block click
        if (event.getClickedBlock() != null) {
            Location loc = event.getClickedBlock().getLocation();
            Grave grave = graveManager.getGraveAt(loc);
            if (grave == null) {
                grave = graveManager.getGraveAt(loc.clone().add(0, 1, 0));
            }
            if (grave == null) {
                grave = graveManager.getGraveAt(loc.clone().add(0, -1, 0));
            }

            if (grave != null) {
                event.setCancelled(true);
                handleInteraction(player, grave, player.isSneaking());
                return;
            }
        }

        // 2. Right-click air or left-click near a grave
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Location eyeLoc = player.getEyeLocation();
            for (Grave g : graveManager.getAllGraves()) {
                if (g.isRemoved() || g.getLocation().getWorld() == null || !g.getLocation().getWorld().equals(player.getWorld())) continue;
                Location gl = g.getLocation().clone().add(0.5, 0.8, 0.5);
                double distSq = eyeLoc.distanceSquared(gl);
                if (distSq <= 16.0) { // Within 4 blocks
                    org.bukkit.util.Vector toGrave = gl.toVector().subtract(eyeLoc.toVector()).normalize();
                    double dot = eyeLoc.getDirection().normalize().dot(toGrave);
                    if (dot > 0.80 || distSq <= 4.0) { // Looking at grave or very close
                        event.setCancelled(true);
                        handleInteraction(player, g, player.isSneaking());
                        return;
                    }
                }
            }
        }
    }

    private void handleInteraction(Player player, Grave grave, boolean isSneaking) {
        if (grave == null || grave.isRemoved()) return;

        // Debounce double clicks
        long now = System.currentTimeMillis();
        Long last = lastInteract.get(player.getUniqueId());
        if (last != null && (now - last) < 250) {
            return;
        }
        lastInteract.put(player.getUniqueId(), now);

        boolean isOwner = player.getUniqueId().equals(grave.getOwnerUUID());
        boolean isAdmin = player.hasPermission("niktrick.graves.admin") || player.hasPermission("axgraves.admin");

        if (config.isInteractOnlyOwn() && !isOwner && !isAdmin) {
            player.sendMessage(GravesConfig.parseColor(config.getInteractNotYourGrave()));
            return;
        }

        // Shift-Right-Click: Instant Pickup
        if (isSneaking && config.isEnableInstantPickup()) {
            if (config.isInstantPickupOnlyOwn() && !isOwner && !isAdmin) {
                player.sendMessage(GravesConfig.parseColor(config.getInteractNotYourGrave()));
                return;
            }

            performInstantPickup(player, grave);
            return;
        }

        // Anti-dupe: do NOT open the GUI if a pickup is already in progress
        if (grave.isRemoved()) return;

        // Normal Right Click: Open Chest GUI
        player.openInventory(grave.getGui());
        try {
            player.playSound(grave.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1.0f);
        } catch (Throwable ignored) {}
    }


    private void performInstantPickup(Player player, Grave grave) {
        // Anti-dupe: atomically acquire pickup lock - only one thread/event can pick up at a time
        if (!grave.tryLockPickup()) return;

        try {
            PlayerInventory inv = player.getInventory();
            // Snapshot and immediately clear the grave's GUI to prevent any concurrent access
            ItemStack[] contents = grave.getGui().getContents().clone();
            grave.getGui().clear();

            // 0. Remove any Shulker Head on player
            if (GraveManager.isShulkerHead(inv.getHelmet())) {
                inv.setHelmet(null);
            }

            // 1. Auto equip armor
            if (config.isAutoEquipArmor()) {
                for (int i = 0; i < contents.length; i++) {
                    ItemStack item = contents[i];
                    if (item == null || item.getType().isAir()) continue;

                    if (GraveManager.isShulkerHead(item)) {
                        contents[i] = null;
                        continue;
                    }

                    String typeName = item.getType().name();
                    if ((inv.getHelmet() == null || GraveManager.isShulkerHead(inv.getHelmet())) && typeName.endsWith("_HELMET")) {
                        inv.setHelmet(item);
                        contents[i] = null;
                    } else if (inv.getChestplate() == null && (typeName.endsWith("_CHESTPLATE") || item.getType() == Material.ELYTRA)) {
                        inv.setChestplate(item);
                        contents[i] = null;
                    } else if (inv.getLeggings() == null && typeName.endsWith("_LEGGINGS")) {
                        inv.setLeggings(item);
                        contents[i] = null;
                    } else if (inv.getBoots() == null && typeName.endsWith("_BOOTS")) {
                        inv.setBoots(item);
                        contents[i] = null;
                    }
                }
            }

            // 2. Add remaining items
            for (int i = 0; i < contents.length; i++) {
                ItemStack item = contents[i];
                if (item == null || item.getType().isAir()) continue;
                if (GraveManager.isShulkerHead(item)) continue;

                HashMap<Integer, ItemStack> leftover = inv.addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
                contents[i] = null;
            }

            // 3. Award XP
            if (grave.getStoredXP() > 0) {
                player.giveExp(grave.getStoredXP());
                grave.setStoredXP(0);
            }

            try {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            } catch (Throwable ignored) {}

            // 4. Remove grave
            graveManager.removeGrave(grave, false);
        } finally {
            grave.unlockPickup();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        for (Grave grave : graveManager.getAllGraves()) {
            if (grave.isRemoved()) continue;
            if (grave.getGui().equals(event.getInventory())) {
                grave.updateHologram(config);

                if (config.isDespawnWhenEmpty() && grave.countItems() == 0) {
                    if (grave.getStoredXP() > 0) {
                        player.giveExp(grave.getStoredXP());
                        grave.setStoredXP(0);
                    }
                    try {
                        player.playSound(grave.getLocation(), Sound.BLOCK_CHEST_CLOSE, 0.8f, 1.0f);
                    } catch (Throwable ignored) {}
                    graveManager.removeGrave(grave, false);
                }
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        Grave grave = graveManager.getGraveByEntity(event.getEntity());
        if (grave != null) {
            event.setCancelled(true);
            if (event instanceof EntityDamageByEntityEvent edbe && edbe.getDamager() instanceof Player player) {
                handleInteraction(player, grave, player.isSneaking());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ())) {
            return;
        }

        Player player = event.getPlayer();
        for (Grave grave : graveManager.getGravesByPlayer(player.getUniqueId())) {
            if (grave.isRemoved() || grave.getLocation().getWorld() == null || !grave.getLocation().getWorld().equals(player.getWorld())) continue;
            Location gl = grave.getLocation();
            double dx = Math.abs(to.getX() - (gl.getX() + 0.5));
            double dz = Math.abs(to.getZ() - (gl.getZ() + 0.5));
            double dy = Math.abs(to.getY() - gl.getY());
            if (dx <= 1.25 && dz <= 1.25 && dy <= 1.8) {
                if (config.isEnableInstantPickup()) {
                    performInstantPickup(player, grave);
                } else {
                    handleInteraction(player, grave, false);
                }
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> graveManager.getGraveAt(b.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (graveManager.getGraveAt(loc) != null || graveManager.getGraveAt(loc.clone().add(0, 1, 0)) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        graveManager.onChunkLoad(event.getChunk());
    }
}

