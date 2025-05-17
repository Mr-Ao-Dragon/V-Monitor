package cn.nirvana.vMonitor;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

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
        return args.length == 1
                ? proxyServer.getAllServers().stream()
                .map(RegisteredServer::getServerInfo)
                .map(ServerInfo::getName)
                .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList())
                : List.of();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        if (args.length > 0) {
            String serverNameArg = args[0];
            Optional<RegisteredServer> server = proxyServer.getServer(serverNameArg);
            if (server.isEmpty()) {
                source.sendMessage(miniMessage.deserialize(configManager.getMessage("server-not-exists").replace("{server}", serverNameArg)));
                return;
            }
                source.sendMessage(miniMessage.deserialize(configManager.getMessage("list-server-format").replace("""
                        {server}""", configManager.getServerDisplayName(server.get().getServerInfo().getName())).replace("{count}",
                        String.valueOf(server.get().getPlayersConnected().size()))));
                source.sendMessage(miniMessage.deserialize(server.get().getPlayersConnected().stream()
                        .map(Player::getUsername)
                        .collect(Collectors.joining(", "))));
        } else {
            proxyServer.getAllServers().forEach(server -> {
                List<Player> players = server.getPlayersConnected().stream().toList();
                String displayServerName = configManager.getServerDisplayName(server.getServerInfo().getName());
                source.sendMessage(miniMessage.deserialize(players.isEmpty()
                        ? configManager.getMessage("list-no-players").replace("{server_display}", displayServerName)
                        : configManager.getMessage("list-server-players-format")
                        .replace("{server_display}", displayServerName)
                        .replace("{players}", players.stream()
                                .map(Player::getUsername)
                                .collect(Collectors.joining(", ")))));
            });
        }
    }
}