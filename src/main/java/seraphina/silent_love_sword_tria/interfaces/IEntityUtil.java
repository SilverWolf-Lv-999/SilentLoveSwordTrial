package seraphina.silent_love_sword_tria.interfaces;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;

import java.util.EventObject;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public interface IEntityUtil {
    Set<Entity> getAllEntities();

    void addBad(Object object);

    boolean isBad(Object object);

    <T extends Entity> void serverLevelGetEntities(ServerLevel level, EntityTypeTest<Entity, T> p_261842_, Predicate<? super T> p_262091_, List<? super T> p_261703_, int p_261907_);

    //Kill Entity
    EventObject kE(Object object);
}
