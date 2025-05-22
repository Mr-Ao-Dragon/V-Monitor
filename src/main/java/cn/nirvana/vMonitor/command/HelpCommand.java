package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.config.LanguageLoader;

import com.velocitypowered.api.command.CommandSource;

import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HelpCommand {
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;

    public HelpCommand(LanguageLoader languageLoader, MiniMessage miniMessage) {
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
    }

    public void execute(CommandSource source, String[] args) {
        String helpMessage = languageLoader.getMessage("help-format");
        if (helpMessage != null && !helpMessage.isEmpty() && !helpMessage.startsWith("<red>Missing Language Key:")) {
            source.sendMessage(miniMessage.deserialize(helpMessage));
        } else {
            source.sendMessage(miniMessage.deserialize("<red>No help message configured or key 'help-format' is missing in the language file.</red>"));
        }
    }

    public CompletableFuture<List<String>> suggest(CommandSource source, String[] args) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    public LanguageLoader getLanguageLoader() {
        return languageLoader;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}