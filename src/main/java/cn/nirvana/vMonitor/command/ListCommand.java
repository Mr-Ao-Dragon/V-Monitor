package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.config.ConfigFileLoader;
import cn.nirvana.vMonitor.config.LanguageLoader;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class ListCommand {
    private final ProxyServer proxyServer;
    private final ConfigFileLoader configFileLoader;
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;

    public ListCommand(ProxyServer proxyServer, ConfigFileLoader configFileLoader, LanguageLoader languageLoader, MiniMessage miniMessage) {
        this.proxyServer = proxyServer;
        this.configFileLoader = configFileLoader;
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
    }

    public void execute(CommandSource source, String[] args) {
        if (args.length > 0) {
            String serverNameArg = args[0];
            Optional<RegisteredServer> server = proxyServer.getServer(serverNameArg);
            if (server.isEmpty()) {
                source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("server-not-exists").replace("{server}", serverNameArg)));
                return;
            }
            RegisteredServer targetServer = server.get();
            List<Player> players = targetServer.getPlayersConnected().stream().toList();
            String displayServerName = configFileLoader.getServerDisplayName(targetServer.getServerInfo().getName());
            String actualServerName = targetServer.getServerInfo().getName();
            int onlinePlayersOnServer = players.size();
            String serverTitleFormat = languageLoader.getMessage("list-server-format")
                    .replace("{server}", actualServerName)
                    .replace("{server_player}", String.valueOf(onlinePlayersOnServer));
            source.sendMessage(miniMessage.deserialize(serverTitleFormat));
            String serverNameFormat = languageLoader.getMessage("list-server-name");
            Component serverPrefixComponent = miniMessage.deserialize(serverNameFormat.replace("{server_display}", displayServerName)).append(Component.text(": "));
            if (players.isEmpty()) {
                String noPlayersText = languageLoader.getMessage("list-no-players");
                source.sendMessage(serverPrefixComponent.append(miniMessage.deserialize(noPlayersText)));
            } else {
                String playersFormat = languageLoader.getMessage("list-server-player");
                String playersString = players.stream()
                        .map(Player::getUsername)
                        .collect(Collectors.joining(", "));
                String playerListText = playersFormat.replace("{players}", playersString);
                source.sendMessage(serverPrefixComponent.append(miniMessage.deserialize(playerListText)));
            }
        } else {
            long totalOnlinePlayers = proxyServer.getAllServers().stream()
                    .mapToLong(server -> server.getPlayersConnected().size())
                    .sum();
            String totalFormatMessage = languageLoader.getMessage("list-total-format")
                    .replace("{total_player}", String.valueOf(totalOnlinePlayers));
            source.sendMessage(miniMessage.deserialize(totalFormatMessage));
            proxyServer.getAllServers().forEach(server -> {
                List<Player> players = server.getPlayersConnected().stream().toList();
                String displayServerName = configFileLoader.getServerDisplayName(server.getServerInfo().getName());
                String serverNameFormat = languageLoader.getMessage("list-server-name");
                Component serverPrefixComponent = miniMessage.deserialize(serverNameFormat.replace("{server_display}", displayServerName)).append(Component.text(": "));
                if (players.isEmpty()) {
                    String noPlayersText = languageLoader.getMessage("list-no-players");
                    source.sendMessage(serverPrefixComponent.append(miniMessage.deserialize(noPlayersText)));
                } else {
                    String playersFormat = languageLoader.getMessage("list-server-player");
                    String playersString = players.stream()
                            .map(Player::getUsername)
                            .collect(Collectors.joining(", "));
                    String playerListText = playersFormat.replace("{players}", playersString);
                    source.sendMessage(serverPrefixComponent.append(miniMessage.deserialize(playerListText)));
                }
            });
        }
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
}