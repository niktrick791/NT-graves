package de.niktr.graves.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Menu<P extends Plugin> implements InventoryHolder {

    protected final P plugin;
    protected final Player player;
    protected final Menu<?> parent;
    protected final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
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

    public abstract Component title();

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
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action == null) return;
        action.accept(event);
    }

    public void handleDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
    }

    public void openLater(Menu<?> menu) {
        Bukkit.getScheduler().runTask(plugin, menu::open);
    }

    public void back() {
        if (parent != null) {
            openLater(parent);
        } else {
            close();
        }
    }

    public void close() {
        Bukkit.getScheduler().runTask(plugin, () -> player.closeInventory());
    }

    public void onClose(InventoryCloseEvent event) {
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, size(), title());
        }
        return inventory;
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        if (slot < 0 || slot >= size()) return;
        getInventory().setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        } else {
            actions.remove(slot);
        }
    }

    protected ItemStack filler() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(Component.empty()).build();
    }

    protected void fillEmpty() {
        Inventory inv = getInventory();
        ItemStack filler = filler();
        for (int slot = 0; slot < inv.getSize(); slot++) {
            if (inv.getItem(slot) == null) inv.setItem(slot, filler);
        }
    }

    protected ItemStack backItem() {
        return new ItemBuilder(Material.ARROW).name(Component.text("« Zurück", NamedTextColor.YELLOW)).build();
    }

    protected ItemStack closeItem() {
        return new ItemBuilder(Material.BARRIER).name(Component.text("✕ Schließen", NamedTextColor.RED)).build();
    }

    protected void setBack(int slot) {
        set(slot, backItem(), event -> back());
    }

    protected void setClose(int slot) {
        set(slot, closeItem(), event -> close());
    }
}
