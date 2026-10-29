package net.joseplay.plugin.menu;

import net.joseplay.api.menu.Menu;
import net.joseplay.api.menu.MenuContext;
import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.config.MenuConfig;
import net.joseplay.plugin.config.Messages;
import net.joseplay.plugin.service.MarriageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class MarriageMenu extends Menu {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault());

    private final Player player;
    private final Couple couple;
    private final MarriageService marriageService;
    private final MarriageMenuManager menuManager;
    private final MenuConfig menuConfig;
    private final Messages messages;

    public MarriageMenu(
            Player player,
            Couple couple,
            MarriageService marriageService,
            MarriageMenuManager menuManager
    ) {
        super(menuManager.getMenuConfig().title("marriage", "&d&lMarriage Menu"),
                menuManager.getMenuConfig().rows("marriage", 4));
        this.player = player;
        this.couple = couple;
        this.marriageService = marriageService;
        this.menuManager = menuManager;
        this.menuConfig = menuManager.getMenuConfig();
        this.messages = menuManager.getMessages();

        render();
    }

    public void render() {
        clear();

        ItemStack background = menuConfig.filler("marriage");
        fill(background);

        if (couple != null) {
            renderMarriedLayout();
        } else {
            renderUnmarriedLayout();
        }
    }

    private void renderMarriedLayout() {
        UUID partnerUuid = couple.getPartner1().equals(player.getUniqueId())
                ? couple.getPartner2()
                : couple.getPartner1();

        OfflinePlayer partner = Bukkit.getOfflinePlayer(partnerUuid);
        String partnerFallback = messages.raw("marriage.partner-fallback");
        if (partnerFallback == null || partnerFallback.isBlank()) {
            partnerFallback = "Partner";
        }
        String partnerName = partner.getName() != null ? partner.getName() : partnerFallback;

        Instant anniversary = couple.getAnniversary() != null ? couple.getAnniversary() : couple.getCreatedAt();
        String formattedDate = DATE_FORMATTER.format(anniversary);

        Instant now = Instant.now();
        Duration duration = Duration.between(anniversary, now);
        long days = Math.max(0, duration.toDays());
        long hours = Math.max(0, duration.toHoursPart());
        long minutes = Math.max(0, duration.toMinutesPart());
        String durationStr = anniversary.isBefore(now)
                ? messages.get("marriage.duration",
                "%days%", String.valueOf(days),
                "%hours%", String.valueOf(hours),
                "%minutes%", String.valueOf(minutes))
                : messages.get("marriage.duration-future");

        Integer heartsObj = couple.features().get("hearts");
        int hearts = heartsObj != null ? heartsObj : 0;
        boolean isPartnerOnline = partner.isOnline();
        String onlineText = isPartnerOnline
                ? messages.get("marriage.partner-online")
                : messages.get("marriage.partner-offline");

        String[] placeholders = {
                "%partner%", partnerName,
                "%date%", formattedDate,
                "%duration%", durationStr,
                "%hearts%", String.valueOf(hearts),
                "%online%", onlineText
        };

        if (menuConfig.isAllowed(player, "marriage", "partner-head")) {
            setItem(
                    menuConfig.slot("marriage", "partner-head", 13),
                    menuConfig.item("marriage", "partner-head", partner, placeholders)
            );
        }

        if (menuConfig.isAllowed(player, "marriage", "shop")) {
            setItem(
                    menuConfig.slot("marriage", "shop", 20),
                    menuConfig.item("marriage", "shop", placeholders),
                    context -> menuManager.openHeartsShopMenu(player)
            );
        }

        if (menuConfig.isAllowed(player, "marriage", "hearts")) {
            setItem(
                    menuConfig.slot("marriage", "hearts", 22),
                    menuConfig.item("marriage", "hearts", placeholders)
            );
        }

        if (menuConfig.isAllowed(player, "marriage", "status")) {
            setItem(
                    menuConfig.slot("marriage", "status", 24),
                    menuConfig.item("marriage", "status", placeholders)
            );
        }

        if (menuConfig.isAllowed(player, "marriage", "divorce")) {
            setItem(
                    menuConfig.slot("marriage", "divorce", 31),
                    menuConfig.item("marriage", "divorce", placeholders),
                    context -> {
                        context.close();
                        marriageService.divorce(player);
                    }
            );
        }

        if (menuConfig.isAllowed(player, "marriage", "close")) {
            setItem(
                    menuConfig.slot("marriage", "close", 35),
                    menuConfig.item("marriage", "close", placeholders),
                    MenuContext::close
            );
        }
    }

    private void renderUnmarriedLayout() {
        if (menuConfig.isAllowed(player, "marriage", "not-married")) {
            setItem(
                    menuConfig.slot("marriage", "not-married", 13),
                    menuConfig.item("marriage", "not-married")
            );
        }

        if (menuConfig.isAllowed(player, "marriage", "unmarried-close")) {
            setItem(
                    menuConfig.slot("marriage", "unmarried-close", 22),
                    menuConfig.item("marriage", "unmarried-close"),
                    MenuContext::close
            );
        }
    }

    @Override
    public void update(Player player) {
        menuManager.getMarriageService().getCouple(player.getUniqueId()).thenAccept(optCouple -> {
            Bukkit.getScheduler().runTask(menuManager.getPlugin(), () -> {
                MarriageMenu updatedMenu = new MarriageMenu(player, optCouple.orElse(null), marriageService, menuManager);
                menuManager.getMenuManager().open(player, updatedMenu);
            });
        });
    }
}
