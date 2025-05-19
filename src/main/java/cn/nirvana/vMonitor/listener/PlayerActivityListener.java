package cn.nirvana.vMonitor.listener;

import cn.nirvana.vMonitor.config.ConfigFileLoader;
import cn.nirvana.vMonitor.config.LanguageLoader;
import cn.nirvana.vMonitor.config.PlayerDataLoader;
import cn.nirvana.vMonitor.config.PlayerDataLoader.PlayerFirstJoinInfo;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.minimessage.MiniMessage;

import com.moandjiezana.toml.Toml;

import java.util.UUID;

public class PlayerActivityListener {
    private final ProxyServer proxyServer;
    private final ConfigFileLoader configFileLoader;
    private final LanguageLoader languageLoader;
    private final PlayerDataLoader playerDataLoader;
    private final MiniMessage miniMessage;

    public PlayerActivityListener(ProxyServer proxyServer, ConfigFileLoader configFileLoader, LanguageLoader languageLoader, PlayerDataLoader playerDataLoader, MiniMessage miniMessage) {
        this.proxyServer = proxyServer;
        this.configFileLoader = configFileLoader;
        this.languageLoader = languageLoader;
        this.playerDataLoader = playerDataLoader;
        this.miniMessage = miniMessage;
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String playerName = player.getUsername();
        boolean isFirstJoin = !playerDataLoader.hasPlayerJoinedBefore(uuid);
        playerDataLoader.addPlayerFirstJoinInfo(uuid, playerName);
        Toml config = configFileLoader.getConfig();
        if (isFirstJoin) {
            String firstJoinMessage = config.getString("messages.first-join");
            if (firstJoinMessage != null && !firstJoinMessage.isEmpty()) {
                proxyServer.sendMessage(miniMessage.deserialize(firstJoinMessage.replace("{player}", playerName)));
            }
            String joinMessage = config.getString("messages.join");
            if (joinMessage != null && !joinMessage.isEmpty()) {
                proxyServer.sendMessage(miniMessage.deserialize(joinMessage.replace("{player}", playerName)));
            }
        } else {
            String joinMessage = config.getString("messages.join");
            if (joinMessage != null && !joinMessage.isEmpty()) {
                proxyServer.sendMessage(miniMessage.deserialize(joinMessage.replace("{player}", playerName)));
            }
        }
    }

    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        Toml config = configFileLoader.getConfig();
        String leaveMessage = config.getString("messages.leave");
        if (leaveMessage != null && !leaveMessage.isEmpty()) {
            proxyServer.sendMessage(miniMessage.deserialize(leaveMessage.replace("{player}", player.getUsername())));
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String fromServer = event.getPreviousServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse(languageLoader.getMessage("unknown-server"));
        String toServer = event.getServer().getServerInfo().getName();
        Toml config = configFileLoader.getConfig();
        String switchMessage = config.getString("messages.switch");
        if (switchMessage != null && !switchMessage.isEmpty()) {
            String displayFromServer = configFileLoader.getServerDisplayName(fromServer);
            String displayToServer = configFileLoader.getServerDisplayName(toServer);
            proxyServer.sendMessage(miniMessage.deserialize(switchMessage
                    .replace("{player}", player.getUsername())
                    .replace("{from}", displayFromServer)
                    .replace("{to}", displayToServer)));
        }
    }
}