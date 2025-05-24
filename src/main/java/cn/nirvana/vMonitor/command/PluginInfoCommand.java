package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.config.LanguageLoader;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import com.velocitypowered.api.command.BrigadierCommand;

public class PluginInfoCommand {
    private final ProxyServer proxyServer;
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;

    public PluginInfoCommand(ProxyServer proxyServer, LanguageLoader languageLoader, MiniMessage miniMessage) {
        this.proxyServer = proxyServer;
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
    }

    /**
     * Executes the 'plugin info <plugin_id>' command.
     * Displays detailed information about a specific plugin or all plugins.
     *
     * @param source   The command source.
     * @param pluginId The ID of the plugin to query, or "all".
     */
    public void execute(CommandSource source, String pluginId) {
        if (source instanceof com.velocitypowered.api.proxy.Player && !source.hasPermission("vmonitor.plugin.info")) {
            source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("no-permission")));
            return;
        }

        if ("all".equalsIgnoreCase(pluginId)) {
            displayAllPluginInfo(source);
            return;
        }

        Optional<PluginContainer> pluginOpt = proxyServer.getPluginManager().getPlugin(pluginId);

        if (!pluginOpt.isPresent()) {
            source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("plugin-info-not-found").replace("{plugin_id}", pluginId)));
            return;
        }

        PluginContainer plugin = pluginOpt.get();
        PluginDescription description = plugin.getDescription();

        // 获取整体的插件信息格式
        String pluginInfoFormat = languageLoader.getMessage("plugin-info-format");
        if (pluginInfoFormat == null || pluginInfoFormat.isEmpty() || pluginInfoFormat.startsWith("<red>Missing Language Key:")) {
            source.sendMessage(miniMessage.deserialize("<red>Error: Missing or invalid 'plugin-info-format' in language file.</red>"));
            return;
        }

        String id = description.getId();
        String name = description.getName().orElse(id);
        String version = description.getVersion().orElse(languageLoader.getMessage("unknown-version"));
        String url = description.getUrl().orElse("");
        String desc = description.getDescription().orElse("");
        String authors = String.join(", ", description.getAuthors());

        String finalMessage = pluginInfoFormat
                .replace("{plugin_name}", name)
                .replace("{id}", id)
                .replace("{name}", name) // 即使名称是可选的，也提供替换，如果为空则为空字符串
                .replace("{version}", version)
                .replace("{url}", url)
                .replace("{description}", desc)
                .replace("{authors}", authors);

        source.sendMessage(miniMessage.deserialize(finalMessage));
    }

    /**
     * Displays detailed information for all loaded plugins.
     *
     * @param source The command source.
     */
    private void displayAllPluginInfo(CommandSource source) {
        List<PluginContainer> plugins = new ArrayList<>(proxyServer.getPluginManager().getPlugins());
        if (plugins.isEmpty()) {
            source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("plugin-list-empty")));
            return;
        }

        plugins.sort(Comparator.comparing(p -> p.getDescription().getName().orElse(p.getDescription().getId()).toLowerCase()));

        String pluginInfoFormat = languageLoader.getMessage("plugin-info-format");
        if (pluginInfoFormat == null || pluginInfoFormat.isEmpty() || pluginInfoFormat.startsWith("<red>Missing Language Key:")) {
            source.sendMessage(miniMessage.deserialize("<red>Error: Missing or invalid 'plugin-info-format' in language file.</red>"));
            return;
        }

        StringBuilder fullMessageBuilder = new StringBuilder();

        // Append the overall header
        String allHeader = languageLoader.getMessage("plugin-info-all-header");
        if (allHeader != null && !allHeader.isEmpty() && !allHeader.startsWith("<red>Missing Language Key:")) {
            fullMessageBuilder.append(allHeader).append("\n\n"); // Append header and a single newline
        }

        for (int i = 0; i < plugins.size(); i++) {
            PluginContainer plugin = plugins.get(i);
            PluginDescription description = plugin.getDescription();

            String id = description.getId();
            String name = description.getName().orElse(id);
            String version = description.getVersion().orElse(languageLoader.getMessage("unknown-version"));
            String url = description.getUrl().orElse("");
            String desc = description.getDescription().orElse("");
            String authors = String.join(", ", description.getAuthors());

            String currentPluginInfo = pluginInfoFormat
                    .replace("{plugin_name}", name)
                    .replace("{id}", id)
                    .replace("{name}", name)
                    .replace("{version}", version)
                    .replace("{url}", url)
                    .replace("{description}", desc)
                    .replace("{authors}", authors);

            fullMessageBuilder.append(currentPluginInfo);

            // Add an empty line between plugin infos, except after the last one
            if (i < plugins.size() - 1) {
                fullMessageBuilder.append("\n"); // Two newlines to create an empty line
            }
        }

        source.sendMessage(miniMessage.deserialize(fullMessageBuilder.toString()));
    }

    /**
     * Provides suggestions for the 'info' command's plugin ID argument.
     *
     * @param source The command source.
     * @param args   The current arguments.
     * @return A CompletableFuture containing a list of suggested plugin IDs.
     */
    public CompletableFuture<List<String>> suggest(CommandSource source, String[] args) {
        if (args.length == 1) {
            String partialPluginId = args[0].toLowerCase();
            List<String> suggestions = proxyServer.getPluginManager().getPlugins().stream()
                    .map(pluginContainer -> pluginContainer.getDescription().getId())
                    .filter(id -> id.toLowerCase().startsWith(partialPluginId))
                    .collect(Collectors.toList());
            if ("all".startsWith(partialPluginId)) {
                suggestions.add("all");
            }
            return CompletableFuture.completedFuture(suggestions);
        }
        return CompletableFuture.completedFuture(new ArrayList<>());
    }

    /**
     * Builds the Brigadier command for 'vmonitor plugin info'.
     *
     * @param pluginInfoCommand The PluginInfoCommand instance.
     * @return A LiteralArgumentBuilder for the 'info' sub-command.
     */
    public static LiteralArgumentBuilder<CommandSource> build(PluginInfoCommand pluginInfoCommand) {
        SuggestionProvider<CommandSource> pluginIdSuggestionProvider = (context, builder) -> {
            CompletableFuture<List<String>> handlerSuggestions = pluginInfoCommand.suggest(context.getSource(), new String[]{builder.getRemaining()});
            return handlerSuggestions.thenApply(suggestionsList -> {
                suggestionsList.forEach(builder::suggest);
                return builder.build();
            });
        };

        RequiredArgumentBuilder<CommandSource, String> pluginArgument = BrigadierCommand.requiredArgumentBuilder("plugin", word())
                .suggests(pluginIdSuggestionProvider)
                .executes(context -> {
                    String pluginId = context.getArgument("plugin", String.class);
                    pluginInfoCommand.execute(context.getSource(), pluginId);
                    return SINGLE_SUCCESS;
                });

        return BrigadierCommand.literalArgumentBuilder("info")
                .requires(source -> source.hasPermission("vmonitor.plugin.info"))
                .then(pluginArgument)
                .executes(context -> {
                    context.getSource().sendMessage(pluginInfoCommand.miniMessage.deserialize(pluginInfoCommand.languageLoader.getMessage("usage-plugin-info")));
                    return SINGLE_SUCCESS;
                });
    }
}