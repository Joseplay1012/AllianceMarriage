package net.joseplay.plugin.menu;

import net.joseplay.api.menu.Menu;
import net.joseplay.api.menu.MenuContext;
import net.joseplay.core.couple.Couple;
import net.joseplay.plugin.service.MarriageService;
import net.joseplay.plugin.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

    public MarriageMenu(
            Player player,
            Couple couple,
            MarriageService marriageService,
            MarriageMenuManager menuManager
    ) {
        super("&d&lMarriage Menu", 4);
        this.player = player;
        this.couple = couple;
        this.marriageService = marriageService;
        this.menuManager = menuManager;

        render();
    }

    public void render() {
        clear();

        ItemStack background = ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE)
                .name(" ")
                .hideToolTip(true)
                .build();
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
        String partnerName = partner.getName() != null ? partner.getName() : "Partner";

        Instant anniversary = couple.getAnniversary() != null ? couple.getAnniversary() : couple.getCreatedAt();
        String formattedDate = DATE_FORMATTER.format(anniversary);

        Instant now = Instant.now();

        Duration duration = Duration.between(anniversary, now);
        long days = Math.max(0, duration.toDays());
        long hours = Math.max(0, duration.toHoursPart());
        long minutes = Math.max(0, duration.toMinutesPart());
        String durationStr = days + " days, " + hours + " hours, " + minutes + " minutes";

        Integer heartsObj = couple.features().get("hearts");
        int hearts = heartsObj != null ? heartsObj : 0;

        // Slot 13: Partner Head
        ItemStack partnerHead = ItemBuilder.of(Material.PLAYER_HEAD)
                .skullOwner(partner)
                .name("&d&l" + partnerName)
                .lore(
                        "&7Married since: &e" + formattedDate,
                        "&7Marriage duration: &e" + (anniversary.isBefore(now) ? durationStr : "&c Did you get married in the future?"),
                        "&7Status: &aHappily Married"
                )
                .build();
        setItem(13, partnerHead);

        // Slot 20: Hearts Shop button
        ItemStack shopButton = ItemBuilder.of(Material.CHEST)
                .name("&c&l♥ Hearts Shop")
                .lore(
                        "&7Browse and purchase exclusive rewards",
                        "&7using your shared Hearts!",
                        "",
                        "&aClick to open shop!"
                )
                .build();
        setItem(20, shopButton, context -> menuManager.openHeartsShopMenu(player));

        // Slot 22: Hearts balance indicator
        ItemStack heartsIndicator = ItemBuilder.of(Material.NETHER_STAR)
                .name("&c&l♥ " + hearts + " Hearts")
                .lore(
                        "&7Your couple's current Hearts balance.",
                        "&7Complete activities together to earn more!"
                )
                .build();
        setItem(22, heartsIndicator);

        // Slot 24: Status / Info button
        boolean isPartnerOnline = partner.isOnline();
        ItemStack statusButton = ItemBuilder.of(Material.BOOK)
                .name("&e&lMarriage Status")
                .lore(
                        "&7Partner: &f" + partnerName,
                        "&7Online: " + (isPartnerOnline ? "&aOnline" : "&7Offline"),
                        "&7Anniversary: &e" + formattedDate,
                        "&7Duration: &e" + (anniversary.isBefore(now) ? durationStr : "&c Did you get married in the future?")
                )
                .build();
        setItem(24, statusButton);

        // Slot 31: Divorce button
        ItemStack divorceButton = ItemBuilder.of(Material.SHEARS)
                .name("&c&lDivorce")
                .lore(
                        "&7Click to end your marriage with &f" + partnerName + "&7.",
                        "&cWarning: This action is permanent!"
                )
                .build();
        setItem(31, divorceButton, context -> {
            context.close();
            marriageService.divorce(player);
        });

        // Slot 35: Close button
        ItemStack closeButton = ItemBuilder.of(Material.ARROW)
                .name("&7Close")
                .lore("&7Close this menu.")
                .build();
        setItem(35, closeButton, MenuContext::close);
    }

    private void renderUnmarriedLayout() {
        ItemStack notMarriedItem = ItemBuilder.of(Material.BARRIER)
                .name("&cNot Married")
                .lore(
                        "&7You are not currently married.",
                        "",
                        "&eUse /marry <player> to propose to someone!"
                )
                .build();
        setItem(13, notMarriedItem);

        ItemStack closeButton = ItemBuilder.of(Material.ARROW)
                .name("&7Close")
                .build();
        setItem(22, closeButton, context -> context.close());
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
