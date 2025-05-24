package cn.nirvana.vMonitor.config;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigFileLoader {
    private final Logger logger;
    private final Path dataDirectory;
    private final String configFileName = "config.toml";

    private Toml config;
    private Map<String, String> serverDisplayNames = new HashMap<>();

    public ConfigFileLoader(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public Toml getConfig() {
        return config;
    }

    public void loadConfig() {
        try {
            File configFile = new File(dataDirectory.toFile(), configFileName);
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                try (InputStream defaultConfigStream = getClass().getResourceAsStream("/" + configFileName)) {
                    if (defaultConfigStream == null) {
                        logger.error("Could not find default config file in JAR: /{}", configFileName);
                        this.config = new Toml();
                        return;
                    }
                    Files.copy(defaultConfigStream, configFile.toPath());
                    logger.info("Default config file created at {}", configFile.getAbsolutePath());
                }
            }
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                this.config = new Toml().read(reader);
            }
            loadServerAliases();
            logger.info("Successfully loaded config file: {}", configFileName);
            logger.info("Config Version: {}", getConfigVersion());
        } catch (IOException e) {
            logger.error("Could not load config file: {}", configFileName, e);
            this.config = new Toml();
        } catch (Exception e) {
            logger.error("Error parsing config file: {}", configFileName, e);
            this.config = new Toml();
        }
    }

    public String getConfigVersion() {
        if (config == null) return "N/A";
        return config.getString("version", "1.1.1");
    }

    private void loadServerAliases() {
        this.serverDisplayNames.clear();
        if (this.config == null) {
            logger.warn("Config not loaded, cannot load server display names.");
            return;
        }
        Toml aliasesSection = this.config.getTable("server-aliases");
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

    public String getServerDisplayName(String actualServerName) {
        return serverDisplayNames.getOrDefault(actualServerName, actualServerName);
    }

    public String getString(String key, String defaultValue) {
        if (config == null) return defaultValue;
        return config.getString(key, defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        if (config == null) return defaultValue;
        return config.getBoolean(key, defaultValue);
    }

    public Toml getTable(String key) {
        if (config == null) return null;
        return config.getTable(key);
    }
}