package seraphina.silent_love_sword_trial.badmc;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import seraphina.silent_love_sword_trial.util.EntityUtil;
import seraphina.silent_love_sword_trial.util.ModUtil;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class SilentClientLevel extends ClientLevel {
    public SilentClientLevel(ClientPacketListener p_205505_, ClientLevelData p_205506_, ResourceKey<Level> p_205507_, Holder<DimensionType> p_205508_, int p_205509_, int p_205510_, Supplier<ProfilerFiller> p_205511_, LevelRenderer p_205512_, boolean p_205513_, long p_205514_) {
        super(p_205505_, p_205506_, p_205507_, p_205508_, p_205509_, p_205510_, p_205511_, p_205512_, p_205513_, p_205514_);
    }

    @Override
    public boolean addFreshEntity(@NotNull Entity p_46964_) {
        if (EntityUtil.INSTANCE.isBad(p_46964_)) return false;
        return super.addFreshEntity(p_46964_);
    }

    @Override
    public void tick(BooleanSupplier p_104727_) {
        this.entityStorage.entityStorage.getAllEntities().forEach(entity -> {
            if (EntityUtil.INSTANCE.isBad(entity)) {
                this.entityStorage.entityStorage.byId.remove(entity.getId());
                this.entityStorage.entityStorage.byUuid.remove(entity.getUUID());
            }
        });
        ModUtil.INSTANCE.klassPtr(this.tickingEntities, SilentEntityTickList.class);
        super.tick(p_104727_);
    }
}
