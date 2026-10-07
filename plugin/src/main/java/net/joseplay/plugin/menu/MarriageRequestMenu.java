package net.joseplay.plugin.menu;

import net.joseplay.api.menu.Menu;
import net.joseplay.plugin.service.MarriageService;
import net.joseplay.plugin.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MarriageRequestMenu extends Menu {
    private final Player player;
    private final UUID requesterUuid;
    private final MarriageService marriageService;

    public MarriageRequestMenu(
            Player player,
            UUID requesterUuid,
            MarriageService marriageService
    ) {
        super("&d&lMarriage Proposal", 3);
        this.player = player;
        this.requesterUuid = requesterUuid;
        this.marriageService = marriageService;

        render();
    }

    public void render() {
        clear();

        ItemStack filler = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .build();
        fill(filler);

        OfflinePlayer requester = Bukkit.getOfflinePlayer(requesterUuid);
        String requesterName = requester.getName() != null ? requester.getName() : "Unknown";

        // Slot 13: Requester's head
        ItemStack head = ItemBuilder.of(Material.PLAYER_HEAD)
                .skullOwner(requester)
                .name("&d&l" + requesterName)
                .lore(
                        "&7Has proposed marriage to you!",
                        "",
                        "&eWill you marry this player?"
                )
                .build();
        setItem(13, head);

        // Slot 11: Accept button
        ItemStack acceptItem = ItemBuilder.of(Material.LIME_CONCRETE)
                .name("&a&lACCEPT")
                .lore(
                        "&7Click to accept the proposal and marry",
                        "&f" + requesterName + "&7!",
                        "",
                        "&aClick to accept"
                )
                .build();
        setItem(11, acceptItem, context -> {
            context.close();
            marriageService.acceptRequest(player, requesterUuid);
        });

        // Slot 15: Deny button
        ItemStack denyItem = ItemBuilder.of(Material.RED_CONCRETE)
                .name("&c&lDENY")
                .lore(
                        "&7Click to decline the proposal from",
                        "&f" + requesterName + "&7.",
                        "",
                        "&cClick to decline"
                )
                .build();
        setItem(15, denyItem, context -> {
            context.close();
            marriageService.denyRequest(player, requesterUuid);
        });
    }
}
