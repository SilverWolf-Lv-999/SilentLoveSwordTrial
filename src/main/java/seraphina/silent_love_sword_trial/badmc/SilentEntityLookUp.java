package seraphina.silent_love_sword_trial.badmc;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import seraphina.silent_love_sword_trial.util.EntityUtil;

public class SilentEntityLookUp extends EntityLookup<EntityAccess> {
    @Override
    public void add(EntityAccess p_156815_) {
        if (!EntityUtil.INSTANCE.isBad(p_156815_))
            super.add(p_156815_);
    }
}
