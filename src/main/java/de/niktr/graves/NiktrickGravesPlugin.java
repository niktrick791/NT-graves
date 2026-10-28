package de.niktr.graves;

import de.niktr.graves.commands.GravesCommand;
import de.niktr.graves.config.GravesConfig;
import de.niktr.graves.listeners.GraveDeathListener;
import de.niktr.graves.listeners.GraveInteractListener;
import de.niktr.graves.manager.GraveManager;
import de.niktr.graves.storage.GraveArchiveStorage;
import de.niktr.graves.storage.GraveStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class NiktrickGravesPlugin extends JavaPlugin {

    private static NiktrickGravesPlugin instance;

    private GravesConfig gravesConfig;
    private GraveStorage storage;
    private GraveArchiveStorage archiveStorage;
    private GraveManager graveManager;

    public static NiktrickGravesPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.gravesConfig = new GravesConfig(this, getDataFolder());
        this.gravesConfig.load();

        this.storage = new GraveStorage(getDataFolder(), getLogger());
        this.archiveStorage = new GraveArchiveStorage(getDataFolder(), getLogger());
        this.graveManager = new GraveManager(this, gravesConfig, storage, archiveStorage, getLogger());
        this.graveManager.loadAll();

        // Register listeners
        getServer().getPluginManager().registerEvents(new GraveDeathListener(graveManager), this);
        getServer().getPluginManager().registerEvents(new GraveInteractListener(graveManager), this);

        // Register command
        GravesCommand cmd = new GravesCommand(this, graveManager);
        PluginCommand command = getCommand("graves");
        if (command != null) {
            command.setExecutor(cmd);
            command.setTabCompleter(cmd);
        }

        // Register 1-second ticker for hologram countdown & rotation
        Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                try {
                    graveManager.tick();
                } catch (Throwable t) {
                    getLogger().warning("[niktrick-graves] Fehler im Grave-Tick: " + t.getMessage());
                }
            }
        }, 20L, 20L);

        // Register auto-save task
        if (gravesConfig.isSaveGravesEnabled() && gravesConfig.getAutoSaveSeconds() > 0) {
            long saveTicks = gravesConfig.getAutoSaveSeconds() * 20L;
            Bukkit.getScheduler().runTaskTimer(this, new Runnable() {
                @Override
                public void run() {
                    try {
                        graveManager.saveAll();
                    } catch (Throwable t) {
                        getLogger().warning("[niktrick-graves] Fehler beim Auto-Save: " + t.getMessage());
                    }
                }
            }, saveTicks, saveTicks);
        }

        getLogger().info("niktrick-graves v" + getDescription().getVersion() + " successfully enabled (Cross-version 1.8 - 1.21.4+)!");
    }

    @Override
    public void onDisable() {
        if (this.graveManager != null) {
            this.graveManager.saveAll();
        }
        getLogger().info("niktrick-graves disabled.");
    }

    public GravesConfig getGravesConfig() {
        return gravesConfig;
    }

    public GraveManager getGraveManager() {
        return graveManager;
    }

    public GraveStorage getStorage() {
        return storage;
    }

    public GraveArchiveStorage getArchiveStorage() {
        return archiveStorage;
    }
}
