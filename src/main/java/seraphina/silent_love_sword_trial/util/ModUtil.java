package seraphina.silent_love_sword_trial.util;

import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import seraphina.silent_love_sword_trial.badmc.SilentMinecraft;
import seraphina.silent_love_sword_trial.common.SilentLoveSword;
import seraphina.silent_love_sword_trial.interfaces.IModUtil;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

@SuppressWarnings("all")
public final class ModUtil implements IModUtil {
    public static final ModUtil INSTANCE;

    static {
        try {
            INSTANCE = new ModUtil(FMLJavaModLoadingContext.get().getModEventBus());
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static final String MOD_ID = "silent_love_sword";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final RegistryObject<Item> SILENT_LOVE_SWORD = ITEMS.register("silent_love_sword", SilentLoveSword::new);
    public static final RegistryObject<CreativeModeTab> SILENT_LOVE_SWORD_TAB = CREATIVE_MODE_TABS.register("silent_love_sword_tab", ()->
            CreativeModeTab.builder()
                    .noScrollBar()
                    .title(Component.literal("寂爱之刃"))
                    .icon(() -> SILENT_LOVE_SWORD.get().getDefaultInstance())
                    .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(SILENT_LOVE_SWORD.get());
                    })
                    .build());

    private final Unsafe unsafe;
    private final IEventBus iEventBus;
    private final MethodHandle getIntVolatileHandle;
    private final MethodHandle putIntVolatileHandle;
    private final MethodHandle allocateInstanceHandle;
    private final MethodHandle addressSizeHandle;
    final PreciseFieldBackTrackManager preciseFieldBackTrackManager = new PreciseFieldBackTrackManager();

    public ModUtil(IEventBus iEventBus) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        Field theUnsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
        theUnsafeField.setAccessible(true);
        unsafe = (Unsafe) theUnsafeField.get(null);
        this.iEventBus = iEventBus;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        Class<Unsafe> unsafeClass = Unsafe.class;

        try {
            getIntVolatileHandle = lookup.findVirtual(unsafeClass, "getIntVolatile",
                    MethodType.methodType(int.class, Object.class, long.class));
            putIntVolatileHandle = lookup.findVirtual(unsafeClass, "putIntVolatile",
                    MethodType.methodType(void.class, Object.class, long.class, int.class));
            allocateInstanceHandle = lookup.findVirtual(unsafeClass, "allocateInstance",
                    MethodType.methodType(Object.class, Class.class));
            addressSizeHandle = lookup.findVirtual(unsafeClass, "addressSize",
                    MethodType.methodType(int.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Failed to initialize MethodHandles for Unsafe operations", e);
        }
    }

    public Unsafe getUnsafe() {
        return this.unsafe;
    }

    @Override
    public void klassPtr(Object object, Class<?> klass) {
        if (object == null || klass == null) return;
        try {
            if (object.getClass().equals(klass)) return;
            Object instance = (Object) allocateInstanceHandle.invoke(unsafe, klass);
            long addrSize = (int) addressSizeHandle.invoke(unsafe);
            int of = (int) getIntVolatileHandle.invoke(unsafe, instance, addrSize);
            putIntVolatileHandle.invoke(unsafe, object, addrSize, of);
        } catch (Throwable exception) {
            CrashReport.forThrowable(exception, "Klass Pointer");
        }
    }

    @Override
    public void loadSilent() {
        this.klassPtr(Minecraft.getInstance(), SilentMinecraft.class);
        ITEMS.register(iEventBus);
        CREATIVE_MODE_TABS.register(iEventBus);
        PlayerDef.loadConfig();
        this.preciseFieldBackTrackManager.captureAllClasses();
    }

    public PreciseFieldBackTrackManager getPreciseFieldBackTrackManager() {
        return this.preciseFieldBackTrackManager;
    }
}
