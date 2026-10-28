package de.niktr.graves.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public abstract class Menu<P extends Plugin> implements InventoryHolder {

    public interface ClickAction {
        void execute(InventoryClickEvent event);
    }

    protected final P plugin;
    protected final Player player;
    protected final Menu<?> parent;
    protected final Map<Integer, ClickAction> actions = new HashMap<Integer, ClickAction>();
    protected Inventory inventory;

    protected Menu(P plugin, Player player) {
        this(plugin, player, null);
    }

    protected Menu(P plugin, Player player, Menu<?> parent) {
        this.plugin = plugin;
        this.player = player;
        this.parent = parent;
    }

    public P getPlugin() {
        return plugin;
    }

    public abstract String title();

    public abstract int size();

    protected abstract void build();

    public void open() {
        render();
        player.openInventory(getInventory());
    }

    public void refresh() {
        render();
    }

    private void render() {
        Inventory inv = getInventory();
        actions.clear();
        inv.clear();
        build();
    }

    public void handle(InventoryClickEvent event) {
        ClickAction action = actions.get(event.getRawSlot());
        if (action == null) return;
        action.execute(event);
    }

    public void handleDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
    }

    public void openLater(final Menu<?> menu) {
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                menu.open();
            }
        });
    }

    public void back() {
        if (parent != null) {
            openLater(parent);
        } else {
            close();
        }
    }

    public void close() {
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                player.closeInventory();
            }
        });
    }

    public void onClose(InventoryCloseEvent event) {
    }

    @Override
    public Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, size(), XGraveVersion.colorize(title()));
        }
        return inventory;
    }

    protected void set(int slot, ItemStack item) {
        if (slot >= 0 && slot < size()) {
            getInventory().setItem(slot, item);
        }
    }

    protected void set(int slot, ItemStack item, ClickAction action) {
        set(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    protected void buildFrame() {
        ItemStack filler = XGraveVersion.createPane(7, "GRAY_STAINED_GLASS_PANE");
        for (int i = 0; i < size(); i++) {
            if (i < 9 || i >= size() - 9 || i % 9 == 0 || (i + 1) % 9 == 0) {
                set(i, new ItemBuilder(filler).name(" ").build(), new ClickAction() {
                    @Override
                    public void execute(InventoryClickEvent e) {
                        e.setCancelled(true);
                    }
                });
            }
        }
    }
}
