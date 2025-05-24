package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.config.LanguageLoader;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.concurrent.CompletableFuture;
import java.util.List;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class CommandRegistrar {
    private final CommandManager commandManager;
    private final ProxyServer proxyServer;
    private final ListCommand listCommand;
    private final HelpCommand helpCommand;
    private final ReloadCommand reloadCommand;
    private final InfoCommand infoCommand;
    private final PluginListCommand pluginListCommand; // 新增
    private final PluginInfoCommand pluginInfoCommand; // 新增

    public CommandRegistrar(CommandManager commandManager, ProxyServer proxyServer, ListCommand listCommand, HelpCommand helpCommand, ReloadCommand reloadCommand, InfoCommand infoCommand, PluginListCommand pluginListCommand, PluginInfoCommand pluginInfoCommand) { // 修改构造函数
        this.commandManager = commandManager;
        this.proxyServer = proxyServer;
        this.listCommand = listCommand;
        this.helpCommand = helpCommand;
        this.reloadCommand = reloadCommand;
        this.infoCommand = infoCommand;
        this.pluginListCommand = pluginListCommand; // 初始化
        this.pluginInfoCommand = pluginInfoCommand; // 初始化
    }

    public void registerCommands() {
        LanguageLoader lang = helpCommand.getLanguageLoader(); // Assuming helpCommand provides LanguageLoader
        MiniMessage mm = helpCommand.getMiniMessage(); // Assuming helpCommand provides MiniMessage

        // help command
        LiteralArgumentBuilder<CommandSource> helpNode = BrigadierCommand.literalArgumentBuilder("help")
                .executes(context -> {
                    helpCommand.execute(context.getSource(), new String[0]);
                    return SINGLE_SUCCESS;
                });

        // reload command
        LiteralArgumentBuilder<CommandSource> reloadNode = BrigadierCommand.literalArgumentBuilder("reload")
                .requires(source -> source.hasPermission("vmonitor.reload"))
                .executes(context -> {
                    reloadCommand.execute(context.getSource(), new String[0]);
                    return SINGLE_SUCCESS;
                });

        // server list command
        LiteralArgumentBuilder<CommandSource> serverListSubCommand = BrigadierCommand.literalArgumentBuilder("list")
                .requires(source -> source.hasPermission("vmonitor.list"))
                .executes(context -> {
                    listCommand.execute(context.getSource(), new String[0]);
                    return SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("server_name", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            CompletableFuture<List<String>> handlerSuggestions = listCommand.suggest(context.getSource(), new String[]{builder.getRemaining()});
                            return handlerSuggestions.thenApply(suggestionsList -> {
                                suggestionsList.forEach(builder::suggest);
                                return builder.build();
                            });
                        })
                        .executes(context -> {
                            String serverNameArg = context.getArgument("server_name", String.class);
                            listCommand.execute(context.getSource(), new String[]{serverNameArg});
                            return SINGLE_SUCCESS;
                        })
                );

        // server info command
        LiteralArgumentBuilder<CommandSource> serverInfoSubCommand = BrigadierCommand.literalArgumentBuilder("info")
                .requires(source -> source.hasPermission("vmonitor.info"))
                .then(BrigadierCommand.requiredArgumentBuilder("server_name", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            CompletableFuture<List<String>> handlerSuggestions = infoCommand.suggest(context.getSource(), new String[]{builder.getRemaining()});
                            return handlerSuggestions.thenApply(suggestionsList -> {
                                suggestionsList.forEach(builder::suggest);
                                return builder.build();
                            });
                        })
                        .executes(context -> {
                            String serverNameArg = context.getArgument("server_name", String.class);
                            infoCommand.execute(context.getSource(), serverNameArg);
                            return SINGLE_SUCCESS;
                        })
                )
                .executes(context -> {
                    context.getSource().sendMessage(mm.deserialize(lang.getMessage("usage-server-info")));
                    return SINGLE_SUCCESS;
                });

        LiteralArgumentBuilder<CommandSource> serverNodeBuilder = BrigadierCommand.literalArgumentBuilder("server")
                .then(serverListSubCommand)
                .then(serverInfoSubCommand);

        // plugin command (New)
        LiteralArgumentBuilder<CommandSource> pluginRootNode = BrigadierCommand.literalArgumentBuilder("plugin")
                .executes(context -> {
                    context.getSource().sendMessage(mm.deserialize(lang.getMessage("usage-plugin"))); // Default help for /vmonitor plugin
                    return SINGLE_SUCCESS;
                })
                .then(PluginListCommand.build(pluginListCommand)) // Add 'list' sub-command
                .then(PluginInfoCommand.build(pluginInfoCommand)); // Add 'info' sub-command


        LiteralCommandNode<CommandSource> vmonitorRootNode = BrigadierCommand.literalArgumentBuilder("vmonitor")
                .executes(context -> {
                    helpCommand.execute(context.getSource(), new String[0]);
                    return SINGLE_SUCCESS;
                })
                .then(helpNode)
                .then(reloadNode)
                .then(serverNodeBuilder)
                .then(pluginRootNode) // Add plugin root command
                .build();

        CommandMeta vmonitorMeta = commandManager.metaBuilder("vmonitor")
                .aliases("vm")
                .build();
        commandManager.register(vmonitorMeta, new BrigadierCommand(vmonitorRootNode));
    }
}