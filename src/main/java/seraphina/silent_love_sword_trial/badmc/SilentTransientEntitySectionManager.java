package seraphina.silent_love_sword_trial.badmc;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import seraphina.silent_love_sword_trial.util.EntityUtil;

public class SilentTransientEntitySectionManager<T extends EntityAccess> extends TransientEntitySectionManager<T> {

    public SilentTransientEntitySectionManager(Class<T> p_157643_, LevelCallback<T> p_157644_) {
        super(p_157643_, p_157644_);
    }

    @Override
    public void addEntity(T p_157654_) {
        if (!EntityUtil.INSTANCE.isBad(p_157654_)) super.addEntity(p_157654_);
    }

}
