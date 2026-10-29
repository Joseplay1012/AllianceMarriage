package net.joseplay.plugin.service;

import net.joseplay.core.Core;
import net.joseplay.core.contexts.MarryContextResult;
import net.joseplay.core.couple.Couple;
import net.joseplay.core.storage.CouplesRepository;
import net.joseplay.plugin.config.Messages;
import org.bukkit.Bukkit;
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
            return System.currentTimeMillis() - timestamp > 60_000L;
        }
    }

    private final Plugin plugin;
    private final Messages messages;
    private final CouplesRepository couplesRepository;
    private final Map<UUID, MarriageRequest> pendingRequests = new ConcurrentHashMap<>();

    public MarriageService(Plugin plugin, Messages messages) {
        this.plugin = plugin;
        this.messages = messages;
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
            messages.send(requester, "validation.cannot-marry-self");
            return CompletableFuture.completedFuture(false);
        }

        return couplesRepository.findPartner(requester.getUniqueId()).thenCompose(partner1 -> {
            if (partner1.isPresent()) {
                messages.send(requester, "validation.already-married");
                return CompletableFuture.completedFuture(false);
            }

            return couplesRepository.findPartner(target.getUniqueId()).thenApply(partner2 -> {
                if (partner2.isPresent()) {
                    messages.send(requester, "validation.target-already-married", "%player%", target.getName());
                    return false;
                }

                MarriageRequest request = new MarriageRequest(
                        requester.getUniqueId(),
                        target.getUniqueId(),
                        System.currentTimeMillis()
                );
                pendingRequests.put(target.getUniqueId(), request);

                messages.send(requester, "marriage.proposal-sent", "%player%", target.getName());
                messages.send(target, "marriage.proposal-received", "%player%", requester.getName());

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
            messages.send(target, "marriage.proposal-expired");
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
                    sendMarriageError(target, result);
                    if (requester != null && requester.isOnline()) {
                        sendMarriageError(requester, result);
                    }
                } else {
                    messages.send(target, "marriage.married");
                    if (requester != null && requester.isOnline()) {
                        messages.send(requester, "marriage.accepted-requester", "%player%", target.getName());
                    }
                }
            });
            return result;
        });
    }

    public void denyRequest(Player target, UUID requesterUuid) {
        pendingRequests.remove(target.getUniqueId());
        messages.send(target, "marriage.declined-target");

        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null && requester.isOnline()) {
            messages.send(requester, "marriage.declined-requester", "%player%", target.getName());
        }
    }

    public CompletableFuture<MarryContextResult> divorce(Player player) {
        return couplesRepository.divorcePlayer(player.getUniqueId()).thenApply(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result.isError()) {
                    sendMarriageError(player, result);
                } else {
                    messages.send(player, "marriage.divorced");
                }
            });
            return result;
        });
    }

    private void sendMarriageError(Player player, MarryContextResult result) {
        if (result.getErrorType() == MarryContextResult.MarryErrorType.ALREADY_MARRIED) {
            messages.send(player, "marriage.already-married");
            return;
        }
        if (result.getErrorType() == MarryContextResult.MarryErrorType.PARTNER_ALREADY_MARRIED) {
            messages.send(player, "marriage.partner-already-married");
            return;
        }
        if (result.getErrorType() == MarryContextResult.MarryErrorType.NOT_MARRIED) {
            messages.send(player, "marriage.player-not-married");
            return;
        }
        String error = result.getErrorMessage() != null ? result.getErrorMessage() : "";
        messages.send(player, "marriage.error", "%error%", error);
    }
}
