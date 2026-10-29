package net.joseplay.plugin.menu;

import net.joseplay.api.menu.Menu;
import net.joseplay.plugin.config.MenuConfig;
import net.joseplay.plugin.service.MarriageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MarriageRequestMenu extends Menu {
    private final Player player;
    private final UUID requesterUuid;
    private final MarriageService marriageService;
    private final MenuConfig menuConfig;

    public MarriageRequestMenu(
            Player player,
            UUID requesterUuid,
            MarriageService marriageService,
            MenuConfig menuConfig
    ) {
        super(menuConfig.title("request", "&d&lMarriage Proposal"), menuConfig.rows("request", 3));
        this.player = player;
        this.requesterUuid = requesterUuid;
        this.marriageService = marriageService;
        this.menuConfig = menuConfig;

        render();
    }

    public void render() {
        clear();

        ItemStack filler = menuConfig.filler("request");
        fill(filler);

        OfflinePlayer requester = Bukkit.getOfflinePlayer(requesterUuid);
        String requesterName = requester.getName() != null ? requester.getName() : "Unknown";
        String[] placeholders = {"%player%", requesterName};

        if (menuConfig.isAllowed(player, "request", "head")) {
            setItem(
                    menuConfig.slot("request", "head", 13),
                    menuConfig.item("request", "head", requester, placeholders)
            );
        }

        if (menuConfig.isAllowed(player, "request", "accept")) {
            setItem(
                    menuConfig.slot("request", "accept", 11),
                    menuConfig.item("request", "accept", placeholders),
                    context -> {
                        context.close();
                        marriageService.acceptRequest(player, requesterUuid);
                    }
            );
        }

        if (menuConfig.isAllowed(player, "request", "deny")) {
            setItem(
                    menuConfig.slot("request", "deny", 15),
                    menuConfig.item("request", "deny", placeholders),
                    context -> {
                        context.close();
                        marriageService.denyRequest(player, requesterUuid);
                    }
            );
        }
    }
}
