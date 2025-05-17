package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.ConfigManager;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class HelpCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public HelpCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        Map<String, Object> language = configManager.getLanguage();

        Object helpObj = language.get("help");
        List<String> helpMessages = new ArrayList<>();

        if (helpObj instanceof List<?>) {
            List<?> rawList = (List<?>) helpObj;
            for (Object obj : rawList) {
                if (obj instanceof String) {
                    helpMessages.add((String) obj);
                }
            }

            if (!helpMessages.isEmpty()) {
                for (String line : helpMessages) {
                    source.sendMessage(miniMessage.deserialize(line));
                }
                return;
            }
        }

        source.sendMessage(miniMessage.deserialize("<red>No help message configured.</red>"));
    }


    @Override
    public List<String> suggest(Invocation invocation) {
        return Arrays.asList();
    }
}