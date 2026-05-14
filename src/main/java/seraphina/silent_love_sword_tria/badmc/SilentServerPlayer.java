package seraphina.silent_love_sword_tria.badmc;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import seraphina.silent_love_sword_tria.util.PlayerUtil;

public class SilentServerPlayer extends ServerPlayer {
    public SilentServerPlayer(MinecraftServer p_254143_, ServerLevel p_254435_, GameProfile p_253651_) {
        super(p_254143_, p_254435_, p_253651_);
    }

    @Override
    public float getHealth() {
        return 20.0F;
    }

    @Override
    public boolean isAlive() {
        return true;
    }

    @Override
    public boolean isDeadOrDying() {
        return false;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        PlayerUtil.defPlayer(this);
    }
}
