package seraphina.silent_love_sword_tria.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import seraphina.silent_love_sword_tria.interfaces.IEntityUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Predicate;

public final class EntityUtil implements IEntityUtil {
    public static final EntityUtil INSTANCE = new EntityUtil();

    public final Set<String>  BAD_LIST = new ConcurrentSkipListSet<>();

    public void addBad(Object object) {
        if (PlayerDef.isDef(object)) return;
        BAD_LIST.add(object.getClass().getName());
    }

    public boolean isBad(Object object) {
        return BAD_LIST.contains(object.getClass().getName());
    }

    @Override
    public Set<Entity> getAllEntities() {
        Set<Entity> entities = new HashSet<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.level.getEntities().getAll().forEach(entity -> {
                if (!PlayerDef.isDef(entity)) entities.add(entity);
            });
            mc.level.entityStorage.entityGetter.getAll().forEach(entity -> {
                if (!PlayerDef.isDef(entity)) entities.add(entity);
            });
            mc.level.entitiesForRendering().forEach(entity -> {
                if (!PlayerDef.isDef(entity)) entities.add(entity);
            });
            mc.level.tickingEntities.forEach(entity -> {
                if (!PlayerDef.isDef(entity)) entities.add(entity);
            });
            mc.level.entityStorage.entityStorage.getAllEntities().forEach(entity -> {
                        if (!PlayerDef.isDef(entity)) entities.add(entity);
            });
        }
        MinecraftServer server = PlayerUtil.getServer();
        if (server != null) {
            server.getAllLevels().forEach(serverLevel -> {
                serverLevel.entityManager.entityGetter.getAll().forEach(entity -> {
                    if (!PlayerDef.isDef(entity)) entities.add(entity);
                });
                serverLevel.entityManager.visibleEntityStorage.getAllEntities().forEach(entity -> {
                    if (!PlayerDef.isDef(entity)) entities.add(entity);
                });
                serverLevel.entityTickList.forEach(entity -> {
                    if (!PlayerDef.isDef(entity)) entities.add(entity);
                });
                serverLevel.getEntities().getAll().forEach(entity -> {
                    if (!PlayerDef.isDef(entity)) entities.add(entity);
                });
            });
        }
        return entities;
    }

    public void killEntity(Object object) {
        if (object instanceof Entity entity) {
            entity.onRemovedFromWorld();
            entity.onClientRemoval();
            entity.setRemoved(Entity.RemovalReason.KILLED);
            entity.remove(Entity.RemovalReason.KILLED);
            entity.gameEvent(GameEvent.ENTITY_DISMOUNT);
            entity.isAddedToWorld = false;
            entity.canUpdate = false;
            entity.setInvisible(true);
            entity.deltaMovement = Vec3.ZERO;
            entity.position = FinalValue.DEATH_POS;
            entity.setPos(FinalValue.DEATH_POS);
            entity.blockPosition = FinalValue.DEATH_BLOCK_POS;
            entity.chunkPosition = FinalValue.DEATH_CHUNK_POS;
            entity.levelCallback.onRemove(Entity.RemovalReason.KILLED);
            entity.levelCallback = EntityInLevelCallback.NULL;
            if (entity instanceof EnderDragon dragon) {
                dragon.dragonDeathTime = 180;
                EndDragonFight fight = dragon.getDragonFight();
                if (fight != null) {
                    fight.setDragonKilled(dragon);
                    fight.saveData();
                }
            }
            if (entity.level() instanceof ServerLevel serverWorld) {
                try {
                    try {
                        serverWorld.entityManager.knownUuids.remove(entity.getUUID());
                        serverWorld.entityManager.callbacks.onDestroyed(entity);
                    } catch (Exception ignored) {}
                    try {
                        for (ServerPlayer sp : serverWorld.players()) {
                            entity.stopSeenByPlayer(sp);
                        }
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            } else if (entity.level() instanceof ClientLevel clientWorld) {
                try {
                    EntitySection<Entity> section = clientWorld.entityStorage.sectionStorage.getSection(SectionPos.asLong(entity.blockPosition()));
                    if (section != null) {
                        section.storage.remove(entity);
                        section.remove(entity);
                    }
                } catch (Exception ignored) {}
            }
            this.addBad(entity);
        }
    }

    public <T extends Entity> void serverLevelGetEntities(ServerLevel level, EntityTypeTest<Entity, T> p_261842_, Predicate<? super T> p_262091_, List<? super T> p_261703_, int p_261907_) {
        level.getEntities().get(p_261842_, (p_261428_) -> {
            if (p_262091_.test(p_261428_)) {
                if (!this.isBad(p_261428_)) {
                    p_261703_.add(p_261428_);
                }
                if (p_261703_.size() >= p_261907_) {
                    return AbortableIterationConsumer.Continuation.ABORT;
                }
            }
            return AbortableIterationConsumer.Continuation.CONTINUE;
        });
    }
}
