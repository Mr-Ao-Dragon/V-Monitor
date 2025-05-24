package cn.nirvana.vMonitor;

import cn.nirvana.vMonitor.command.CommandRegistrar;
import cn.nirvana.vMonitor.command.HelpCommand;
import cn.nirvana.vMonitor.command.InfoCommand;
import cn.nirvana.vMonitor.command.ListCommand;
import cn.nirvana.vMonitor.command.PluginListCommand;
import cn.nirvana.vMonitor.command.PluginInfoCommand;
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

@Plugin(id = "v-monitor", name = "V-Monitor", version = "1.1.1", url = "https://github.com/Nirvana99/V-Monitor")
public final class VMonitor {
    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;
    private final CommandManager commandManager;
    private final MiniMessage miniMessage;

    private ConfigFileLoader configFileLoader;
    private LanguageLoader languageLoader;
    private PlayerDataLoader playerDataLoader;
    private CommandRegistrar commandRegistrar; // Keep a reference to CommandRegistrar

    @Inject
    public VMonitor(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory, CommandManager commandManager) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.commandManager = commandManager;
        this.miniMessage = MiniMessage.miniMessage(); // Initialize MiniMessage
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("Initializing V-Monitor...");

        // Load configurations
        configFileLoader = new ConfigFileLoader(logger, dataDirectory);
        configFileLoader.loadConfig();

        languageLoader = new LanguageLoader(logger, dataDirectory, configFileLoader);
        languageLoader.loadLanguage();

        playerDataLoader = new PlayerDataLoader(logger, dataDirectory);
        playerDataLoader.loadPlayerData();

        // Register event listeners
        proxyServer.getEventManager().register(this, new PlayerActivityListener(proxyServer, configFileLoader, languageLoader, playerDataLoader, miniMessage));

        // 1. Initialize CommandRegistrar
        commandRegistrar = new CommandRegistrar(commandManager, proxyServer, languageLoader, miniMessage, logger);

        // 2. Instantiate HelpCommand and ReloadCommand.
        //    These are just plain classes encapsulating logic, not registering commands themselves.
        HelpCommand helpCommandInstance = new HelpCommand(languageLoader, miniMessage);
        ReloadCommand reloadCommandInstance = new ReloadCommand(configFileLoader, languageLoader, miniMessage);

        // 3. Pass HelpCommand and ReloadCommand instances to CommandRegistrar.
        //    CommandRegistrar will use these instances for the executes() logic of /vmonitor help and /vmonitor reload.
        commandRegistrar.setHelpCommand(helpCommandInstance);
        commandRegistrar.setReloadCommand(reloadCommandInstance);

        // 4. IMPORTANT: Register the main command structure in CommandRegistrar FIRST.
        //    This initializes the root nodes for "vmonitor", "server", and "plugin".
        commandRegistrar.registerCommands(); // <-- This call MUST happen BEFORE instantiating classes that register sub-commands.

        // 5. Instantiate other command classes. Their constructors will now correctly
        //    call register...SubCommand() because the required LiteralCommandNodes in CommandRegistrar are initialized.
        new ListCommand(proxyServer, configFileLoader, languageLoader, miniMessage, commandRegistrar);
        new InfoCommand(proxyServer, languageLoader, miniMessage, configFileLoader, commandRegistrar);
        new PluginListCommand(proxyServer, languageLoader, miniMessage, commandRegistrar);
        new PluginInfoCommand(proxyServer, languageLoader, miniMessage, commandRegistrar);

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