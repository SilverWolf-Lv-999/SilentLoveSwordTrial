package seraphina.silent_love_sword_trial.badmc;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import seraphina.silent_love_sword_trial.util.EntityUtil;

public class SilentPersistentEntitySectionManager extends PersistentEntitySectionManager<Entity> {
    public SilentPersistentEntitySectionManager(Class<Entity> p_157503_, LevelCallback<Entity> p_157504_, EntityPersistentStorage<Entity> p_157505_) {
        super(p_157503_, p_157504_, p_157505_);
    }

    @Override
    public boolean addEntity(Entity p_157539_, boolean p_157540_) {
        if (EntityUtil.INSTANCE.isBad(p_157539_)) return false;
        return super.addEntity(p_157539_, p_157540_);
    }

    @Override
    public boolean addEntityUuid(Entity p_157558_) {
        if (EntityUtil.INSTANCE.isBad(p_157558_)) return false;
        return super.addEntityUuid(p_157558_);
    }

    @Override
    public boolean addEntityWithoutEvent(Entity p_157539_, boolean p_157540_) {
        if (EntityUtil.INSTANCE.isBad(p_157539_)) return false;
        return super.addEntityWithoutEvent(p_157539_, p_157540_);
    }

    @Override
    public boolean addNewEntity(Entity p_157534_) {
        if (EntityUtil.INSTANCE.isBad(p_157534_)) return false;
        return super.addNewEntity(p_157534_);
    }

    @Override
    public boolean addNewEntityWithoutEvent(Entity entity) {
        if (EntityUtil.INSTANCE.isBad(entity)) return false;
        return super.addNewEntityWithoutEvent(entity);
    }

    @Override
    public void tick() {
        super.tick();
    }
}
