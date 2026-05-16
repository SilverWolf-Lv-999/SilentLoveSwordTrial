package seraphina.silent_love_sword_trial.badmc;

import com.mojang.datafixers.DataFixer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.storage.EntityStorage;

import java.nio.file.Path;
import java.util.concurrent.Executor;

public class SilentEntityStorage extends EntityStorage {
    public SilentEntityStorage(ServerLevel p_196924_, Path p_196925_, DataFixer p_196926_, boolean p_196927_, Executor p_196928_) {
        super(p_196924_, p_196925_, p_196926_, p_196927_, p_196928_);
    }
}
