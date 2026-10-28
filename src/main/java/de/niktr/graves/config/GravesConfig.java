package de.niktr.graves.config;

import de.niktr.graves.util.XGraveVersion;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class GravesConfig {

    private final Plugin plugin;
    private final File configFile;
    private final File messagesFile;

    private FileConfiguration config;
    private FileConfiguration messages;

    // Config options
    private String language = "en";
    private String prefixRaw;
    private String prefixComponent;
    private int despawnTimeSeconds;
    private boolean dropItems;
    private boolean droppedItemVelocity;
    private boolean enableInstantPickup;
    private boolean instantPickupOnlyOwn;
    private boolean overrideKeepInventory;
    private boolean overrideKeepLevel;
    private double hologramHeight;
    private double headHeight;
    private boolean rotateHead360;
    private boolean autoRotationEnabled;
    private double autoRotationSpeed;
    private boolean interactOnlyOwn;
    private float xpKeepPercentage;
    private boolean storeItems;
    private boolean storeXP;
    private List<String> graveItemOrder;
    private boolean autoEquipArmor;
    private int graveLimit;
    private boolean despawnWhenEmpty;
    private boolean saveGravesEnabled;
    private int autoSaveSeconds;
    private boolean customSkullEnabled;
    private String customSkullBase64;
    private List<String> disabledWorlds;
    private List<String> blacklistedDeathCauses;
    private Map<String, String> worldNames = new HashMap<>();

    // Hologram settings
    private String hologramBgColor;
    private String hologramBillboard;
    private boolean hologramSeeThrough;
    private boolean hologramShadow;

    // Messages
    private List<String> hologramLines;
    private List<String> helpLines;
    private String guiName;
    private String unknownPlayer;
    private String reloadSuccess;
    private String reloadFailed;
    private String interactNotYourGrave;
    private boolean deathMessageEnabled;
    private String deathMessage;
    private String graveListHeader;
    private String graveListFormat;
    private String graveListNoGraves;
    private String cmdInvalidValue;
    private String cmdInvalidCommand;
    private String cmdMissingArgument;
    private String cmdNoPermission;
    private String cmdPlayerOnly;
    private String cmdInvalidPlayer;

    public GravesConfig(Plugin plugin, File dataFolder) {
        this.plugin = plugin;
        this.configFile = new File(dataFolder, "config.yml");
        this.messagesFile = new File(dataFolder, "messages.yml");
    }

    public void load() {
        if (!configFile.exists()) {
            saveResourceDefault("config.yml", configFile);
        }
        if (!messagesFile.exists()) {
            saveResourceDefault("messages.yml", messagesFile);
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        language = config.getString("language", "en").toLowerCase(Locale.ROOT);
        if (!"de".equals(language) && !"en".equals(language)) {
            language = "en";
        }
        boolean isGerman = "de".equals(language);

        // Parse config values
        prefixRaw = config.getString("prefix", isGerman
                ? "<gradient:#FF5555:#FFAA00><b>BorderMC Gr\u00e4ber</b></gradient> &7\u00bb "
                : "&8[&6niktrick-graves&8] &7");
        prefixComponent = parseColor(prefixRaw);

        despawnTimeSeconds = config.getInt("despawn-time-seconds", 1200);
        dropItems = config.getBoolean("drop-items", true);
        droppedItemVelocity = config.getBoolean("dropped-item-velocity", true);
        enableInstantPickup = config.getBoolean("enable-instant-pickup", true);
        instantPickupOnlyOwn = config.getBoolean("instant-pickup-only-own", false);
        overrideKeepInventory = config.getBoolean("override-keep-inventory", false);
        overrideKeepLevel = config.getBoolean("override-keep-level", false);
        hologramHeight = config.getDouble("hologram-height", 1.15);
        headHeight = config.getDouble("head-height", -1.2);
        rotateHead360 = config.getBoolean("rotate-head-360", true);

        autoRotationEnabled = config.getBoolean("auto-rotation.enabled", false);
        autoRotationSpeed = config.getDouble("auto-rotation.speed", 10.0);

        interactOnlyOwn = config.getBoolean("interact-only-own", false);
        xpKeepPercentage = (float) config.getDouble("xp-keep-percentage", 1.0);
        storeItems = config.getBoolean("store-items", true);
        storeXP = config.getBoolean("store-xp", true);

        graveItemOrder = config.getStringList("grave-item-order");
        if (graveItemOrder == null || graveItemOrder.isEmpty()) {
            graveItemOrder = Arrays.asList("ARMOR", "HAND", "OFFHAND");
        }

        autoEquipArmor = config.getBoolean("auto-equip-armor", true);
        graveLimit = config.getInt("grave-limit", -1);
        despawnWhenEmpty = config.getBoolean("despawn-when-empty", true);

        saveGravesEnabled = config.getBoolean("save-graves.enabled", true);
        autoSaveSeconds = config.getInt("save-graves.auto-save-seconds", 30);

        customSkullEnabled = config.getBoolean("custom-grave-skull.enabled", false);
        customSkullBase64 = config.getString("custom-grave-skull.base64", "");

        disabledWorlds = config.getStringList("disabled-worlds");
        if (disabledWorlds == null) disabledWorlds = Collections.emptyList();

        blacklistedDeathCauses = config.getStringList("blacklisted-death-causes");
        if (blacklistedDeathCauses == null) blacklistedDeathCauses = Collections.emptyList();

        worldNames.clear();
        ConfigurationSection worldSection = config.getConfigurationSection("world-name");
        if (worldSection != null) {
            for (String key : worldSection.getKeys(false)) {
                worldNames.put(key, worldSection.getString(key, key));
            }
        }

        hologramBgColor = config.getString("holograms.background-color", "00000000");
        hologramBillboard = config.getString("holograms.billboard", "center");
        hologramSeeThrough = config.getBoolean("holograms.see-through", true);
        hologramShadow = config.getBoolean("holograms.shadow", true);

        // Messages
        hologramLines = messages.getStringList("hologram");
        if (hologramLines == null || hologramLines.isEmpty()) {
            hologramLines = Arrays.asList(
                    "<gradient:#FF5555:#FFAA00><b>%player%'s Grave</b></gradient>",
                    "&#00CC00\u231b &#00FF00%despawn-time% &#00CC00\u231b",
                    "&#00AAFF%item% &fItems &8| &#00AAFF%xp% &fXP"
            );
        }

        helpLines = messages.getStringList("help");
        guiName = messages.getString("gui-name", isGerman ? "&0Grab von %player%" : "&0%player%'s Grave");
        unknownPlayer = messages.getString("unknown-player", isGerman ? "Unbekannt" : "Unknown");
        reloadSuccess = messages.getString("reload.success", isGerman ? "&#33FF33Plugin erfolgreich neu geladen!" : "&#33FF33Plugin reloaded successfully!");
        reloadFailed = messages.getString("reload.failed", isGerman ? "&#FF3333Fehler beim Neuladen der Konfiguration!" : "&#FF3333Failed to reload configuration!");
        interactNotYourGrave = messages.getString("interact.not-your-grave", isGerman ? "&#FFAAFFDu kannst nur dein eigenes Grab \u00f6ffnen!" : "&#FFAAFFYou can only open your own grave!");

        deathMessageEnabled = messages.getBoolean("death-message.enabled", false);
        deathMessage = messages.getString("death-message.message", isGerman ? "&#FFAAAADu bist bei &f%world% %x%, %y%, %z% &#FFAAAAgestorben!" : "&#FFAAAAYou died at &f%world% %x%, %y%, %z%&#FFAAAA!");

        graveListHeader = messages.getString("grave-list.header", isGerman ? "&#FFAA00&l=== GR\u00c4BER ===" : "&#FFAA00&l=== GRAVES ===");
        graveListFormat = messages.getString("grave-list.grave", "&#FF5555[TP] &#FFAAFF%player% &7- &#FFAAFF%world% %x%, %y%, %z% &7(%time%)");
        graveListNoGraves = messages.getString("grave-list.no-graves", isGerman ? "&#FFAAFFKeine Gr\u00e4ber gefunden!" : "&#FFAAFFNo graves found!");

        cmdInvalidValue = messages.getString("commands.invalid-value", isGerman ? "&#FF0000Ung\u00fcltiger Parameter: &#BB0000%value%" : "&#FF0000Invalid parameter: &#BB0000%value%");
        cmdInvalidCommand = messages.getString("commands.invalid-command", isGerman ? "&#FF0000Ung\u00fcltiger Befehl oder Unterbefehl!" : "&#FF0000Invalid command or subcommand!");
        cmdMissingArgument = messages.getString("commands.missing-argument", isGerman ? "&#FF0000Fehlendes Argument! Bitte gib einen Wert an." : "&#FF0000Missing argument! Please specify a value.");
        cmdNoPermission = messages.getString("commands.no-permission", isGerman ? "&#FF0000Du hast keine Berechtigung f\u00fcr diesen Befehl!" : "&#FF0000You do not have permission for this command!");
        cmdPlayerOnly = messages.getString("commands.player-only", isGerman ? "&#FF0000Dieser Befehl kann nur als Spieler ausgef\u00fchrt werden!" : "&#FF0000This command can only be executed by a player!");
        cmdInvalidPlayer = messages.getString("commands.invalid-player", isGerman ? "&#FF0000Der Spieler &#BB0000%player% &#FF0000wurde nicht gefunden!" : "&#FF0000Player &#BB0000%player% &#FF0000was not found!");
    }

    public synchronized void saveChanges() {
        if (config == null) return;
        try {
            config.set("language", language);
            config.set("despawn-time-seconds", despawnTimeSeconds);
            config.set("drop-items", dropItems);
            config.set("dropped-item-velocity", droppedItemVelocity);
            config.set("enable-instant-pickup", enableInstantPickup);
            config.set("instant-pickup-only-own", instantPickupOnlyOwn);
            config.set("auto-equip-armor", autoEquipArmor);
            config.set("interact-only-own", interactOnlyOwn);
            config.set("auto-rotation.enabled", autoRotationEnabled);
            config.set("despawn-when-empty", despawnWhenEmpty);
            config.save(configFile);
        } catch (Exception e) {
            plugin.getLogger().severe("[BorderMC-Graves] Fehler beim Speichern der Konfiguration: " + e.getMessage());
        }
    }

    private void saveResourceDefault(String name, File target) {
        try {
            if (!target.getParentFile().exists()) {
                target.getParentFile().mkdirs();
            }
            InputStream in = plugin.getResource(name);
            if (in == null) {
                in = getClass().getClassLoader().getResourceAsStream(name);
            }
            if (in != null) {
                Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not copy default resource " + name + ": " + e.getMessage());
        }
    }

    public static String parseColor(String input) {
        if (input == null || input.isEmpty()) return "";
        return XGraveVersion.colorize(input);
    }

    public String getPrefix() {
        return prefixComponent;
    }

    public String getPrefixRaw() {
        return prefixRaw;
    }

    public int getDespawnTimeSeconds() {
        return despawnTimeSeconds;
    }

    public void setDespawnTimeSeconds(int despawnTimeSeconds) {
        this.despawnTimeSeconds = despawnTimeSeconds;
    }

    public boolean isDropItems() {
        return dropItems;
    }

    public void setDropItems(boolean dropItems) {
        this.dropItems = dropItems;
    }

    public boolean isDroppedItemVelocity() {
        return droppedItemVelocity;
    }

    public void setDroppedItemVelocity(boolean droppedItemVelocity) {
        this.droppedItemVelocity = droppedItemVelocity;
    }

    public boolean isEnableInstantPickup() {
        return enableInstantPickup;
    }

    public void setEnableInstantPickup(boolean enableInstantPickup) {
        this.enableInstantPickup = enableInstantPickup;
    }

    public boolean isInstantPickupOnlyOwn() {
        return instantPickupOnlyOwn;
    }

    public void setInstantPickupOnlyOwn(boolean instantPickupOnlyOwn) {
        this.instantPickupOnlyOwn = instantPickupOnlyOwn;
    }

    public boolean isOverrideKeepInventory() {
        return overrideKeepInventory;
    }

    public boolean isOverrideKeepLevel() {
        return overrideKeepLevel;
    }

    public double getHologramHeight() {
        return hologramHeight;
    }

    public double getHeadHeight() {
        return headHeight;
    }

    public boolean isRotateHead360() {
        return rotateHead360;
    }

    public boolean isAutoRotationEnabled() {
        return autoRotationEnabled;
    }

    public void setAutoRotationEnabled(boolean autoRotationEnabled) {
        this.autoRotationEnabled = autoRotationEnabled;
    }

    public double getAutoRotationSpeed() {
        return autoRotationSpeed;
    }

    public boolean isInteractOnlyOwn() {
        return interactOnlyOwn;
    }

    public void setInteractOnlyOwn(boolean interactOnlyOwn) {
        this.interactOnlyOwn = interactOnlyOwn;
    }

    public float getXpKeepPercentage() {
        return xpKeepPercentage;
    }

    public boolean isStoreItems() {
        return storeItems;
    }

    public boolean isStoreXP() {
        return storeXP;
    }

    public List<String> getGraveItemOrder() {
        return graveItemOrder;
    }

    public boolean isAutoEquipArmor() {
        return autoEquipArmor;
    }

    public void setAutoEquipArmor(boolean autoEquipArmor) {
        this.autoEquipArmor = autoEquipArmor;
    }

    public int getGraveLimit() {
        return graveLimit;
    }

    public boolean isDespawnWhenEmpty() {
        return despawnWhenEmpty;
    }

    public void setDespawnWhenEmpty(boolean despawnWhenEmpty) {
        this.despawnWhenEmpty = despawnWhenEmpty;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String lang) {
        String clean = (lang != null && lang.equalsIgnoreCase("de")) ? "de" : "en";
        this.language = clean;
        if (config != null) {
            config.set("language", clean);
            saveChanges();
            load();
        }
    }

    public boolean isDe() {
        return "de".equalsIgnoreCase(language);
    }

    public boolean isSaveGravesEnabled() {
        return saveGravesEnabled;
    }

    public int getAutoSaveSeconds() {
        return autoSaveSeconds;
    }

    public boolean isCustomSkullEnabled() {
        return customSkullEnabled;
    }

    public String getCustomSkullBase64() {
        return customSkullBase64;
    }

    public List<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public List<String> getBlacklistedDeathCauses() {
        return blacklistedDeathCauses;
    }

    public String getWorldDisplayName(String worldName) {
        return worldNames.getOrDefault(worldName, worldName);
    }

    public String getHologramBgColor() {
        return hologramBgColor;
    }

    public String getHologramBillboard() {
        return hologramBillboard;
    }

    public boolean isHologramSeeThrough() {
        return hologramSeeThrough;
    }

    public boolean isHologramShadow() {
        return hologramShadow;
    }

    public List<String> getHologramLines() {
        return hologramLines;
    }

    public List<String> getHelpLines() {
        return helpLines;
    }

    public String getGuiName() {
        return guiName;
    }

    public String getUnknownPlayer() {
        return unknownPlayer;
    }

    public String getReloadSuccess() {
        return reloadSuccess;
    }

    public String getReloadFailed() {
        return reloadFailed;
    }

    public String getInteractNotYourGrave() {
        return interactNotYourGrave;
    }

    public boolean isDeathMessageEnabled() {
        return deathMessageEnabled;
    }

    public String getDeathMessage() {
        return deathMessage;
    }

    public String getGraveListHeader() {
        return graveListHeader;
    }

    public String getGraveListFormat() {
        return graveListFormat;
    }

    public String getGraveListNoGraves() {
        return graveListNoGraves;
    }

    public String getCmdInvalidValue() {
        return cmdInvalidValue;
    }

    public String getCmdInvalidCommand() {
        return cmdInvalidCommand;
    }

    public String getCmdMissingArgument() {
        return cmdMissingArgument;
    }

    public String getCmdNoPermission() {
        return cmdNoPermission;
    }

    public String getCmdPlayerOnly() {
        return cmdPlayerOnly;
    }

    public String getCmdInvalidPlayer() {
        return cmdInvalidPlayer;
    }
}

