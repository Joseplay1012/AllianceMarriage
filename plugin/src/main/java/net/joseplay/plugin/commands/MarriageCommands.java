package net.joseplay.plugin.commands;

import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.menu.MarriageMenuManager;
import net.joseplay.plugin.service.MarriageService;
import net.joseplay.plugin.shop.HeartsShop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MarriageCommands implements CommandExecutor, TabCompleter {
    private final MarriageMenuManager menuManager;
    private final MarriageService marriageService;
    private final HeartsShop heartsShop;

    public MarriageCommands(
            MarriageMenuManager menuManager,
            MarriageService marriageService,
            HeartsShop heartsShop
    ) {
        this.menuManager = menuManager;
        this.marriageService = marriageService;
        this.heartsShop = heartsShop;
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
            sender.sendMessage("Only players can use this command.");
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
            player.sendMessage(ChatColor.RED + "Usage: /marry <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player is not online.");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot marry yourself.");
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
            player.sendMessage(ChatColor.RED + "You do not have any pending marriage proposals.");
            return;
        }

        marriageService.acceptRequest(player, pendingRequester.get());
    }

    private void handleDeny(Player player) {
        Optional<UUID> pendingRequester = marriageService.getPendingRequester(player.getUniqueId());
        if (pendingRequester.isEmpty()) {
            player.sendMessage(ChatColor.RED + "You do not have any pending marriage proposals.");
            return;
        }

        marriageService.denyRequest(player, pendingRequester.get());
    }

    private void handleCouple(Player player) {
        marriageService.getCouple(player.getUniqueId())
                .thenAccept(coupleOpt -> {
                    if (coupleOpt.isEmpty()) {
                        player.sendMessage(ChatColor.RED + "You are not married.");
                        return;
                    }

                    Couple couple = coupleOpt.get();
                    couple.features().increment("hearts", 10).thenAccept(hearts -> {
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "Your couple currently has "
                                + ChatColor.YELLOW + (hearts != null ? hearts : 0) + " Hearts.");
                    });
                })
                .exceptionally(throwable -> {
                    player.sendMessage(ChatColor.RED + "An error occurred while loading your couple data.");
                    throwable.printStackTrace();
                    return null;
                });
    }

    private void handleDivorce(Player player) {
        marriageService.divorce(player);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("marry.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to reload the marriage plugin.");
            return;
        }

        heartsShop.load();
        sender.sendMessage(ChatColor.GREEN + "[AllianceMarriage] Hearts shop configuration reloaded successfully!");
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
            Player player = sender instanceof Player p ? p : null;

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
        player.sendMessage(ChatColor.LIGHT_PURPLE + "=== AllianceMarriage Commands ===");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " menu" + ChatColor.GRAY + " - Open main marriage menu");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " shop" + ChatColor.GRAY + " - Open Hearts shop");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " marry <player>" + ChatColor.GRAY + " - Propose to a player");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " accept" + ChatColor.GRAY + " - Accept a proposal");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " deny" + ChatColor.GRAY + " - Decline a proposal");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " couple" + ChatColor.GRAY + " - View marriage points");
        player.sendMessage(ChatColor.YELLOW + "/" + label + " divorce" + ChatColor.GRAY + " - Divorce your partner");
        if (player.hasPermission("marry.admin") || player.isOp()) {
            player.sendMessage(ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - Reload shop configuration");
        }
    }
}
