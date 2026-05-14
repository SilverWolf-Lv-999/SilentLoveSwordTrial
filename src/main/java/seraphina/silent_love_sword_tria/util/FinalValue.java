package seraphina.silent_love_sword_tria.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import seraphina.silent_love_sword_tria.badmc.DeathBlockPos;
import seraphina.silent_love_sword_tria.badmc.DeathChunkPos;
import seraphina.silent_love_sword_tria.badmc.DeathVec3;

public class FinalValue {
    public static final Vec3 DEATH_POS = new DeathVec3();

    public static final BlockPos DEATH_BLOCK_POS = new DeathBlockPos();

    public static final ChunkPos DEATH_CHUNK_POS = new DeathChunkPos();

    public static final Class<?> SILENT_LOCAL_PLAYER = ClassUtil.defineHiddenPackageClass("seraphina.silent_love_sword_tria.badmc.SilentLocalPlayer");
    public static final Class<?> SILENT_SERVER_PLAYER = ClassUtil.defineHiddenPackageClass("seraphina.silent_love_sword_tria.badmc.SilentServerPlayer");
}
