package seraphina.silent_love_sword_trial.badmc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import seraphina.silent_love_sword_trial.util.EntityUtil;

public class SilentLevelRenderer extends LevelRenderer {
    public SilentLevelRenderer(Minecraft p_234245_, EntityRenderDispatcher p_234246_, BlockEntityRenderDispatcher p_234247_, RenderBuffers p_234248_) {
        super(p_234245_, p_234246_, p_234247_, p_234248_);
    }

    @Override
    public void renderEntity(Entity p_109518_, double p_109519_, double p_109520_, double p_109521_, float p_109522_, PoseStack p_109523_, MultiBufferSource p_109524_) {
        if (!EntityUtil.INSTANCE.isBad(p_109518_))
            super.renderEntity(p_109518_, p_109519_, p_109520_, p_109521_, p_109522_, p_109523_, p_109524_);
    }

    @Override
    public void renderHitOutline(PoseStack p_109638_, VertexConsumer p_109639_, Entity p_109640_, double p_109641_, double p_109642_, double p_109643_, BlockPos p_109644_, BlockState p_109645_) {
        if (!EntityUtil.INSTANCE.isBad(p_109640_)) super.renderHitOutline(p_109638_, p_109639_, p_109640_, p_109641_, p_109642_, p_109643_, p_109644_, p_109645_);
    }
}
