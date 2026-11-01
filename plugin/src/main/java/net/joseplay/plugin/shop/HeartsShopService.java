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
    private final Set<UUID> processingCouplePurchases = ConcurrentHashMap.newKeySet();

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
            if (messages != null) messages.send(player, "shop.purchase-in-progress");
            return CompletableFuture.completedFuture(false);
        }

        return marriageService.getCouple(playerId).thenCompose(optionalCouple -> {
            if (optionalCouple.isEmpty()) {
                processingPurchases.remove(playerId);
                if (messages != null) messages.send(player, "shop.must-be-married");
                return CompletableFuture.completedFuture(false);
            }

            Couple couple = optionalCouple.get();
            UUID coupleId = couple.getId();

            if (!processingCouplePurchases.add(coupleId)) {
                processingPurchases.remove(playerId);
                if (messages != null) messages.send(player, "shop.purchase-in-progress");
                return CompletableFuture.completedFuture(false);
            }

            int price = item.getPrice();
            Integer balanceObj = couple.features().get("hearts");
            int currentHearts = balanceObj != null ? balanceObj : 0;

            if (currentHearts < price) {
                processingPurchases.remove(playerId);
                processingCouplePurchases.remove(coupleId);
                if (messages != null) messages.send(player, "shop.not-enough-hearts",
                        "%price%", String.valueOf(price),
                        "%hearts%", String.valueOf(currentHearts));
                return CompletableFuture.completedFuture(false);
            }

            return couple.features().decrementIfSufficient("hearts", price).thenCompose(success -> {
                if (!success) {
                    processingPurchases.remove(playerId);
                    processingCouplePurchases.remove(coupleId);
                    int remaining = couple.features().get("hearts");
                    if (messages != null) messages.send(player, "shop.not-enough-hearts",
                            "%price%", String.valueOf(price),
                            "%hearts%", String.valueOf(remaining));
                    return CompletableFuture.completedFuture(false);
                }

                int newHearts = couple.features().get("hearts");
                CompletableFuture<Boolean> purchaseFuture = new CompletableFuture<>();

                runOnMainThread(() -> {
                    try {
                        executeCommands(player, item, newHearts);
                        String itemName = item.getName().isEmpty()
                                ? item.getMaterial().name()
                                : ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', item.getName()));
                        if (messages != null) messages.send(player, "shop.purchased",
                                "%item%", itemName,
                                "%price%", String.valueOf(price),
                                "%hearts%", String.valueOf(newHearts));
                        processingPurchases.remove(playerId);
                        processingCouplePurchases.remove(coupleId);
                        purchaseFuture.complete(true);
                    } catch (Exception ex) {
                        if (plugin != null) {
                            plugin.getLogger().severe("Command execution failed during purchase for " + player.getName() + ": " + ex.getMessage());
                        }
                        // Refund hearts because purchase execution failed!
                        couple.features().increment("hearts", price).whenComplete((res, refundEx) -> {
                            processingPurchases.remove(playerId);
                            processingCouplePurchases.remove(coupleId);
                            if (messages != null) messages.send(player, "shop.purchase-error");
                            purchaseFuture.complete(false);
                        });
                    }
                });

                return purchaseFuture;
            }).exceptionally(throwable -> {
                processingPurchases.remove(playerId);
                processingCouplePurchases.remove(coupleId);
                if (messages != null) messages.send(player, "shop.purchase-error");
                if (plugin != null) {
                    plugin.getLogger().severe("Purchase failed for " + player.getName() + ": " + throwable.getMessage());
                }
                return false;
            });
        }).exceptionally(throwable -> {
            processingPurchases.remove(playerId);
            if (messages != null) messages.send(player, "shop.couple-data-error");
            return false;
        });
    }

    protected void runOnMainThread(Runnable task) {
        if (plugin != null && Bukkit.getServer() != null) {
            Bukkit.getScheduler().runTask(plugin, task);
        } else {
            task.run();
        }
    }

    protected void executeCommands(Player player, HeartsShopItem item, int remainingHearts) {
        if (Bukkit.getServer() == null || Bukkit.getConsoleSender() == null) {
            return;
        }

        List<String> commands = item.getCommands();
        if (commands == null) {
            return;
        }

        for (String rawCmd : commands) {
            String formatted = rawCmd
                    .replace("%player%", player.getName() != null ? player.getName() : "")
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
