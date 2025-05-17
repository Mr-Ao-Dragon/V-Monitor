package cn.nirvana.vMonitor;

import cn.nirvana.vMonitor.command.CommandRegistrar;
import cn.nirvana.vMonitor.command.HelpCommand;

import com.google.inject.Inject;

import com.moandjiezana.toml.Toml;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "v-monitor")
public class VMonitor {

    private static Logger logger;
    private static ProxyServer proxyServer;
    private static MiniMessage miniMessage = MiniMessage.miniMessage();
    private static Path dataDirectory;
    private ConfigManager configManager;
    private OnlineListManager onlineListManager;
    private PlayerActivityListener playerActivityListener;
    private HelpCommand helpCommand;
    private final CommandManager commandManager;

    @Inject
    public VMonitor(Logger logger, @DataDirectory Path dataDirectory, ProxyServer proxyServer, CommandManager commandManager) {
        VMonitor.logger = logger;
        VMonitor.proxyServer = proxyServer;
        VMonitor.dataDirectory = dataDirectory;
        this.commandManager = commandManager;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        configManager = new ConfigManager(proxyServer, this, miniMessage);
        onlineListManager = new OnlineListManager(proxyServer, configManager, miniMessage);
        playerActivityListener = new PlayerActivityListener(proxyServer, configManager, miniMessage, configManager.getPlayerData());
        helpCommand = new HelpCommand(configManager);

        CommandRegistrar commandRegistrar = new CommandRegistrar(proxyServer, commandManager, configManager, onlineListManager, helpCommand);
        commandRegistrar.registerCommands();

        proxyServer.getEventManager().register(this, playerActivityListener);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static ProxyServer getProxyServer() {
        return proxyServer;
    }

    public static MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public static Path getDataDirectory() {
        return dataDirectory;
    }

    public Toml getConfig() {
        return configManager.getConfig();
    }

    public void loadConfig() {
        configManager.loadConfig();
    }

    public void savePlayerData() {
        configManager.savePlayerData();
    }
}