package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.config.LanguageLoader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription; // Make sure this is imported
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional; // Added import for Optional
import java.util.stream.Collectors; // Added import for Collectors

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class PluginListCommand {
    private final ProxyServer proxyServer;
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;
    private final CommandRegistrar commandRegistrar;

    public PluginListCommand(ProxyServer proxyServer, LanguageLoader languageLoader,
                             MiniMessage miniMessage, CommandRegistrar commandRegistrar) {
        this.proxyServer = proxyServer;
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
        this.commandRegistrar = commandRegistrar;
        registerPluginListCommand();
    }

    private void registerPluginListCommand() {
        LiteralCommandNode<CommandSource> listNode = BrigadierCommand.literalArgumentBuilder("list")
                .requires(source -> source.hasPermission("vmonitor.plugin.list"))
                .executes(context -> {
                    execute(context.getSource());
                    return SINGLE_SUCCESS;
                })
                .build();

        commandRegistrar.registerPluginSubCommand(pluginNode -> {
            pluginNode.addChild(listNode);
        });
    }

    public void execute(CommandSource source) {
        // Fix 1: Use getPluginManager().getPlugins() to get all plugins
        List<PluginContainer> plugins = new ArrayList<>(proxyServer.getPluginManager().getPlugins());
        plugins.sort(Comparator.comparing(p -> p.getDescription().getName().orElse(p.getDescription().getId())));

        String pluginListFormat = languageLoader.getMessage("plugin-list-format");
        String pluginListEntryFormat = languageLoader.getMessage("plugin-list-entry-format");
        String pluginListHoverFormat = languageLoader.getMessage("plugin-list-hover-format");

        StringBuilder pluginEntries = new StringBuilder();

        for (PluginContainer plugin : plugins) {
            PluginDescription description = plugin.getDescription(); // This is correct
            String id = description.getId();
            // Fix 2: Handle Optional<String> correctly for name and version
            String name = description.getName().orElse(id);
            String version = description.getVersion().orElse(languageLoader.getMessage("unknown-version"));
            String url = description.getUrl().orElse(languageLoader.getMessage("no-url"));
            String descriptionText = description.getDescription().orElse(languageLoader.getMessage("no-description"));
            String authors = String.join(", ", description.getAuthors()); // getAuthors returns Collection<String>
            if (authors.isEmpty()) {
                authors = languageLoader.getMessage("no-authors");
            }

            String entryLine = pluginListEntryFormat
                    .replace("{name}", name)
                    .replace("{version}", version);

            String filledHoverText = pluginListHoverFormat
                    .replace("{id}", id)
                    .replace("{name}", name)
                    .replace("{version}", version)
                    .replace("{url}", url)
                    .replace("{description}", descriptionText)
                    .replace("{authors}", authors);

            Component finalEntryComponent = miniMessage.deserialize(entryLine)
                    .hoverEvent(HoverEvent.showText(miniMessage.deserialize(filledHoverText)))
                    .clickEvent(ClickEvent.runCommand("/vmonitor plugin info " + id));

            pluginEntries.append(miniMessage.serialize(finalEntryComponent)).append("\n");
        }

        String finalMessage = pluginListFormat
                .replace("{count}", String.valueOf(plugins.size()))
                .replace("{plugin_entries}", pluginEntries.toString().trim());

        source.sendMessage(miniMessage.deserialize(finalMessage));
    }
}