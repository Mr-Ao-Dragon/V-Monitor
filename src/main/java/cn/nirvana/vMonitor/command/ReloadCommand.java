package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.config.ConfigFileLoader;
import cn.nirvana.vMonitor.config.LanguageLoader;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReloadCommand {
    private final ConfigFileLoader configFileLoader;
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;

    public ReloadCommand(ConfigFileLoader configFileLoader, LanguageLoader languageLoader, MiniMessage miniMessage) {
        this.configFileLoader = configFileLoader;
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
    }

    public void execute(CommandSource source, String[] args) {
        if (source instanceof Player && !source.hasPermission("vmonitor.reload")) {
            source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("no-permission")));
            return;
        }
        configFileLoader.loadConfig();
        languageLoader.loadLanguage();
        source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("reload-success")));
    }

    public CompletableFuture<List<String>> suggest(CommandSource source, String[] args) {
        return CompletableFuture.completedFuture(new ArrayList<>());
    }
}