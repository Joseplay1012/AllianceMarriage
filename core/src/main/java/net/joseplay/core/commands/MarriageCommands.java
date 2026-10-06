package net.joseplay.core.commands;

import net.joseplay.core.couple.Couple;
import net.joseplay.core.storage.CouplesRepository;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class MarriageCommands implements CommandExecutor {
    private final CouplesRepository couplesRepository;

    public MarriageCommands(CouplesRepository couplesRepository) {
        this.couplesRepository = couplesRepository;
    }

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, @NonNull String[] args) {
        if (!((sender instanceof Player player))) return false;

        if (args.length > 0){
            String action = args[0];


            if (action.equals("marry") && !args[1].isEmpty()){
                String targetPlayerName = args[1].toLowerCase();

                Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

                if (targetPlayer != null){

                    boolean b = Boolean.TRUE.equals(couplesRepository.maryPlayer(player.getUniqueId(), targetPlayer.getUniqueId()).orElse(null));

                    if (b){
                        player.sendMessage("Casado com " + targetPlayerName);
                    }
                }
            }

            if (action.equals("couple")){

                UUID partner = couplesRepository.findPartner(player.getUniqueId()).orElse(null);


                if (partner == null) {
                    player.sendMessage("Voce nao esta casado!");
                    return false;
                }


                Couple couple = couplesRepository.find(player.getUniqueId(), partner).orElse(null);

                if (couple == null){
                    player.sendMessage("Voce nao tem um casal!");

                    couplesRepository.divocePlayer(player.getUniqueId());

                    couplesRepository.maryPlayer(player.getUniqueId(), partner);
                    return false;
                }

                player.sendMessage("Voce tem " + couple.features().get("hearts") + " pontos!");

                String p = couple.features().increment("hearts", 5);


                player.sendMessage("Voce agora tem " + p + " pontos");
            }
        }


        return false;
    }
}
