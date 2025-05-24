package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.config.ConfigFileLoader;
import cn.nirvana.vMonitor.config.LanguageLoader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class InfoCommand {
    private final ProxyServer proxyServer;
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;
    private final ConfigFileLoader configFileLoader;
    private final CommandRegistrar commandRegistrar;

    public InfoCommand(ProxyServer proxyServer, LanguageLoader languageLoader, MiniMessage miniMessage,
                       ConfigFileLoader configFileLoader, CommandRegistrar commandRegistrar) {
        this.proxyServer = proxyServer;
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
        this.configFileLoader = configFileLoader;
        this.commandRegistrar = commandRegistrar;
        registerCommands();
    }

    private void registerCommands() {
        commandRegistrar.registerServerSubCommand(serverNode -> {
            LiteralCommandNode<CommandSource> infoNode = LiteralArgumentBuilder.<CommandSource>literal("info")
                    .executes(context -> {
                        context.getSource().sendMessage(miniMessage.deserialize(languageLoader.getMessage("server-info-help")));
                        return SINGLE_SUCCESS;
                    })
                    .build();

            infoNode.addChild(LiteralArgumentBuilder.<CommandSource>literal("all")
                    .executes(context -> {
                        executeInfoAll(context.getSource());
                        return SINGLE_SUCCESS;
                    })
                    .build()
            );

            infoNode.addChild(RequiredArgumentBuilder.<CommandSource, String>argument("server", word())
                    .suggests(new ServerNameSuggestionProvider(proxyServer))
                    .executes(context -> {
                        executeInfoSingleServer(context.getSource(), StringArgumentType.getString(context, "server"));
                        return SINGLE_SUCCESS;
                    })
                    .build()
            );

            serverNode.addChild(infoNode);
        });
    }

    private void executeInfoSingleServer(CommandSource source, String serverNameArg) {
        Optional<RegisteredServer> optionalServer = proxyServer.getServer(serverNameArg);
        if (optionalServer.isPresent()) {
            RegisteredServer registeredServer = optionalServer.get();
            String serverDisplayName = configFileLoader.getServerDisplayName(registeredServer.getServerInfo().getName());

            registeredServer.ping().whenComplete((pingResult, throwable) -> {
                if (throwable != null || pingResult == null) {
                    source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("server-ping-failed").replace("{server}", serverDisplayName)));
                    return;
                }

                String motd = PlainTextComponentSerializer.plainText().serialize(pingResult.getDescriptionComponent());
                String version = pingResult.getVersion() != null ? pingResult.getVersion().getName() : languageLoader.getMessage("unknown-version");

                int onlinePlayers = 0;
                int maxPlayers = 0;
                Optional<ServerPing.Players> playersOptional = pingResult.getPlayers();
                if (playersOptional.isPresent()) {
                    ServerPing.Players players = playersOptional.get();
                    onlinePlayers = players.getOnline();
                    maxPlayers = players.getMax();
                }

                String infoMessage = languageLoader.getMessage("info-server-format")
                        .replace("{server}", serverDisplayName)
                        .replace("{motd}", motd)
                        .replace("{version}", version)
                        .replace("{online_players}", String.valueOf(onlinePlayers))
                        .replace("{max_players}", String.valueOf(maxPlayers));

                source.sendMessage(miniMessage.deserialize(infoMessage));
            }).exceptionally(ex -> {
                source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("server-ping-failed").replace("{server}", serverDisplayName)));
                return null;
            });
        } else {
            source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("server-not-found").replace("{server}", serverNameArg)));
        }
    }

    private void executeInfoAll(CommandSource source) {
        String header = languageLoader.getMessage("info-all-header");
        if (header != null && !header.isEmpty()) {
            source.sendMessage(miniMessage.deserialize(header));
        }

        List<CompletableFuture<Void>> futures = proxyServer.getAllServers().stream()
                .map(registeredServer -> {
                    String serverName = registeredServer.getServerInfo().getName();
                    String serverDisplayName = configFileLoader.getServerDisplayName(serverName);

                    // Pinging each server and processing the result for display
                    // The .thenAccept() method is key here, as it processes the result
                    // but returns CompletableFuture<Void>, which matches our List type.
                    return registeredServer.ping()
                            .thenAccept(pingResult -> {
                                String statusKey = "status-offline";
                                int playersOnline = 0;
                                if (pingResult != null) { // If ping was successful
                                    statusKey = "status-online";
                                    Optional<ServerPing.Players> playersOptional = pingResult.getPlayers();
                                    if (playersOptional.isPresent()) {
                                        playersOnline = playersOptional.get().getOnline();
                                    }
                                }

                                String entryFormat = languageLoader.getMessage("info-server-status-entry");
                                String filledEntry = entryFormat
                                        .replace("{server}", serverDisplayName)
                                        .replace("{status}", languageLoader.getMessage(statusKey))
                                        .replace("{online_players}", String.valueOf(playersOnline));
                                source.sendMessage(miniMessage.deserialize(filledEntry));
                            })
                            .exceptionally(ex -> {
                                // If ping fails (exception occurs), send offline message
                                String entryFormat = languageLoader.getMessage("info-server-status-entry");
                                String filledEntry = entryFormat
                                        .replace("{server}", serverDisplayName)
                                        .replace("{status}", languageLoader.getMessage("status-offline"))
                                        .replace("{online_players}", "0");
                                source.sendMessage(miniMessage.deserialize(filledEntry));
                                return null; // exceptionally also returns CompletableFuture<Void> if its function returns null
                            });
                })
                .collect(Collectors.toList());

        // Wait for all pings to complete. This is useful for knowing when all messages have been sent.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, throwable) -> {
                    // All pings and their message sending logic are complete.
                    // You could add a final "footer" message here if needed.
                });
    }

    static class ServerNameSuggestionProvider implements SuggestionProvider<CommandSource> {
        private final ProxyServer proxyServer;

        public ServerNameSuggestionProvider(ProxyServer proxyServer) {
            this.proxyServer = proxyServer;
        }

        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
            String remaining = builder.getRemaining().toLowerCase();
            proxyServer.getAllServers().stream()
                    .map(server -> server.getServerInfo().getName())
                    .filter(name -> name.toLowerCase().startsWith(remaining))
                    .sorted()
                    .forEach(builder::suggest);
            return builder.buildFuture();
        }
    }
}