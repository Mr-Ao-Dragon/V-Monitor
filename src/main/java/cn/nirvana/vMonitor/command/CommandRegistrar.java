package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.ConfigManager;
import cn.nirvana.vMonitor.OnlineListManager;
import cn.nirvana.vMonitor.VMonitor;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.command.SimpleCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class CommandRegistrar {

    private final ProxyServer proxyServer;
    private final CommandManager commandManager;
    private final ConfigManager configManager;
    private final OnlineListManager onlineListManager;
    private final HelpCommand helpCommand;

    public CommandRegistrar(ProxyServer proxyServer, CommandManager commandManager,
                            ConfigManager configManager, OnlineListManager onlineListManager, HelpCommand helpCommand) {
        this.proxyServer = proxyServer;
        this.commandManager = commandManager;
        this.configManager = configManager;
        this.onlineListManager = onlineListManager;
        this.helpCommand = helpCommand;
    }

    public void registerCommands() {
        LiteralCommandNode<CommandSource> vmonitorRootNode = BrigadierCommand.literalArgumentBuilder("vmonitor")
                .executes(context -> {
                    helpCommand.execute(new SimpleCommandInvocation(context.getSource(), new String[0], "vmonitor help"));
                    return SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.literalArgumentBuilder("list")
                        .executes(context -> {
                            onlineListManager.execute(new SimpleCommandInvocation(context.getSource(), new String[0], "vmonitor list"));
                            return SINGLE_SUCCESS;
                        })
                        .then(BrigadierCommand.requiredArgumentBuilder("server", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String partialServerName = builder.getRemaining().toLowerCase();
                                    proxyServer.getAllServers().stream()
                                            .map(RegisteredServer::getServerInfo)
                                            .map(ServerInfo::getName)
                                            .filter(name -> name.toLowerCase().startsWith(partialServerName))
                                            .forEach(builder::suggest);
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String serverName = context.getArgument("server", String.class);
                                    onlineListManager.execute(new SimpleCommandInvocation(context.getSource(), new String[]{serverName}, "vmonitor list"));
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(BrigadierCommand.literalArgumentBuilder("reload")
                        .requires(source -> source.hasPermission("vmonitor.reload"))
                        .executes(context -> {
                            configManager.execute(new SimpleCommandInvocation(context.getSource(), new String[0], "vmonitor reload"));
                            return SINGLE_SUCCESS;
                        })
                )
                .then(BrigadierCommand.literalArgumentBuilder("help")
                        .executes(context -> {
                            helpCommand.execute(new SimpleCommandInvocation(context.getSource(), new String[0], "vmonitor help"));
                            return SINGLE_SUCCESS;
                        })
                )
                .build();
        CommandMeta vmonitorMeta = commandManager.metaBuilder("vmonitor")
                .aliases("vm")
                .build();
        commandManager.register(vmonitorMeta, new BrigadierCommand(vmonitorRootNode));
    }

    private static class SimpleCommandInvocation implements com.velocitypowered.api.command.SimpleCommand.Invocation {
        private final CommandSource source;
        private final String[] arguments;
        private final String alias;

        public SimpleCommandInvocation(CommandSource source, String[] arguments, String alias) {
            this.source = source;
            this.arguments = arguments;
            this.alias = alias;
        }

        @Override
        public CommandSource source() {
            return source;
        }

        @Override
        public String alias() {
            return alias;
        }

        @Override
        public String[] arguments() {
            return arguments;
        }
    }
}