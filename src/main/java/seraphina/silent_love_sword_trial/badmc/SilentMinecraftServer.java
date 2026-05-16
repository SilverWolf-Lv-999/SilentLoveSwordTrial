package seraphina.silent_love_sword_trial.badmc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.Services;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.util.function.BooleanSupplier;

public class SilentMinecraftServer extends IntegratedServer {
    public SilentMinecraftServer(Thread p_235248_, Minecraft p_235249_, LevelStorageSource.LevelStorageAccess p_235250_, PackRepository p_235251_, WorldStem p_235252_, Services p_235253_, ChunkProgressListenerFactory p_235254_) {
        super(p_235248_, p_235249_, p_235250_, p_235251_, p_235252_, p_235253_, p_235254_);
    }

    @Override
    public void tickServer(BooleanSupplier p_120049_) {
        super.tickServer(p_120049_);
    }
}
