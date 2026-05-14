package seraphina.silent_love_sword_tria.badmc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import org.jetbrains.annotations.Nullable;
import seraphina.silent_love_sword_tria.util.ClassUtil;
import seraphina.silent_love_sword_tria.util.PlayerDef;

public class SilentMinecraft extends Minecraft {
    public SilentMinecraft(GameConfig p_91084_) {
        super(p_91084_);
    }

    @Override
    public void setScreen(@Nullable Screen p_91153_) {
        if (p_91153_ != null && PlayerDef.isDef(this.player) && (p_91153_ instanceof DeathScreen || ClassUtil.isModClass(p_91153_.getClass()))) return;
        super.setScreen(p_91153_);
    }

    @Override
    public boolean isRunning() {
        return super.isRunning();
    }
}
