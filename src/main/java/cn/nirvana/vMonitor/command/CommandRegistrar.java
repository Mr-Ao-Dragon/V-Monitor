package cn.nirvana.vMonitor.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.stream.Collectors;

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
        LiteralCommandNode<CommandSource> listNode = BrigadierCommand.literalArgumentBuilder("list")
                .executes(context -> {
                    listCommand.execute(context.getSource(), new String[0]);
                    return SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.requiredArgumentBuilder("server", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            CompletableFuture<List<String>> handlerSuggestions = listCommand.suggest(context.getSource(), new String[]{builder.getRemaining()});
                            return handlerSuggestions.thenApply(suggestionsList -> {
                                suggestionsList.forEach(builder::suggest);
                                return builder.build();
                            });
                        })
                        .executes(context -> {
                            String serverNameArg = context.getArgument("server", String.class);
                            listCommand.execute(context.getSource(), new String[]{serverNameArg});
                            return SINGLE_SUCCESS;
                        })
                )
                .build();
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
        LiteralArgumentBuilder<CommandSource> infoBuilder = InfoCommand.createBrigadierCommand(this.infoCommand, this.proxyServer);
        LiteralCommandNode<CommandSource> infoNode = infoBuilder.build();
        LiteralCommandNode<CommandSource> vmonitorRootNode = BrigadierCommand.literalArgumentBuilder("vmonitor")
                .executes(context -> {
                    helpCommand.execute(context.getSource(), new String[0]);
                    return SINGLE_SUCCESS;
                })
                .then(listNode)
                .then(helpNode)
                .then(reloadNode)
                .then(infoNode)
                .build();
        CommandMeta vmonitorMeta = commandManager.metaBuilder("vmonitor")
                .aliases("vm")
                .build();
        commandManager.register(vmonitorMeta, new BrigadierCommand(vmonitorRootNode));
    }
}