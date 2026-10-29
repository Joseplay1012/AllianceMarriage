package net.joseplay.plugin.config;

import net.joseplay.core.config.PluginConfig;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads player-facing text from {@code messages.yml}.
 * Values are resolved on each call so a config reload is picked up without rebuilding this class.
 */
public final class Messages {
    private final PluginConfig config;

    public Messages(PluginConfig config) {
        this.config = config;
    }

    public PluginConfig config() {
        return config;
    }

    public String raw(String path) {
        return config.getString(path, "");
    }

    public String get(String path, String... replacements) {
        String message = config.getString(path, path);
        return color(apply(message, replacements));
    }

    public List<String> getList(String path, String... replacements) {
        List<String> lines = config.getStringList(path);
        List<String> colored = new ArrayList<>();
        for (String line : lines) {
            colored.add(color(apply(line, replacements)));
        }
        return colored;
    }

    public void send(CommandSender sender, String path, String... replacements) {
        if (sender == null) {
            return;
        }
        if (config.isList(path)) {
            sendList(sender, path, replacements);
            return;
        }
        String message = get(path, replacements);
        if (!message.isEmpty()) {
            sender.sendMessage(message);
        }
    }

    public void sendList(CommandSender sender, String path, String... replacements) {
        if (sender == null) {
            return;
        }
        for (String line : getList(path, replacements)) {
            if (!line.isEmpty()) {
                sender.sendMessage(line);
            }
        }
    }

    public String error(String fallback, String... replacements) {
        if (fallback == null || fallback.isBlank()) {
            return get("marriage.error", replacements);
        }
        return color(apply(fallback, replacements));
    }

    public static String color(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String apply(String text, String... replacements) {
        if (text == null) {
            return "";
        }
        if (replacements == null || replacements.length == 0) {
            return text;
        }

        String result = text;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            String key = replacements[i];
            String value = replacements[i + 1] != null ? replacements[i + 1] : "";
            result = result.replace(key, value);
        }
        return result;
    }
}
