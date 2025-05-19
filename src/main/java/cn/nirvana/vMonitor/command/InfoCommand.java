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

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.proxy.server.ServerPing.Players;
import com.velocitypowered.api.proxy.server.ServerPing.Version;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.command.BrigadierCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class InfoCommand {
    private final ProxyServer proxyServer;
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;
    private final ConfigFileLoader configFileLoader;

    public InfoCommand(ProxyServer proxyServer, LanguageLoader languageLoader, MiniMessage miniMessage, ConfigFileLoader configFileLoader) {
        this.proxyServer = proxyServer;
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
        this.configFileLoader = configFileLoader;
    }

    public void execute(CommandSource source, String serverNameArg) {
        source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("info-pinging")));
        if (serverNameArg == null) {
            displayProxyInfo(source);
        } else {
            displayServerInfo(source, serverNameArg);
        }
    }

    private void displayProxyInfo(CommandSource source) {
        String proxyFormat = languageLoader.getMessage("info-proxy-format");
        long totalOnlinePlayers = proxyServer.getAllServers().stream()
                .mapToLong(server -> server.getPlayersConnected().size())
                .sum();
        int registeredServerCount = proxyServer.getAllServers().size();
        Map<RegisteredServer, CompletableFuture<Optional<ServerPing>>> pingFuturesMap = new HashMap<>();
        for (RegisteredServer server : proxyServer.getAllServers()) {
            pingFuturesMap.put(server, server.ping().exceptionally(e -> null).thenApply(Optional::ofNullable));
        }
        CompletableFuture.allOf(pingFuturesMap.values().toArray(new CompletableFuture[0]))
                .thenAccept(ignored -> {
                    long runningServers = pingFuturesMap.values().stream()
                            .map(CompletableFuture::join)
                            .filter(Optional::isPresent)
                            .count();
                    long offlineServers = pingFuturesMap.values().stream()
                            .map(CompletableFuture::join)
                            .filter(Optional::isEmpty)
                            .count();
                    StringBuilder serverListBuilder = new StringBuilder();
                    String serverLineFormat = languageLoader.getMessage("info-individual-server-format");
                    for (RegisteredServer server : proxyServer.getAllServers()) {
                        Optional<ServerPing> pingResultOptional = pingFuturesMap.get(server).join();
                        String actualServerName = server.getServerInfo().getName();
                        String displayServerName = configFileLoader.getServerDisplayName(actualServerName);
                        String statusText;
                        String onlinePlayersText;
                        if (pingResultOptional.isPresent()) {
                            statusText = languageLoader.getMessage("info-server-status-running-short");
                            ServerPing pingResult = pingResultOptional.get();
                            Optional<Players> playersOptional = pingResult.getPlayers();
                            onlinePlayersText = playersOptional.isPresent() ? String.valueOf(playersOptional.get().getOnline()) : String.valueOf(server.getPlayersConnected().size());
                        } else {
                            statusText = languageLoader.getMessage("info-server-status-offline-short");
                            onlinePlayersText = "0";
                        }
                        String serverLine = serverLineFormat
                                .replace("{server}", actualServerName)
                                .replace("{display_name}", displayServerName)
                                .replace("{status}", statusText)
                                .replace("{online_players}", onlinePlayersText);
                        serverListBuilder.append(serverLine).append("\n");
                    }
                    String serverListString = serverListBuilder.toString();
                    if (!serverListString.isEmpty()) {
                        serverListString = serverListString.substring(0, serverListString.length() - 1);
                    }
                    String formattedProxyInfo = proxyFormat
                            .replace("{proxy_version}", proxyServer.getVersion().getVersion())
                            .replace("{total_player}", String.valueOf(totalOnlinePlayers))
                            .replace("{server_count}", String.valueOf(registeredServerCount))
                            .replace("{running_servers}", String.valueOf(runningServers))
                            .replace("{offline_servers}", String.valueOf(offlineServers))
                            .replace("{server_status_list}", serverListString);
                    source.sendMessage(miniMessage.deserialize(formattedProxyInfo));
                }).exceptionally(e -> {
                    source.sendMessage(miniMessage.deserialize("<red>Error getting server info summary.</red>"));
                    return null;
                });
    }

    private void displayServerInfo(CommandSource source, String serverNameArg) {
        Optional<RegisteredServer> server = proxyServer.getServer(serverNameArg);
        if (server.isEmpty()) {
            source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("server-not-exists").replace("{server}", serverNameArg)));
            return;
        }
        RegisteredServer targetServer = server.get();
        String actualServerName = targetServer.getServerInfo().getName();
        targetServer.ping().thenAccept(pingResult -> {
            String serverDetailFormat = languageLoader.getMessage("info-server-detail-format");
            Version version = pingResult.getVersion();
            Optional<Players> playersOptional = pingResult.getPlayers();
            Component description = pingResult.getDescriptionComponent();
            String unknownText = languageLoader.getMessage("unknown-info");
            String coreName = unknownText;
            String serverVersion = (version != null) ? version.getName() : unknownText;
            String modSupportStatus = unknownText;
            String onlinePlayers = (playersOptional != null && playersOptional.isPresent()) ? String.valueOf(playersOptional.get().getOnline()) : unknownText;
            String maxPlayers = (playersOptional != null && playersOptional.isPresent()) ? String.valueOf(playersOptional.get().getMax()) : unknownText;
            String motd = (description != null) ? PlainTextComponentSerializer.plainText().serialize(description) : unknownText;
            String formattedServerDetail = serverDetailFormat
                    .replace("{server_name}", actualServerName)
                    .replace("{core_name}", coreName)
                    .replace("{server_version}", serverVersion)
                    .replace("{mod_support_status}", modSupportStatus)
                    .replace("{online_players}", onlinePlayers)
                    .replace("{max_players}", maxPlayers)
                    .replace("{motd}", motd);

            source.sendMessage(miniMessage.deserialize(formattedServerDetail));
        }).exceptionally(e -> {
            source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("info-server-unreachable").replace("{server}", actualServerName)));
            return null;
        });
    }

    public CompletableFuture<List<String>> suggest(CommandSource source, String[] args) {
        if (args.length == 1) {
            String partialServerName = args[0].toLowerCase();
            List<String> suggestions = proxyServer.getAllServers().stream()
                    .map(registeredServer -> registeredServer.getServerInfo().getName())
                    .filter(name -> name.toLowerCase().startsWith(partialServerName))
                    .collect(Collectors.toList());
            return CompletableFuture.completedFuture(suggestions);
        }
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    public static LiteralArgumentBuilder<CommandSource> createBrigadierCommand(InfoCommand infoCommandParam, ProxyServer proxyServer) {
        final InfoCommand finalInfoCommand = infoCommandParam;
        LiteralArgumentBuilder<CommandSource> infoLiteral = BrigadierCommand.literalArgumentBuilder("info");
        RequiredArgumentBuilder<CommandSource, String> serverArgument = BrigadierCommand.requiredArgumentBuilder("server", StringArgumentType.word())
                .suggests(new SuggestionProvider<CommandSource>() {
                    @Override
                    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
                        CompletableFuture<List<String>> handlerSuggestions = finalInfoCommand.suggest(context.getSource(), new String[]{builder.getRemaining()});
                        return handlerSuggestions.thenApply(suggestionsList -> {
                            suggestionsList.forEach(builder::suggest);
                            return builder.build();
                        });
                    }
                })
                .executes(context -> {
                    String serverNameArg = context.getArgument("server", String.class);
                    finalInfoCommand.execute(context.getSource(), serverNameArg);
                    return SINGLE_SUCCESS;
                });
        infoLiteral.then(serverArgument);
        infoLiteral.executes(context -> {
            finalInfoCommand.execute(context.getSource(), null);
            return SINGLE_SUCCESS;
        });
        return infoLiteral;
    }
}