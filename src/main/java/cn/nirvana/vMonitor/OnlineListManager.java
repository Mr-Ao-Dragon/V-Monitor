package cn.nirvana.vMonitor;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class OnlineListManager implements SimpleCommand {

    private final ProxyServer proxyServer;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage;

    public OnlineListManager(ProxyServer proxyServer, ConfigManager configManager, MiniMessage miniMessage) {
        this.proxyServer = proxyServer;
        this.configManager = configManager;
        this.miniMessage = miniMessage;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            String partialName = args[0].toLowerCase();
            return proxyServer.getAllServers().stream()
                    .map(RegisteredServer::getServerInfo)
                    .map(ServerInfo::getName)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (args.length > 0) {
            String serverNameArg = args[0];
            Optional<RegisteredServer> server = proxyServer.getServer(serverNameArg);
            String serverFormat = configManager.getMessage("list-server-format");
            if (server.isEmpty()) {
                source.sendMessage(miniMessage.deserialize(configManager.getMessage("server-not-exists").replace("{server}", serverNameArg)));
            } else {
                int playerCount = server.get().getPlayersConnected().size();
                String actualServerName = server.get().getServerInfo().getName();
                String displayServerName = configManager.getServerDisplayName(actualServerName);
                String combinedLine = serverFormat.replace("{server}", displayServerName).replace("{count}", String.valueOf(playerCount));
                source.sendMessage(miniMessage.deserialize(combinedLine));
                String playerNames = server.get().getPlayersConnected().stream()
                        .map(Player::getUsername)
                        .collect(Collectors.joining(", "));
                source.sendMessage(miniMessage.deserialize(playerNames));

            }
        } else {
            String totalFormat = configManager.getMessage("list-total-format");
            String emptyServerFormat = configManager.getMessage("list-no-players");
            String serverPlayersFormat = configManager.getMessage("list-server-players-format");
            int totalPlayerCount = proxyServer.getPlayerCount();
            String combinedLine = totalFormat.replace("{count}", String.valueOf(totalPlayerCount));
            source.sendMessage(miniMessage.deserialize(combinedLine));
            proxyServer.getAllServers().forEach(server -> {
                List<Player> players = server.getPlayersConnected().stream().collect(Collectors.toList());
                String serverName = server.getServerInfo().getName();
                String displayServerName = configManager.getServerDisplayName(serverName);
                if (players.isEmpty()) {
                    String emptyServerLine = emptyServerFormat
                            .replace("{server_display}", displayServerName);
                    source.sendMessage(miniMessage.deserialize(emptyServerLine));
                } else {
                    String playerNames = players.stream()
                            .map(Player::getUsername)
                            .collect(Collectors.joining(", "));
                    String serverPlayersLine = serverPlayersFormat
                            .replace("{server_display}", displayServerName)
                            .replace("{players}", playerNames);
                    source.sendMessage(miniMessage.deserialize(serverPlayersLine));
                }
            });
        }
    }
}