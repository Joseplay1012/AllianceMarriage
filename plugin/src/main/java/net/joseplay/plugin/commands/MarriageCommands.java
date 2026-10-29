package net.joseplay.plugin.commands;

import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.Plugin;
import net.joseplay.plugin.config.Messages;
import net.joseplay.plugin.menu.CoupleSettings;
import net.joseplay.plugin.menu.MarriageMenuManager;
import net.joseplay.plugin.service.MarriageService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MarriageCommands implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final MarriageMenuManager menuManager;
    private final MarriageService marriageService;
    private final Messages messages;

    public MarriageCommands(
            Plugin plugin,
            MarriageMenuManager menuManager,
            MarriageService marriageService,
            Messages messages
    ) {
        this.plugin = plugin;
        this.menuManager = menuManager;
        this.marriageService = marriageService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(
            @NonNull CommandSender sender,
            @NonNull Command command,
            @NonNull String label,
            @NonNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                handleReload(sender);
                return true;
            }
            messages.send(sender, "commands.player-only");
            return true;
        }

        if (args.length == 0) {
            menuManager.openMarriageMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu" -> menuManager.openMarriageMenu(player);
            case "shop" -> menuManager.openHeartsShopMenu(player);
            case "marry" -> handleMarry(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "couple" -> handleCouple(player);
            case "divorce" -> handleDivorce(player);
            case "reload" -> handleReload(sender);
            default -> sendUsage(player, label);
        }

        return true;
    }

    private void handleMarry(Player player, String[] args) {
        if (args.length < 2 || args[1].isBlank()) {
            messages.send(player, "commands.marry-usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            messages.send(player, "validation.player-offline");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            messages.send(player, "validation.cannot-marry-self");
            return;
        }

        marriageService.sendRequest(player, target).thenAccept(sent -> {
            if (sent) {
                menuManager.openMarriageRequestMenu(target, player.getUniqueId());
            }
        });
    }

    private void handleAccept(Player player) {
        Optional<UUID> pendingRequester = marriageService.getPendingRequester(player.getUniqueId());
        if (pendingRequester.isEmpty()) {
            messages.send(player, "validation.no-pending-proposal");
            return;
        }

        marriageService.acceptRequest(player, pendingRequester.get());
    }

    private void handleDeny(Player player) {
        Optional<UUID> pendingRequester = marriageService.getPendingRequester(player.getUniqueId());
        if (pendingRequester.isEmpty()) {
            messages.send(player, "validation.no-pending-proposal");
            return;
        }

        marriageService.denyRequest(player, pendingRequester.get());
    }

    private void handleCouple(Player player) {
        marriageService.getCouple(player.getUniqueId())
                .thenAccept(coupleOpt -> {
                    if (coupleOpt.isEmpty()) {
                        messages.send(player, "validation.not-married");
                        return;
                    }

                    Couple couple = coupleOpt.get();

                    Bukkit.getScheduler().runTask(menuManager.getPlugin(), () -> {
                        CoupleSettings coupleSettings = new CoupleSettings(player, couple, menuManager);
                        menuManager.getMenuManager().open(player, coupleSettings);
                    });
                })
                .exceptionally(throwable -> {
                    messages.send(player, "validation.couple-load-error");
                    throwable.printStackTrace();
                    return null;
                });
    }

    private void handleDivorce(Player player) {
        marriageService.divorce(player);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("marry.admin") && !sender.isOp()) {
            messages.send(sender, "commands.no-permission");
            return;
        }

        boolean success = plugin.reloadPluginConfiguration();
        messages.send(sender, success ? "commands.reload-success" : "commands.reload-partial");
    }

    @Override
    public List<String> onTabComplete(
            @NonNull CommandSender sender,
            @NonNull Command command,
            @NonNull String alias,
            @NonNull String[] args
    ) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return List.of("menu", "shop", "marry", "accept", "deny", "couple", "divorce", "reload")
                    .stream()
                    .filter(option -> option.startsWith(input))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("marry")) {
            String input = args[1].toLowerCase();
            Player player = sender instanceof Player commandPlayer ? commandPlayer : null;

            return Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(target -> player == null || !target.getUniqueId().equals(player.getUniqueId()))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .sorted()
                    .toList();
        }

        return List.of();
    }

    private void sendUsage(Player player, String label) {
        messages.send(player, "commands.usage-header");
        messages.sendList(player, "commands.usage", "%label%", label);
        if (player.hasPermission("marry.admin") || player.isOp()) {
            messages.send(player, "commands.usage-reload", "%label%", label);
        }
    }
}
