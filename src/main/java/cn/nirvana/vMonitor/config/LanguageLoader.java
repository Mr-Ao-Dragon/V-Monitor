package cn.nirvana.vMonitor.config;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class LanguageLoader {
    private final Logger logger;
    private final Path dataDirectory;
    private final ConfigFileLoader configFileLoader;
    private final String langFolderName = "lang";

    private Map<String, Object> language;

    public LanguageLoader(Logger logger, Path dataDirectory, ConfigFileLoader configFileLoader) {
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.configFileLoader = configFileLoader;
    }

    public void loadLanguage() {
        if (this.configFileLoader.getConfig() == null) {
            logger.error("Config is not loaded. Cannot load language.");
            this.language = new HashMap<>();
            return;
        }
        String defaultLang = this.configFileLoader.getString("language.default", "en_us");
        File langFile = new File(dataDirectory.toFile(), langFolderName + File.separator + defaultLang + ".yml");
        if (!langFile.exists()) {
            try {
                langFile.getParentFile().mkdirs();
                String resourceLangPath = "/lang/" + defaultLang + ".yml";
                InputStream defaultLangStream = getClass().getResourceAsStream(resourceLangPath);
                if (defaultLangStream == null && !defaultLang.equals("en_us")) {
                    logger.warn("Default language file ({}) not found in JAR. Trying en_us.yml.", defaultLang + ".yml");
                    resourceLangPath = "/lang/en_us.yml";
                    defaultLangStream = getClass().getResourceAsStream(resourceLangPath);
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
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(langFile), StandardCharsets.UTF_8)) {
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
        } catch (Exception e) {
            this.language = new HashMap<>();
            logger.error("Error parsing language file: {}", langFile.getName(), e);
        }
    }

    public String getMessage(String key) {
        if (language == null) {
            logger.warn("Language not loaded when trying to get key: {}", key);
            return "<red>Internal Error: Language not loaded.</red>";
        }
        Object value = language.get(key);
        if (value == null) {
            logger.warn("Missing language key: {}", key);
            return "<red>Missing Language Key: " + key + "</red>";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof List) {
            logger.warn("Tried to get language key '{}' as String, but it's a List. Use getLanguageMap().get() and cast appropriately.", key);
            return "<red>Language key '" + key + "' is a list, expected a string.</red>";
        }
        logger.warn("Language key '{}' has unexpected type: {}. Trying toString().", key, value.getClass().getName());
        return value.toString();
    }

    public Map<String, Object> getLanguageMap() {
        return language;
    }
}