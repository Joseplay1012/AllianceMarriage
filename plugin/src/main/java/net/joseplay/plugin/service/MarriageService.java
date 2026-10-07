package net.joseplay.plugin.service;

import net.joseplay.core.Core;
import net.joseplay.core.contexts.MarryContextResult;
import net.joseplay.core.couple.Couple;
import net.joseplay.core.storage.CouplesRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class MarriageService {

    public record MarriageRequest(UUID requester, UUID target, long timestamp) {
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60_000L; // 60 seconds expiration
        }
    }

    private final Plugin plugin;
    private final CouplesRepository couplesRepository;
    private final Map<UUID, MarriageRequest> pendingRequests = new ConcurrentHashMap<>();

    public MarriageService(Plugin plugin) {
        this.plugin = plugin;
        this.couplesRepository = Core.getInstance().getCouplesRepository();
    }

    public CouplesRepository getCouplesRepository() {
        return couplesRepository;
    }

    public CompletableFuture<Optional<Couple>> getCouple(UUID playerUuid) {
        return couplesRepository.findPartner(playerUuid).thenCompose(partnerOpt -> {
            if (partnerOpt.isEmpty()) {
                return CompletableFuture.completedFuture(Optional.empty());
            }
            return couplesRepository.find(playerUuid, partnerOpt.get());
        });
    }

    public CompletableFuture<Boolean> sendRequest(Player requester, Player target) {
        if (requester.getUniqueId().equals(target.getUniqueId())) {
            requester.sendMessage(ChatColor.RED + "You cannot marry yourself.");
            return CompletableFuture.completedFuture(false);
        }

        return couplesRepository.findPartner(requester.getUniqueId()).thenCompose(partner1 -> {
            if (partner1.isPresent()) {
                requester.sendMessage(ChatColor.RED + "You are already married.");
                return CompletableFuture.completedFuture(false);
            }

            return couplesRepository.findPartner(target.getUniqueId()).thenApply(partner2 -> {
                if (partner2.isPresent()) {
                    requester.sendMessage(ChatColor.RED + target.getName() + " is already married.");
                    return false;
                }

                MarriageRequest request = new MarriageRequest(
                        requester.getUniqueId(),
                        target.getUniqueId(),
                        System.currentTimeMillis()
                );
                pendingRequests.put(target.getUniqueId(), request);

                requester.sendMessage(ChatColor.GREEN + "Marriage proposal sent to " + ChatColor.LIGHT_PURPLE + target.getName() + ChatColor.GREEN + "!");
                target.sendMessage(ChatColor.LIGHT_PURPLE + requester.getName() + ChatColor.GREEN + " sent you a marriage proposal!");

                return true;
            });
        });
    }

    public Optional<UUID> getPendingRequester(UUID targetUuid) {
        MarriageRequest request = pendingRequests.get(targetUuid);
        if (request == null) {
            return Optional.empty();
        }
        if (request.isExpired()) {
            pendingRequests.remove(targetUuid);
            return Optional.empty();
        }
        return Optional.of(request.requester());
    }

    public CompletableFuture<MarryContextResult> acceptRequest(Player target, UUID requesterUuid) {
        MarriageRequest request = pendingRequests.get(target.getUniqueId());
        if (request == null || !request.requester().equals(requesterUuid) || request.isExpired()) {
            pendingRequests.remove(target.getUniqueId());
            target.sendMessage(ChatColor.RED + "This marriage proposal has expired or does not exist.");
            return CompletableFuture.completedFuture(MarryContextResult.error(
                    requesterUuid,
                    target.getUniqueId(),
                    MarryContextResult.MarryErrorType.NOT_MARRIED,
                    "Proposal expired."
            ));
        }

        pendingRequests.remove(target.getUniqueId());

        return couplesRepository.marryPlayer(requesterUuid, target.getUniqueId()).thenApply(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player requester = Bukkit.getPlayer(requesterUuid);
                if (result.isError()) {
                    target.sendMessage(ChatColor.RED + result.getErrorMessage());
                    if (requester != null && requester.isOnline()) {
                        requester.sendMessage(ChatColor.RED + result.getErrorMessage());
                    }
                } else {
                    target.sendMessage(ChatColor.GREEN + "You are now married!");
                    if (requester != null && requester.isOnline()) {
                        requester.sendMessage(ChatColor.GREEN + target.getName() + " accepted your marriage proposal! You are now married!");
                    }
                }
            });
            return result;
        });
    }

    public void denyRequest(Player target, UUID requesterUuid) {
        MarriageRequest request = pendingRequests.remove(target.getUniqueId());
        target.sendMessage(ChatColor.YELLOW + "You declined the marriage proposal.");

        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null && requester.isOnline()) {
            requester.sendMessage(ChatColor.RED + target.getName() + " declined your marriage proposal.");
        }
    }

    public CompletableFuture<MarryContextResult> divorce(Player player) {
        return couplesRepository.divorcePlayer(player.getUniqueId()).thenApply(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isError()) {
                    player.sendMessage(ChatColor.RED + result.getErrorMessage());
                } else {
                    player.sendMessage(ChatColor.GREEN + "You are now divorced.");
                }
            });
            return result;
        });
    }
}
