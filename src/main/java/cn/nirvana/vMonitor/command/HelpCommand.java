package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.config.LanguageLoader;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.minimessage.MiniMessage;

// No Brigadier imports needed here as registration is handled externally by CommandRegistrar

public class HelpCommand {
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;

    // Constructor no longer takes CommandRegistrar
    public HelpCommand(LanguageLoader languageLoader, MiniMessage miniMessage) {
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
        // No register method call here anymore
    }

    /**
     * Executes the help command logic.
     * Displays the help message from the language file.
     * This method is called directly by CommandRegistrar.
     *
     * @param source The command source.
     */
    public void execute(CommandSource source) { // Simplified execute method
        String helpMessage = languageLoader.getMessage("help-format");
        if (helpMessage != null && !helpMessage.isEmpty() && !helpMessage.startsWith("<red>Missing Language Key:")) {
            source.sendMessage(miniMessage.deserialize(helpMessage));
        } else {
            source.sendMessage(miniMessage.deserialize("<red>No help message configured or key 'help-format' is missing in the language file.</red>"));
        }
    }
}