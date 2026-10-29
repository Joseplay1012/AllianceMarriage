package net.joseplay.plugin.shop;

import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.config.Messages;
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
    private final Messages messages;
    private final Set<UUID> processingPurchases = ConcurrentHashMap.newKeySet();

    public HeartsShopService(Plugin plugin, MarriageService marriageService, Messages messages) {
        this.plugin = plugin;
        this.marriageService = marriageService;
        this.messages = messages;
    }

    public CompletableFuture<Boolean> purchase(Player player, HeartsShopItem item) {
        if (player == null || item == null) {
            return CompletableFuture.completedFuture(false);
        }

        UUID playerId = player.getUniqueId();

        if (!processingPurchases.add(playerId)) {
            messages.send(player, "shop.purchase-in-progress");
            return CompletableFuture.completedFuture(false);
        }

        return marriageService.getCouple(playerId).thenCompose(optionalCouple -> {
            if (optionalCouple.isEmpty()) {
                processingPurchases.remove(playerId);
                messages.send(player, "shop.must-be-married");
                return CompletableFuture.completedFuture(false);
            }

            Couple couple = optionalCouple.get();
            Integer balanceObj = couple.features().get("hearts");
            int currentHearts = balanceObj != null ? balanceObj : 0;

            if (currentHearts < item.getPrice()) {
                processingPurchases.remove(playerId);
                messages.send(player, "shop.not-enough-hearts",
                        "%price%", String.valueOf(item.getPrice()),
                        "%hearts%", String.valueOf(currentHearts));
                return CompletableFuture.completedFuture(false);
            }

            return couple.features().increment("hearts", -item.getPrice()).thenApply(newHearts -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    try {
                        executeCommands(player, item, newHearts);
                        String itemName = item.getName().isEmpty()
                                ? item.getMaterial().name()
                                : ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getName()));
                        messages.send(player, "shop.purchased",
                                "%item%", itemName,
                                "%price%", String.valueOf(item.getPrice()),
                                "%hearts%", String.valueOf(newHearts));
                    } finally {
                        processingPurchases.remove(playerId);
                    }
                });
                return true;
            }).exceptionally(throwable -> {
                processingPurchases.remove(playerId);
                messages.send(player, "shop.purchase-error");
                plugin.getLogger().severe("Purchase failed for " + player.getName() + ": " + throwable.getMessage());
                throwable.printStackTrace();
                return false;
            });
        }).exceptionally(throwable -> {
            processingPurchases.remove(playerId);
            messages.send(player, "shop.couple-data-error");
            return false;
        });
    }

    private void executeCommands(Player player, HeartsShopItem item, int remainingHearts) {
        List<String> commands = item.getCommands();
        if (commands == null) {
            return;
        }

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
