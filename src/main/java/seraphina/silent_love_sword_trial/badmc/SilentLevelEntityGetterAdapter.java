package seraphina.silent_love_sword_trial.badmc;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.LevelEntityGetterAdapter;
import seraphina.silent_love_sword_trial.util.EntityUtil;

import java.util.ArrayList;
import java.util.List;

public class SilentLevelEntityGetterAdapter<T extends EntityAccess> extends LevelEntityGetterAdapter<T> {
    public SilentLevelEntityGetterAdapter(EntityLookup<T> p_156943_, EntitySectionStorage<T> p_156944_) {
        super(p_156943_, p_156944_);
    }

    @Override
    public Iterable<T> getAll() {
        Iterable<T> old = this.visibleEntities.getAllEntities();
        List<T> now = new ArrayList<>();
        for (T entity : old) {
            if (!EntityUtil.INSTANCE.isBad(entity)) {
                now.add(entity);
            }
        }
        return now;
    }
}
