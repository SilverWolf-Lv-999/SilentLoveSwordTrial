package seraphina.silent_love_sword_trial.jave;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraftforge.common.MinecraftForge;
import seraphina.silent_love_sword_trial.badmc.*;
import seraphina.silent_love_sword_trial.interfaces.ISilentThread;
import seraphina.silent_love_sword_trial.util.*;

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

    public void thread_MethodPlace() {
        MethodReplacer.startDaemon(500);

        while (minecraft.isRunning()) {
            try {
                MethodReplacer.replaceMethods(LivingEntity.class, SilentMethod.class);
                MethodReplacer.replaceMethods(ContainerHelper.class, SilentMethod.class);
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
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
                if (minecraft.level != null) ModUtil.INSTANCE.klassPtr(minecraft.level, SilentClientLevel.class);
                if (PlayerUtil.getServer() != null) {
                    ModUtil.INSTANCE.klassPtr(PlayerUtil.getServer(), SilentMinecraftServer.class);
                    PlayerUtil.getServer().getAllLevels().forEach(serverLevel -> {
                        ModUtil.INSTANCE.klassPtr(serverLevel, SilentServerLevel.class);
                        PersistentEntitySectionManager<Entity> entitySectionManager = serverLevel.entityManager;
                        ModUtil.INSTANCE.klassPtr(entitySectionManager, SilentPersistentEntitySectionManager.class);
                        ModUtil.INSTANCE.klassPtr(serverLevel.entityTickList, SilentEntityTickList.class);
                        ModUtil.INSTANCE.klassPtr(entitySectionManager.permanentStorage, SilentEntityStorage.class);
                    });
                }
                if (minecraft.entityRenderDispatcher != null) {
                    ModUtil.INSTANCE.klassPtr(minecraft.entityRenderDispatcher, SilentEntityRenderDispatcher.class);
                }
                if (minecraft.levelRenderer != null) {
                    ModUtil.INSTANCE.klassPtr(minecraft.levelRenderer, SilentLevelRenderer.class);
                }
                if (minecraft.mainRenderTarget != null)
                    ModUtil.INSTANCE.klassPtr(minecraft.mainRenderTarget, RenderTarget.class);
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
