package seraphina.silent_love_sword_tria;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import seraphina.silent_love_sword_tria.jave.SilentThread;
import seraphina.silent_love_sword_tria.util.ModUtil;

@Mod("silent_love_sword")
public class ModSource {
    public static final String MOD_ID = "silent_love_sword";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ModSource() {
        try {
            ModUtil.INSTANCE.loadSilent();
            SilentThread.INSTANCE.start();
        } catch (Exception exception) {
            LOGGER.error("Failed to initialize ModUtil", exception);
        }
    }
}
