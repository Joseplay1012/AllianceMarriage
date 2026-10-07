package net.joseplay.plugin.shop;

import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.service.MarriageService;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class HeartsShopService {
    private final Plugin plugin;
    private final MarriageService marriageService;
    private final Set<UUID> processingPurchases = ConcurrentHashMap.newKeySet();

    public HeartsShopService(Plugin plugin, MarriageService marriageService) {
        this.plugin = plugin;
        this.marriageService = marriageService;
    }

    public CompletableFuture<Boolean> purchase(Player player, HeartsShopItem item) {
        if (player == null || item == null) {
            return CompletableFuture.completedFuture(false);
        }

        UUID playerId = player.getUniqueId();

        if (!processingPurchases.add(playerId)) {
            player.sendMessage(ChatColor.RED + "Please wait, your previous purchase is still processing.");
            return CompletableFuture.completedFuture(false);
        }

        return marriageService.getCouple(playerId).thenCompose(optionalCouple -> {
            if (optionalCouple.isEmpty()) {
                processingPurchases.remove(playerId);
                player.sendMessage(ChatColor.RED + "You must be married to purchase items from the Hearts shop!");
                return CompletableFuture.completedFuture(false);
            }

            Couple couple = optionalCouple.get();
            Integer balanceObj = couple.features().get("hearts");
            int currentHearts = balanceObj != null ? balanceObj : 0;

            if (currentHearts < item.getPrice()) {
                processingPurchases.remove(playerId);
                player.sendMessage(ChatColor.RED + "You do not have enough Hearts! Price: "
                        + ChatColor.LIGHT_PURPLE + "♥ " + item.getPrice()
                        + ChatColor.RED + ", Current balance: "
                        + ChatColor.LIGHT_PURPLE + "♥ " + currentHearts);
                return CompletableFuture.completedFuture(false);
            }

            return couple.features().increment("hearts", -item.getPrice()).thenApply(newHearts -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        executeCommands(player, item, newHearts);
                        String itemName = item.getName().isEmpty()
                                ? item.getMaterial().name()
                                : ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getName()));
                        player.sendMessage(ChatColor.GREEN + "Purchased "
                                + ChatColor.YELLOW + itemName
                                + ChatColor.GREEN + " for "
                                + ChatColor.LIGHT_PURPLE + "♥ " + item.getPrice() + " Hearts"
                                + ChatColor.GREEN + "! Remaining balance: "
                                + ChatColor.LIGHT_PURPLE + "♥ " + newHearts);
                    } finally {
                        processingPurchases.remove(playerId);
                    }
                });
                return true;
            }).exceptionally(throwable -> {
                processingPurchases.remove(playerId);
                player.sendMessage(ChatColor.RED + "An error occurred while processing your purchase. No Hearts were lost.");
                plugin.getLogger().severe("Purchase failed for " + player.getName() + ": " + throwable.getMessage());
                throwable.printStackTrace();
                return false;
            });
        }).exceptionally(throwable -> {
            processingPurchases.remove(playerId);
            player.sendMessage(ChatColor.RED + "Could not retrieve marriage data.");
            return false;
        });
    }

    private void executeCommands(Player player, HeartsShopItem item, int remainingHearts) {
        List<String> commands = item.getCommands();
        if (commands == null) return;

        for (String rawCmd : commands) {
            String formatted = rawCmd
                    .replace("%player%", player.getName())
                    .replace("%uuid%", player.getUniqueId().toString())
                    .replace("%price%", String.valueOf(item.getPrice()))
                    .replace("%item%", item.getId())
                    .replace("%hearts%", String.valueOf(remainingHearts));

            if (formatted.startsWith("/")) {
                formatted = formatted.substring(1);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formatted);
        }
    }
}
