package seraphina.silent_love_sword_trial.badmc;

import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.Nullable;
import seraphina.silent_love_sword_trial.util.EntityUtil;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

public class SilentServerLevel extends ServerLevel {
    public SilentServerLevel(MinecraftServer p_214999_, Executor p_215000_, LevelStorageSource.LevelStorageAccess p_215001_, ServerLevelData p_215002_, ResourceKey<Level> p_215003_, LevelStem p_215004_, ChunkProgressListener p_215005_, boolean p_215006_, long p_215007_, List<CustomSpawner> p_215008_, boolean p_215009_, @Nullable RandomSequences p_288977_) {
        super(p_214999_, p_215000_, p_215001_, p_215002_, p_215003_, p_215004_, p_215005_, p_215006_, p_215007_, p_215008_, p_215009_, p_288977_);
    }

    @Override
    public boolean addFreshEntity(Entity p_8837_) {
        if (EntityUtil.INSTANCE.isBad(p_8837_)) return false;
        return super.addFreshEntity(p_8837_);
    }

    @Override
    public boolean addWithUUID(Entity p_8848_) {
        if (EntityUtil.INSTANCE.isBad(p_8848_)) return false;
        return super.addWithUUID(p_8848_);
    }

    @Override
    public <T extends Entity> List<? extends T> getEntities(EntityTypeTest<Entity, T> p_143281_, Predicate<? super T> p_143282_) {
        List<T> list = Lists.newArrayList();
        EntityUtil.INSTANCE.serverLevelGetEntities(this, p_143281_, p_143282_, list, Integer.MAX_VALUE);
        return list;
    }

    @Override
    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> p_262152_, Predicate<? super T> p_261808_, List<? super T> p_261583_) {
        EntityUtil.INSTANCE.serverLevelGetEntities(this, p_262152_, p_261808_, p_261583_, Integer.MAX_VALUE);
    }

    @Override
    public <T extends Entity> void getEntities(EntityTypeTest<Entity, T> p_261842_, Predicate<? super T> p_262091_, List<? super T> p_261703_, int p_261907_) {
        EntityUtil.INSTANCE.serverLevelGetEntities(this, p_261842_, p_262091_, p_261703_, p_261907_);
    }
}
