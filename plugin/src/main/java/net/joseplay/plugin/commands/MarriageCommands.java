package net.joseplay.plugin.commands;

import net.joseplay.core.Core;
import net.joseplay.core.couple.Couple;
import net.joseplay.core.storage.CouplesRepository;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MarriageCommands implements CommandExecutor, TabCompleter {
    private final CouplesRepository couplesRepository = Core.getInstance().getCouplesRepository();

    @Override
    public boolean onCommand(
            @NonNull CommandSender sender,
            @NonNull Command command,
            @NonNull String label,
            @NonNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "marry" -> handleMarry(player, args);
            case "couple" -> handleCouple(player);
            case "divorce" -> handleDivorce(player);
            default -> sendUsage(player, label);
        }

        return true;
    }

    private void handleMarry(Player player, String[] args) {
        if (args.length < 2 || args[1].isBlank()) {
            player.sendMessage("Usage: /marry <player>");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            player.sendMessage("Player is not online.");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage("You cannot marry yourself.");
            return;
        }

        couplesRepository
                .marryPlayer(player.getUniqueId(), target.getUniqueId())
                .thenAccept(result -> {
                    if (result.isError()) {
                        player.sendMessage(result.getErrorMessage());
                        return;
                    }

                    player.sendMessage(
                            "You are now married to " + target.getName() + "."
                    );

                    target.sendMessage(
                            player.getName() + " is now married to you."
                    );
                })
                .exceptionally(throwable -> {
                    player.sendMessage(
                            "An error occurred while creating the marriage."
                    );
                    throwable.printStackTrace();
                    return null;
                });
    }

    private void handleCouple(Player player) {
        couplesRepository
                .findPartner(player.getUniqueId())
                .thenCompose(partner -> {
                    if (partner.isEmpty()) {
                        player.sendMessage("You are not married.");
                        return CompletableFuture.completedFuture(null);
                    }

                    UUID partnerUUID = partner.get();

                    return couplesRepository
                            .find(player.getUniqueId(), partnerUUID)
                            .thenAccept(couple -> {
                                if (couple.isEmpty()) {
                                    player.sendMessage(
                                            "Your marriage data could not be found."
                                    );
                                    return;
                                }

                                showCouple(player, couple.get());
                            });
                })
                .exceptionally(throwable -> {
                    player.sendMessage(
                            "An error occurred while loading your couple."
                    );
                    throwable.printStackTrace();
                    return null;
                });
    }

    private void showCouple(Player player, Couple couple) {
        Integer hearts = couple.features().get("hearts");

        player.sendMessage(
                "Your couple has " + hearts + " points."
        );

        couple.features()
                .increment("hearts", 5)
                .thenAccept(points ->
                        player.sendMessage(
                                "You now have " + points + " points."
                        )
                )
                .exceptionally(throwable -> {
                    player.sendMessage(
                            "Failed to update couple points."
                    );
                    throwable.printStackTrace();
                    return null;
                });
    }

    private void handleDivorce(Player player) {
        couplesRepository
                .divorcePlayer(player.getUniqueId())
                .thenAccept(result -> {
                    if (result.isError()) {
                        player.sendMessage(result.getErrorMessage());
                        return;
                    }

                    player.sendMessage("You are now divorced.");
                })
                .exceptionally(throwable -> {
                    player.sendMessage(
                            "An error occurred while divorcing."
                    );
                    throwable.printStackTrace();
                    return null;
                });
    }

    @Override
    public List<String> onTabComplete(
            @NonNull CommandSender sender,
            @NonNull Command command,
            @NonNull String alias,
            @NonNull String[] args
    ) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            return List.of("marry", "couple", "divorce")
                    .stream()
                    .filter(option -> option.startsWith(input))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("marry")) {
            String input = args[1].toLowerCase();

            return Bukkit.getOnlinePlayers()
                    .stream()
                    .filter(target -> !target.getUniqueId().equals(player.getUniqueId()))
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .sorted()
                    .toList();
        }

        return List.of();
    }

    private void sendUsage(Player player, String label) {
        player.sendMessage("Usage:");
        player.sendMessage("/" + label + " marry <player>");
        player.sendMessage("/" + label + " couple");
        player.sendMessage("/" + label + " divorce");
    }
}