package cn.nirvana.vMonitor;

import cn.nirvana.vMonitor.command.CommandRegistrar;
import cn.nirvana.vMonitor.command.HelpCommand;
import cn.nirvana.vMonitor.command.InfoCommand;
import cn.nirvana.vMonitor.command.ListCommand;
import cn.nirvana.vMonitor.command.ReloadCommand;
import cn.nirvana.vMonitor.config.ConfigFileLoader;
import cn.nirvana.vMonitor.config.LanguageLoader;
import cn.nirvana.vMonitor.config.PlayerDataLoader;
import cn.nirvana.vMonitor.listener.PlayerActivityListener;

import com.google.inject.Inject;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "v-monitor", name = "V-Monitor", version = "1.1.0", authors = {"YourName"})
public class VMonitor {
    private final Logger logger;
    private final ProxyServer proxyServer;
    private final Path dataDirectory;
    private final CommandManager commandManager;
    private final MiniMessage miniMessage;

    private ConfigFileLoader configFileLoader;
    private LanguageLoader languageLoader;
    private PlayerDataLoader playerDataLoader;
    private PlayerActivityListener playerActivityListener;
    private CommandRegistrar commandRegistrar;

    private ListCommand listCommand;
    private HelpCommand helpCommand;
    private ReloadCommand reloadCommand;
    private InfoCommand infoCommand;


    @Inject
    public VMonitor(Logger logger, @DataDirectory Path dataDirectory, ProxyServer proxyServer, CommandManager commandManager) {
        this.logger = logger;
        this.proxyServer = proxyServer;
        this.dataDirectory = dataDirectory;
        this.commandManager = commandManager;
        this.miniMessage = MiniMessage.miniMessage();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("V-Monitor is starting...");

        configFileLoader = new ConfigFileLoader(logger, dataDirectory);
        languageLoader = new LanguageLoader(logger, dataDirectory, configFileLoader);
        playerDataLoader = new PlayerDataLoader(logger, dataDirectory);

        configFileLoader.loadConfig();
        languageLoader.loadLanguage();
        playerDataLoader.loadPlayerData();

        playerActivityListener = new PlayerActivityListener(proxyServer, configFileLoader, languageLoader, playerDataLoader, miniMessage);
        proxyServer.getEventManager().register(this, playerActivityListener);

        listCommand = new ListCommand(proxyServer, configFileLoader, languageLoader, miniMessage);
        helpCommand = new HelpCommand(languageLoader, miniMessage);
        reloadCommand = new ReloadCommand(configFileLoader, languageLoader, miniMessage);
        infoCommand = new InfoCommand(proxyServer, languageLoader, miniMessage, configFileLoader); // Pass configFileLoader

        commandRegistrar = new CommandRegistrar(commandManager, proxyServer, listCommand, helpCommand, reloadCommand, infoCommand);
        commandRegistrar.registerCommands();

        logger.info("V-Monitor enabled!");
    }

    public ConfigFileLoader getConfigFileLoader() {
        return configFileLoader;
    }

    public LanguageLoader getLanguageLoader() {
        return languageLoader;
    }

    public PlayerDataLoader getPlayerDataLoader() {
        return playerDataLoader;
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }
}