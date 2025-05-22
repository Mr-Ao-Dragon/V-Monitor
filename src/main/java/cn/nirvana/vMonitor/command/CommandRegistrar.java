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

    public CommandRegistrar(CommandManager commandManager, ProxyServer proxyServer, ListCommand listCommand, HelpCommand helpCommand, ReloadCommand reloadCommand, InfoCommand infoCommand) {
        this.commandManager = commandManager;
        this.proxyServer = proxyServer;
        this.listCommand = listCommand;
        this.helpCommand = helpCommand;
        this.reloadCommand = reloadCommand;
        this.infoCommand = infoCommand;
    }

    public void registerCommands() {
        LanguageLoader lang = this.helpCommand.getLanguageLoader();
        MiniMessage mm = this.helpCommand.getMiniMessage();
        LiteralCommandNode<CommandSource> helpNode = BrigadierCommand.literalArgumentBuilder("help")
                .executes(context -> {
                    helpCommand.execute(context.getSource(), new String[0]);
                    return SINGLE_SUCCESS;
                })
                .build();
        LiteralCommandNode<CommandSource> reloadNode = BrigadierCommand.literalArgumentBuilder("reload")
                .requires(source -> source.hasPermission("vmonitor.reload"))
                .executes(context -> {
                    reloadCommand.execute(context.getSource(), new String[0]);
                    return SINGLE_SUCCESS;
                })
                .build();
        com.mojang.brigadier.suggestion.SuggestionProvider<CommandSource> serverSuggestionProvider = (context, builder) -> {
            CompletableFuture<List<String>> handlerSuggestions = listCommand.suggest(context.getSource(), new String[]{builder.getRemaining()});
            return handlerSuggestions.thenApply(suggestionsList -> {
                suggestionsList.forEach(builder::suggest);
                return builder.build();
            });
        };
        LiteralArgumentBuilder<CommandSource> serverListSubCommand = BrigadierCommand.literalArgumentBuilder("list");
        serverListSubCommand
                .then(BrigadierCommand.literalArgumentBuilder("all")
                        .executes(context -> {
                            listCommand.execute(context.getSource(), new String[0]);
                            return SINGLE_SUCCESS;
                        })
                )
                .then(BrigadierCommand.requiredArgumentBuilder("server_name", StringArgumentType.word())
                        .suggests(serverSuggestionProvider)
                        .executes(context -> {
                            String serverNameArg = context.getArgument("server_name", String.class);
                            listCommand.execute(context.getSource(), new String[]{serverNameArg});
                            return SINGLE_SUCCESS;
                        })
                )
                .executes(context -> {
                    context.getSource().sendMessage(mm.deserialize(lang.getMessage("usage-server-list")));
                    return SINGLE_SUCCESS;
                });
        LiteralArgumentBuilder<CommandSource> serverInfoSubCommand = BrigadierCommand.literalArgumentBuilder("info");
        serverInfoSubCommand
                .then(BrigadierCommand.literalArgumentBuilder("all")
                        .executes(context -> {
                            infoCommand.execute(context.getSource(), null);
                            return SINGLE_SUCCESS;
                        })
                )
                .then(BrigadierCommand.requiredArgumentBuilder("server_name", StringArgumentType.word())
                        .suggests(serverSuggestionProvider)
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
                .then(serverListSubCommand.build())
                .then(serverInfoSubCommand.build());
        LiteralCommandNode<CommandSource> vmonitorRootNode = BrigadierCommand.literalArgumentBuilder("vmonitor")
                .executes(context -> {
                    helpCommand.execute(context.getSource(), new String[0]);
                    return SINGLE_SUCCESS;
                })
                .then(helpNode)
                .then(reloadNode)
                .then(serverNodeBuilder.build())
                .build();
        CommandMeta vmonitorMeta = commandManager.metaBuilder("vmonitor")
                .aliases("vm")
                .build();
        commandManager.register(vmonitorMeta, new BrigadierCommand(vmonitorRootNode));
    }
}