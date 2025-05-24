package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.config.ConfigFileLoader;
import cn.nirvana.vMonitor.config.LanguageLoader;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.minimessage.MiniMessage;

// No Brigadier imports needed here as registration is handled externally by CommandRegistrar

public class ReloadCommand {
    private final ConfigFileLoader configFileLoader;
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;

    // Constructor no longer takes CommandRegistrar
    public ReloadCommand(ConfigFileLoader configFileLoader, LanguageLoader languageLoader, MiniMessage miniMessage) {
        this.configFileLoader = configFileLoader;
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
        // No register method call here anymore
    }

    /**
     * Executes the reload command logic.
     * This method is called directly by CommandRegistrar.
     *
     * @param source The command source.
     */
    public void execute(CommandSource source) { // Simplified execute method
        if (configFileLoader != null) {
            configFileLoader.loadConfig();
        } else {
            // This scenario should ideally not happen if ReloadCommand is always initialized with a proper ConfigFileLoader in VMonitor.
            // For now, we just log an error.
            source.sendMessage(miniMessage.deserialize("<red>Configuration loader not available for reload.</red>"));
        }
        languageLoader.loadLanguage();
        source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("reload-success")));
    }
}