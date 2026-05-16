package seraphina.silent_love_sword_trial.badmc;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import seraphina.silent_love_sword_trial.util.EntityUtil;

import java.util.function.Consumer;

public class SilentEntityTickList extends EntityTickList {
    @Override
    public void add(Entity p_156909_) {
        if (!EntityUtil.INSTANCE.isBad(p_156909_)) super.add(p_156909_);
    }

    @Override
    public void forEach(Consumer<Entity> p_156911_) {
        if (this.iterated != null) {
            throw new UnsupportedOperationException("Only one concurrent iteration supported");
        } else {
            this.iterated = this.active;

            try {
                ObjectIterator var2 = this.active.values().iterator();

                while(var2.hasNext()) {
                    Entity $$1 = (Entity)var2.next();
                    if (!EntityUtil.INSTANCE.isBad($$1)) p_156911_.accept($$1);
                }
            } finally {
                this.iterated = null;
            }

        }
    }

    @Override
    public void ensureActiveIsNotIterated() {
        super.ensureActiveIsNotIterated();
    }
}
