package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.config.LanguageLoader;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta; // Make sure this is imported
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.util.function.Consumer;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class CommandRegistrar {
    private final CommandManager commandManager;
    private final ProxyServer proxyServer;
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;
    private final Logger logger;

    private LiteralCommandNode<CommandSource> vmonitorRootNode;
    private LiteralCommandNode<CommandSource> serverSubCommandNode;
    private LiteralCommandNode<CommandSource> pluginSubCommandNode;

    // References to command classes for execution (HelpCommand, ReloadCommand)
    private HelpCommand helpCommand;
    private ReloadCommand reloadCommand;

    public CommandRegistrar(CommandManager commandManager, ProxyServer proxyServer,
                            LanguageLoader languageLoader, MiniMessage miniMessage, Logger logger) {
        this.commandManager = commandManager;
        this.proxyServer = proxyServer;
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
        this.logger = logger;
    }

    // Setters for command instances - VMonitor will call these
    public void setHelpCommand(HelpCommand helpCommand) {
        this.helpCommand = helpCommand;
    }

    public void setReloadCommand(ReloadCommand reloadCommand) {
        this.reloadCommand = reloadCommand;
    }

    public void registerCommands() {
        // Build the primary command node for "/vmonitor".
        vmonitorRootNode = LiteralArgumentBuilder.<CommandSource>literal("vmonitor")
                .executes(context -> {
                    // Default command execution for /vmonitor: show plugin version or help
                    String version = "1.1.1"; // Placeholder, ideally get this from VMonitor main class
                    Component message = miniMessage.deserialize(languageLoader.getMessage("plugin-version-info")
                            .replace("{version}", version));
                    context.getSource().sendMessage(message);
                    return SINGLE_SUCCESS;
                }).build();

        // 1. /vmonitor help (vm help) - Re-add this logic directly in CommandRegistrar
        vmonitorRootNode.addChild(LiteralArgumentBuilder.<CommandSource>literal("help")
                .executes(context -> {
                    if (helpCommand != null) {
                        helpCommand.execute(context.getSource());
                    } else {
                        // This error should ideally not happen with proper initialization
                        context.getSource().sendMessage(miniMessage.deserialize("<red>Help command not initialized. Internal error.</red>"));
                        logger.error("HelpCommand instance is null when trying to execute /vmonitor help.");
                    }
                    return SINGLE_SUCCESS;
                }).build()
        );

        // 2. /vmonitor reload (vm reload) - Re-add this logic directly in CommandRegistrar
        vmonitorRootNode.addChild(LiteralArgumentBuilder.<CommandSource>literal("reload")
                .requires(source -> source.hasPermission("vmonitor.reload"))
                .executes(context -> {
                    if (reloadCommand != null) {
                        reloadCommand.execute(context.getSource());
                    } else {
                        // This error should ideally not happen with proper initialization
                        context.getSource().sendMessage(miniMessage.deserialize("<red>Reload command not initialized. Internal error.</red>"));
                        logger.error("ReloadCommand instance is null when trying to execute /vmonitor reload.");
                    }
                    return SINGLE_SUCCESS;
                }).build()
        );

        // 3. /vmonitor server (vm server) - Root for server related commands
        this.serverSubCommandNode = LiteralArgumentBuilder.<CommandSource>literal("server")
                .executes(context -> {
                    context.getSource().sendMessage(miniMessage.deserialize(languageLoader.getMessage("server-command-help")));
                    return SINGLE_SUCCESS;
                }).build();
        vmonitorRootNode.addChild(serverSubCommandNode);

        // 4. /vmonitor plugin (vm plugin) - Root for plugin related commands
        this.pluginSubCommandNode = LiteralArgumentBuilder.<CommandSource>literal("plugin")
                .requires(source -> source.hasPermission("vmonitor.plugin"))
                .executes(context -> {
                    context.getSource().sendMessage(miniMessage.deserialize(languageLoader.getMessage("plugin-command-help")));
                    return SINGLE_SUCCESS;
                }).build();
        vmonitorRootNode.addChild(pluginSubCommandNode);

        // Register the primary command "vmonitor" with CommandMeta, including "vm" as an alias.
        CommandMeta commandMeta = commandManager.metaBuilder("vmonitor")
                .aliases("vm") // Ensure "vm" is correctly added as an alias
                .build();
        commandManager.register(commandMeta, new BrigadierCommand(vmonitorRootNode));

        logger.info("Main V-Monitor commands and first-level sub-commands (help, reload, server, plugin) registered.");
    }

    /**
     * Provides an interface for other classes to register sub-commands under the 'server' root.
     * External classes will add children to the `serverSubCommandNode`.
     *
     * @param subCommandBuilder A consumer that accepts the LiteralCommandNode for the 'server' command.
     */
    public void registerServerSubCommand(Consumer<LiteralCommandNode<CommandSource>> subCommandBuilder) {
        if (serverSubCommandNode != null) {
            subCommandBuilder.accept(serverSubCommandNode);
            logger.info("Registered a sub-command under /vmonitor server.");
        } else {
            logger.warn("Server sub-command root not initialized. Cannot register sub-command.");
        }
    }

    /**
     * Provides an interface for other classes to register sub-commands under the 'plugin' root.
     * External classes will add children to the `pluginSubCommandNode`.
     *
     * @param subCommandBuilder A consumer that accepts the LiteralCommandNode for the 'plugin' command.
     */
    public void registerPluginSubCommand(Consumer<LiteralCommandNode<CommandSource>> subCommandBuilder) {
        if (pluginSubCommandNode != null) {
            subCommandBuilder.accept(pluginSubCommandNode);
            logger.info("Registered a sub-command under /vmonitor plugin.");
        } else {
            logger.warn("Plugin sub-command root not initialized. Cannot register sub-command.");
        }
    }
}