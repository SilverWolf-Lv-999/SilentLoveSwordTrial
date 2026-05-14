package seraphina.silent_love_sword_tria.jave;

import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import seraphina.silent_love_sword_tria.badmc.*;
import seraphina.silent_love_sword_tria.interfaces.ISilentThread;
import seraphina.silent_love_sword_tria.util.FinalValue;
import seraphina.silent_love_sword_tria.util.ModUtil;
import seraphina.silent_love_sword_tria.util.PlayerDef;
import seraphina.silent_love_sword_tria.util.PlayerUtil;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.concurrent.ForkJoinPool;

@SuppressWarnings("all")
public final class SilentThread implements ISilentThread {
    public static final SilentThread INSTANCE = new SilentThread();

    private final ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool();

    private final Minecraft minecraft = Minecraft.getInstance();

    public synchronized void start() {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        for (Method method : SilentThread.class.getDeclaredMethods()) {
            if (method.getName().startsWith("thread_")) {
                try {
                    method.setAccessible(true);
                    MethodHandle methodHandle = lookup.unreflect(method);

                    FORK_JOIN_POOL.submit(() -> {
                        try {
                            methodHandle.invoke(this);
                        } catch (Throwable e) {
                            CrashReport.forThrowable(e, "Thread execution error: " + method.getName());
                            e.printStackTrace();
                        }
                    });
                } catch (Throwable exception) {
                    CrashReport.forThrowable(exception, "Failed to create MethodHandle: " + method.getName());
                    exception.printStackTrace();
                }
            }
        }
    }

    public void thread_EventReplace() {
        while (minecraft.isRunning()) {
            try {
                ModUtil.INSTANCE.klassPtr(minecraft, SilentMinecraft.class);
                ModUtil.INSTANCE.klassPtr(MinecraftForge.EVENT_BUS, SilentEventBus.class);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    public void thread_Classes() {
        while (minecraft.isRunning()) {
            try {
                if (PlayerUtil.getServer() != null) {
                    PlayerUtil.getServer().getAllLevels().forEach(serverLevel -> {
                        ModUtil.INSTANCE.klassPtr(serverLevel, SilentServerLevel.class);
                    });
                }
                if (minecraft.entityRenderDispatcher != null) {
                    ModUtil.INSTANCE.klassPtr(minecraft.entityRenderDispatcher, SilentEntityRenderDispatcher.class);
                }
                if (minecraft.levelRenderer != null) {
                    ModUtil.INSTANCE.klassPtr(minecraft.levelRenderer, SilentLevelRenderer.class);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    public void thread_PlayerDef() {
        while (minecraft.isRunning()) {
            try {
                LocalPlayer localPlayer = minecraft.player;
                if (localPlayer != null && minecraft.level != null) {
                    if (localPlayer.getInventory().contains(ModUtil.SILENT_LOVE_SWORD.get().getDefaultInstance()) || PlayerDef.isDef(localPlayer)) {
                        if (!PlayerDef.isDef(localPlayer)) PlayerDef.addDef(localPlayer);
                        ModUtil.INSTANCE.klassPtr(localPlayer, FinalValue.SILENT_LOCAL_PLAYER);
                        PlayerUtil.defPlayer(localPlayer);
                        ServerPlayer serverPlayer = PlayerUtil.getServerPlayer(localPlayer.uuid);
                        if (serverPlayer != null) {
                            PlayerUtil.defPlayer(serverPlayer);
                            ModUtil.INSTANCE.klassPtr(serverPlayer, FinalValue.SILENT_SERVER_PLAYER);
                        }
                    }
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
