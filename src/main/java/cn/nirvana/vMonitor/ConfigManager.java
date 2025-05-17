package cn.nirvana.vMonitor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import com.moandjiezana.toml.Toml;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.slf4j.Logger;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public class ConfigManager implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final VMonitor plugin;
    private Toml config;
    private Map<String, Object> language;
    private final MiniMessage miniMessage;
    private final String configFileName = "config.toml";
    private final String playerDataFileName = "playerdata.json";
    private final String langFolderName = "lang";
    private final Path dataDirectory;
    private final Logger logger;
    private HashMap<UUID, PlayerActivityListener.PlayerFirstJoinInfo> playerData = new HashMap<>();
    private Map<String, String> serverDisplayNames = new HashMap<>();

    public ConfigManager(ProxyServer proxyServer, VMonitor plugin, MiniMessage miniMessage) {
        this.proxyServer = proxyServer;
        this.plugin = plugin;
        this.miniMessage = miniMessage;
        this.dataDirectory = VMonitor.getDataDirectory();
        this.logger = VMonitor.getLogger();
        loadConfig();
        loadLanguage();
        loadPlayerData();
        loadServerAliases();
    }

    public Toml getConfig() {
        return this.config;
    }

    public String getConfigVersion() {
        if (config == null) return "1.0";
        return config.getString("version", "1.0");
    }

    public Map<String, Object> getLanguage() {
        return language;
    }

    public String getMessage(String key) {
        if (language == null) {
            VMonitor.getLogger().warn("Language not loaded when trying to get key: {}", key);
            return "<red>Language not loaded or key missing: " + key + "</red>";
        }
        Object value = language.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof List) {
            VMonitor.getLogger().warn("Tried to get language key '{}' as String, but it's a List. Use getLanguage().get() and cast appropriately.", key);
            return "<red>Language key '" + key + "' is a list, expected a string.</red>";
        }
        if (value == null) {
            VMonitor.getLogger().warn("Missing language key: {}", key);
            return "<red>Missing Language Key: " + key + "</red>";
        }
        VMonitor.getLogger().warn("Language key '{}' has unexpected type: {}. Trying toString().", key, value.getClass().getName());
        return value.toString();
    }

    public HashMap<UUID, PlayerActivityListener.PlayerFirstJoinInfo> getPlayerData() {
        return playerData;
    }

    public void loadConfig() {
        try {
            File configFile = new File(dataDirectory.toFile(), configFileName);
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                try (InputStream defaultConfigStream = VMonitor.class.getResourceAsStream("/" + configFileName)) {
                    if (defaultConfigStream == null) {
                        logger.error("Could not find default config file in JAR: /{}", configFileName);
                        this.config = new Toml();
                        return;
                    }
                    Files.copy(defaultConfigStream, configFile.toPath());
                }
            }
            config = new Toml().read(configFile);
            logger.info("Successfully loaded config file");
            logger.info("Config Version: {}", getConfigVersion());
        } catch (IOException e) {
            logger.error("Could not load config file: {}", configFileName, e);
            this.config = new Toml();
        }
    }

    public void loadLanguage() {
        if (this.config == null) {
            logger.error("Config is not loaded. Cannot load language.");
            this.language = new HashMap<>();
            return;
        }
        String defaultLang = this.config.getString("language.default", "en_us");
        File langFile = new File(dataDirectory.toFile(), langFolderName + File.separator + defaultLang + ".yml");
        if (!langFile.exists()) {
            try {
                langFile.getParentFile().mkdirs();
                String resourceLangPath = "/lang/" + defaultLang + ".yml";
                InputStream defaultLangStream = VMonitor.class.getResourceAsStream(resourceLangPath);
                if (defaultLangStream == null && !defaultLang.equals("en_us")) {
                    logger.warn("Default language file ({}) not found in JAR. Trying en_us.yml.", defaultLang + ".yml");
                    resourceLangPath = "/lang/en_us.yml";
                    defaultLangStream = VMonitor.class.getResourceAsStream(resourceLangPath);
                }
                if (defaultLangStream != null) {
                    Files.copy(defaultLangStream, langFile.toPath());
                    logger.info("Default language file ({}) created from JAR.", langFile.getName());
                } else {
                    this.language = new HashMap<>();
                    logger.error("Could not find any default language file ({} or en_us.yml) in JAR.", defaultLang + ".yml");
                    return;
                }
            } catch (IOException e) {
                this.language = new HashMap<>();
                logger.error("Could not create default language file {}: ", langFile.getName(), e);
                return;
            }
        }
        Yaml yaml = new Yaml(new Constructor(Map.class));
        try (FileReader reader = new FileReader(langFile)) {
            Map<String, Object> loadedData = yaml.load(reader);
            if (loadedData == null) {
                this.language = new HashMap<>();
                logger.warn("Language file is empty or invalid: {}", langFile.getName());
            } else {
                this.language = loadedData;
            }
            logger.info("Successfully loaded language file: {}", langFile.getName());
        } catch (IOException e) {
            this.language = new HashMap<>();
            logger.error("Could not load language file: {}", langFile.getName(), e);
        }
    }

    private void loadServerAliases() {
        if (this.config == null) {
            logger.error("Config is not loaded. Cannot load server display names.");
            this.serverDisplayNames = new HashMap<>(); // 初始化显示名称映射
            return;
        }
        Toml aliasesSection = this.config.getTable("server-aliases");
        this.serverDisplayNames.clear();

        if (aliasesSection != null) {
            try {
                Map<String, Object> rawAliases = aliasesSection.toMap();
                for (Map.Entry<String, Object> entry : rawAliases.entrySet()) {
                    String actualName = entry.getKey();
                    Object displayAliasObj = entry.getValue();
                    if (displayAliasObj instanceof String) {
                        String displayAlias = (String) displayAliasObj;
                        if (this.serverDisplayNames.containsKey(actualName)) {
                            logger.warn("Duplicate actual server name '{}' found in aliases. Using the last defined display alias.", actualName);
                        }
                        this.serverDisplayNames.put(actualName, displayAlias);
                    } else {
                        logger.warn("Server alias for actual server name '{}' has a non-string value. Skipping.", actualName);
                    }
                }
                logger.info("Successfully loaded {} server display names.", this.serverDisplayNames.size());
            } catch (Exception e) {
                logger.error("Could not load server display names from config.", e);
                this.serverDisplayNames = new HashMap<>();
            }
        } else {
            logger.info("No server aliases section found in config.");
            this.serverDisplayNames = new HashMap<>();
        }
    }

    public void loadPlayerData() {
        File playerDataFile = new File(dataDirectory.toFile(), playerDataFileName);
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.getParentFile().mkdirs();
                if (playerDataFile.createNewFile()) {
                    savePlayerData();
                }
            } catch (IOException e) {
                logger.error("Could not create player data file ", e);
            }
            if (this.playerData == null) {
                this.playerData = new HashMap<>();
            }
            return;
        }
        try (FileReader reader = new FileReader(playerDataFile)) {
            Gson gson = new Gson();
            java.lang.reflect.Type type = new TypeToken<HashMap<UUID, PlayerActivityListener.PlayerFirstJoinInfo>>() {
            }.getType();
            HashMap<UUID, PlayerActivityListener.PlayerFirstJoinInfo> loadedData = gson.fromJson(reader, type);
            if (loadedData != null) {
                this.playerData.clear();
                this.playerData.putAll(loadedData);
            } else if (this.playerData == null) {
                this.playerData = new HashMap<>();
            }
            logger.info("Successfully loaded player data file");
        } catch (IOException e) {
            logger.error("Could not load player data file ", e);
            if (this.playerData == null) {
                this.playerData = new HashMap<>();
            }
        }
    }

    public void savePlayerData() {
        File playerDataFile = new File(dataDirectory.toFile(), playerDataFileName);
        try (FileWriter writer = new FileWriter(playerDataFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            if (this.playerData == null) {
                this.playerData = new HashMap<>();
            }
            gson.toJson(this.playerData, writer);
            logger.info("Successfully saved player data file");
        } catch (IOException e) {
            logger.error("Could not save player data file ", e);
        }
    }

    public String getServerDisplayName(String actualServerName) {
        return serverDisplayNames.getOrDefault(actualServerName, actualServerName);
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (source instanceof Player) {
            Player player = (Player) source;
            if (player.hasPermission("vmonitor.reload")) {
                loadConfig();
                loadLanguage();
                loadServerAliases();
                source.sendMessage(miniMessage.deserialize(getMessage("reload-success")));
            } else {
                player.sendMessage(miniMessage.deserialize(getMessage("no-permission")));
            }
        } else {
            loadConfig();
            loadLanguage();
            loadServerAliases();
            source.sendMessage(miniMessage.deserialize(getMessage("reload-success")));
        }
    }
}