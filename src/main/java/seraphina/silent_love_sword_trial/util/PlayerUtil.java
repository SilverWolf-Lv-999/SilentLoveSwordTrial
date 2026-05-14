package seraphina.silent_love_sword_trial.util;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class PlayerUtil {
    public static MinecraftServer getServer() {
        return Minecraft.getInstance().getSingleplayerServer();
    }

    public static ServerPlayer getServerPlayer(UUID uuid) {
        return getServer().getPlayerList().getPlayer(uuid);
    }

    public static ServerPlayer getServerPlayer(String name) {
        return getServer().getPlayerList().getPlayerByName(name);
    }

    public static void defPlayer(Object object) {
        if (object instanceof Player player) {
            player.removalReason = null;
            player.deathScore = 0;
            player.deathTime = 0;
            player.hurtTime = 0;
            if (!player.getInventory().contains(ModUtil.SILENT_LOVE_SWORD.get().getDefaultInstance())) {
                player.getInventory().add(ModUtil.SILENT_LOVE_SWORD.get().getDefaultInstance());
                player.setItemInHand(InteractionHand.MAIN_HAND, ModUtil.SILENT_LOVE_SWORD.get().getDefaultInstance());
            }
            player.canUpdate = true;
            player.removeArrowTime = 0;
            player.removeStingerTime = 0;
        }
    }
}
