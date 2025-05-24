package cn.nirvana.vMonitor.command;

import cn.nirvana.vMonitor.config.LanguageLoader;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.Command;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.TextComponent; // This might not be strictly needed, but good to have
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import com.velocitypowered.api.command.BrigadierCommand;

public class PluginListCommand {
    private final ProxyServer proxyServer;
    private final LanguageLoader languageLoader;
    private final MiniMessage miniMessage;

    public PluginListCommand(ProxyServer proxyServer, LanguageLoader languageLoader, MiniMessage miniMessage) {
        this.proxyServer = proxyServer;
        this.languageLoader = languageLoader;
        this.miniMessage = miniMessage;
    }

    /**
     * Executes the 'plugin list' command.
     * Displays a list of all loaded plugins.
     *
     * @param source The command source.
     */
    public void execute(CommandSource source) {
        if (source instanceof com.velocitypowered.api.proxy.Player && !source.hasPermission("vmonitor.plugin.list")) {
            source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("no-permission")));
            return;
        }

        List<PluginContainer> plugins = new ArrayList<>(proxyServer.getPluginManager().getPlugins());
        if (plugins.isEmpty()) {
            source.sendMessage(miniMessage.deserialize(languageLoader.getMessage("plugin-list-empty")));
            return;
        }

        plugins.sort(Comparator.comparing(p -> p.getDescription().getName().orElse(p.getDescription().getId())));

        // 获取整体的插件列表格式
        String pluginListFormat = languageLoader.getMessage("plugin-list-format");
        if (pluginListFormat == null || pluginListFormat.isEmpty() || pluginListFormat.startsWith("<red>Missing Language Key:")) {
            source.sendMessage(miniMessage.deserialize("<red>Error: Missing or invalid 'plugin-list-format' in language file.</red>"));
            return;
        }

        // 获取用于 hover event 的格式。现在这是一个多行字面量块。
        String pluginListHoverFormat = languageLoader.getMessage("plugin-list-hover-format");
        if (pluginListHoverFormat == null || pluginListHoverFormat.isEmpty() || pluginListHoverFormat.startsWith("<red>Missing Language Key:")) {
            // 提供一个默认的、简单的回退格式，避免崩溃
            pluginListHoverFormat = "<gold>ID:</gold> <gray>{id}</gray>\n<gold>Version:</gold> <gray>{version}</gray>\n<gold>Description:</gold> <gray>{description}</gray>";
            source.sendMessage(miniMessage.deserialize("<yellow>Warning: Missing or invalid 'plugin-list-hover-format' in language file. Using default.</yellow>"));
        }


        StringBuilder pluginEntries = new StringBuilder();
        for (PluginContainer plugin : plugins) {
            PluginDescription description = plugin.getDescription();
            String id = description.getId();
            String name = description.getName().orElse(id);
            String version = description.getVersion().orElse(languageLoader.getMessage("unknown-version"));
            String url = description.getUrl().orElse("");
            String desc = description.getDescription().orElse("");
            String authors = String.join(", ", description.getAuthors());


            // 构造单个插件的行，这里需要与 YAML 中 {plugin_entry} 占位符对应
            String entryLine = languageLoader.getMessage("plugin-list-entry-line")
                    .replace("{plugin_name}", name)
                    .replace("{plugin_version}", version);

            // 填充 hover 文本的占位符
            String filledHoverText = pluginListHoverFormat
                    .replace("{id}", id)
                    .replace("{name}", name)
                    .replace("{version}", version)
                    .replace("{url}", url)
                    .replace("{description}", desc)
                    .replace("{authors}", authors);


            // MiniMessage.deserialize 可以直接处理包含换行符的完整 MiniMessage 字符串
            Component finalEntryComponent = miniMessage.deserialize(entryLine)
                    .hoverEvent(HoverEvent.showText(miniMessage.deserialize(filledHoverText)))
                    .clickEvent(ClickEvent.runCommand("/vmonitor plugin info " + id));

            pluginEntries.append(miniMessage.serialize(finalEntryComponent)).append("\n"); // 将 Component 序列化回字符串并添加换行
        }

        // 填充整体格式中的占位符
        String finalMessage = pluginListFormat
                .replace("{count}", String.valueOf(plugins.size()))
                .replace("{plugin_entries}", pluginEntries.toString().trim()); // 使用 trim() 移除末尾多余换行

        source.sendMessage(miniMessage.deserialize(finalMessage));
    }

    /**
     * Builds the Brigadier command for 'vmonitor plugin list'.
     *
     * @param pluginListCommand The PluginListCommand instance.
     * @return A LiteralArgumentBuilder for the 'list' sub-command.
     */
    public static LiteralArgumentBuilder<CommandSource> build(PluginListCommand pluginListCommand) {
        return BrigadierCommand.literalArgumentBuilder("list")
                .requires(source -> source.hasPermission("vmonitor.plugin.list"))
                .executes(context -> {
                    pluginListCommand.execute(context.getSource());
                    return SINGLE_SUCCESS;
                });
    }
}