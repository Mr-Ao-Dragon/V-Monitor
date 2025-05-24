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
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class ListCommand {
    private final ProxyServer proxyServer;
    private final ConfigFileLoader configFileLoader;
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;
    private final CommandRegistrar commandRegistrar;

    public ListCommand(ProxyServer proxyServer, ConfigFileLoader configFileLoader, LanguageLoader languageLoader,
                       MiniMessage miniMessage, CommandRegistrar commandRegistrar) {
        this.proxyServer = proxyServer;
        this.configFileLoader = configFileLoader;
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
        this.commandRegistrar = commandRegistrar;
        registerCommands();
    }

    private void registerCommands() {
        commandRegistrar.registerServerSubCommand(serverNode -> {
            LiteralCommandNode<CommandSource> listNode = LiteralArgumentBuilder.<CommandSource>literal("list")
                    .executes(context -> {
                        // Default behavior for /vm server list (no arguments): show help message
                        context.getSource().sendMessage(miniMessage.deserialize(languageLoader.getMessage("server-list-help")));
                        return SINGLE_SUCCESS;
                    })
                    .build();

            // Add 'all' argument for /vm server list all
            listNode.addChild(LiteralArgumentBuilder.<CommandSource>literal("all")
                    .executes(context -> {
                        executeListAll(context.getSource()); // Call a specific method for 'all'
                        return SINGLE_SUCCESS;
                    })
                    .build()
            );

            // Re-add server name argument for /vm server list <server_name>
            listNode.addChild(RequiredArgumentBuilder.<CommandSource, String>argument("server", word())
                    .suggests(new ServerNameSuggestionProvider(proxyServer)) // Suggest server names
                    .executes(context -> {
                        executeListPlayersOnServer(context.getSource(), StringArgumentType.getString(context, "server"));
                        return SINGLE_SUCCESS;
                    })
                    .build()
            );

            serverNode.addChild(listNode);
        });
    }

    private void executeListAll(CommandSource source) {
        String formatHeader = languageLoader.getMessage("list-all-header");
        if (formatHeader != null && !formatHeader.isEmpty()) {
            source.sendMessage(miniMessage.deserialize(formatHeader));
        }

        proxyServer.getAllServers().forEach(registeredServer -> {
            String serverName = registeredServer.getServerInfo().getName();
            String serverDisplayName = configFileLoader.getServerDisplayName(serverName);
            Collection<Player> players = registeredServer.getPlayersConnected();

            String entryFormat = languageLoader.getMessage("list-all-entry");

            String playersCount = String.valueOf(players.size());
            String filledEntry = entryFormat
                    .replace("{server}", serverDisplayName)
                    .replace("{players}", playersCount);

            Component serverPrefixComponent = miniMessage.deserialize(filledEntry);
            source.sendMessage(serverPrefixComponent);
        });
    }

    // This method handles /vm server list <server_name>
    private void executeListPlayersOnServer(CommandSource source, String serverNameArg) {
        Optional<RegisteredServer> targetServer = proxyServer.getServer(serverNameArg);
        if (targetServer.isPresent()) {
            RegisteredServer registeredServer = targetServer.get();
            Collection<Player> players = registeredServer.getPlayersConnected();
            if (players.isEmpty()) {
                source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("no-players-on-server").replace("{server}", configFileLoader.getServerDisplayName(serverNameArg))));
            } else {
                String header = languageLoader.getMessage("list-server-players-header").replace("{server}", configFileLoader.getServerDisplayName(serverNameArg));
                source.sendMessage(miniMessage.deserialize(header));
                // Build a list of player names, e.g., "Player1, Player2, Player3"
                String playerNames = players.stream()
                        .map(Player::getUsername)
                        .collect(Collectors.joining(", "));

                source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("list-server-player-entry").replace("{players_list}", playerNames)));
            }
        } else {
            source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("server-not-found").replace("{server}", serverNameArg)));
        }
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