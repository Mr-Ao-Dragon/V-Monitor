package cn.nirvana.vMonitor;

import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.minimessage.MiniMessage;

import com.moandjiezana.toml.Toml;

import java.util.HashMap;
import java.util.UUID;

public class PlayerActivityListener {

    private final ProxyServer proxyServer;
    private final ConfigManager configManager;
    private final MiniMessage miniMessage;
    private final HashMap<UUID, PlayerFirstJoinInfo> playerData;

    public static class PlayerFirstJoinInfo {
        public long firstJoinTime;
        public String playerName;
        public PlayerFirstJoinInfo(long firstJoinTime, String playerName) {
            this.firstJoinTime = firstJoinTime;
            this.playerName = playerName;
        }
    }

    public PlayerActivityListener(ProxyServer proxyServer, ConfigManager configManager, MiniMessage miniMessage, HashMap<UUID, PlayerFirstJoinInfo> playerData) {
        this.proxyServer = proxyServer;
        this.configManager = configManager;
        this.miniMessage = miniMessage;
        this.playerData = playerData;
    }

    public void onPlayerJoin(LoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        long joinTime = System.currentTimeMillis();
        Toml config = configManager.getConfig();
        if (!playerData.containsKey(uuid)) {
            playerData.put(uuid, new PlayerFirstJoinInfo(joinTime, player.getUsername()));
            configManager.savePlayerData();
            String firstJoinMessage = config.getString("messages.first-join");
            if (firstJoinMessage != null && !firstJoinMessage.isEmpty()) {
                proxyServer.sendMessage(miniMessage.deserialize(firstJoinMessage.replace("{player}", player.getUsername())));
            }
            String joinMessage = config.getString("messages.join");
            if (joinMessage != null && !joinMessage.isEmpty()) {
                proxyServer.sendMessage(miniMessage.deserialize(joinMessage.replace("{player}", player.getUsername())));
            }
        } else {
            PlayerFirstJoinInfo existingData = playerData.get(uuid);
            if (!existingData.playerName.equals(player.getUsername())) {
                existingData.playerName = player.getUsername();
                configManager.savePlayerData();
            }
            String joinMessage = config.getString("messages.join");
            if (joinMessage != null && !joinMessage.isEmpty()) {
                proxyServer.sendMessage(miniMessage.deserialize(joinMessage.replace("{player}", player.getUsername())));
            }
        }
    }

    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        Toml config = configManager.getConfig();
        String leaveMessage = config.getString("messages.leave");
        if (leaveMessage != null && !leaveMessage.isEmpty()) {
            proxyServer.sendMessage(miniMessage.deserialize(leaveMessage.replace("{player}", player.getUsername())));
        }
    }

    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String fromServer = event.getPreviousServer()
                .map(serverConnection -> serverConnection.getServerInfo().getName())
                .orElse("Unknown");
        String toServer = event.getServer().getServerInfo().getName();
        Toml config = configManager.getConfig();
        String switchMessage = config.getString("messages.switch");
        if (switchMessage != null && !switchMessage.isEmpty()) {
            proxyServer.sendMessage(miniMessage.deserialize(switchMessage
                    .replace("{player}", player.getUsername())
                    .replace("{from}", fromServer)
                    .replace("{to}", toServer)));
        }
    }
}