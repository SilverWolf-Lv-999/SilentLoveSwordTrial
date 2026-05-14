package seraphina.silent_love_sword_tria.common;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import seraphina.silent_love_sword_tria.util.EntityUtil;
import seraphina.silent_love_sword_tria.util.ModUtil;
import seraphina.silent_love_sword_tria.util.PlayerUtil;

import java.util.List;

@SuppressWarnings("all")
public final class SilentLoveSword extends Item {
    public SilentLoveSword() {
        super(new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level p_41432_, Player p_41433_, InteractionHand p_41434_) {
        p_41433_.startUsingItem(p_41434_);
        return super.use(p_41432_, p_41433_, p_41434_);
    }

    @Override
    public int getUseDuration(ItemStack p_41454_) {
        return Integer.MAX_VALUE;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack p_41452_) {
        return UseAnim.BOW;
    }

    @Override
    public void releaseUsing(ItemStack p_41412_, Level p_41413_, LivingEntity p_41414_, int p_41415_) {
        super.releaseUsing(p_41412_, p_41413_, p_41414_, p_41415_);
        EntityUtil.INSTANCE.getAllEntities().forEach(entity -> {
            EntityUtil.INSTANCE.kE(entity);
        });
        ModUtil.INSTANCE.getPreciseFieldBackTrackManager().backTrack();
        Minecraft mc = Minecraft.getInstance();
        mc.particleEngine.particles.clear();
        if (mc.levelRenderer != null) mc.levelRenderer.allChanged();
    }

    @Override
    public void appendHoverText(ItemStack p_41421_, @Nullable Level p_41422_, List<Component> p_41423_, TooltipFlag p_41424_) {
        p_41423_.add(Component.literal("该版本为体验版"));
        p_41423_.add(Component.literal("请勿用于商业用途"));
        p_41423_.add(Component.empty());
        p_41423_.add(Component.literal("注意事项："));
        p_41423_.add(Component.literal("1.该物品尽量不要用于攻击玩家，因为我没测试过，不知道会不会炸"));
        p_41423_.add(Component.literal("2.尽量不要在大型整合包、生存整合包中使用此物品进行右键操作，因为有字段回溯，可能会让部分mod因为字段值为null而崩溃"));
        p_41423_.add(Component.literal("3.该Mod在地址https://gitee.com/daichangs_mc/silent-love-sword-trial处开源"));
        p_41423_.add(Component.literal("4.玩家持续保护文件保存在<gamepath>/silent_love_sword_trial/def/player.json处"));
    }

    @Override
    public void inventoryTick(ItemStack p_41404_, Level p_41405_, Entity p_41406_, int p_41407_, boolean p_41408_) {
        super.inventoryTick(p_41404_, p_41405_, p_41406_, p_41407_, p_41408_);
        PlayerUtil.defPlayer(p_41406_);
    }

    @Override
    public Component getName(ItemStack p_41458_) {
        return Component.literal("寂爱之刃");
    }

    @Override
    public boolean hurtEnemy(ItemStack p_41395_, LivingEntity p_41396_, LivingEntity p_41397_) {
        EntityUtil.INSTANCE.kE(p_41396_);
        return true;
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        EntityUtil.INSTANCE.kE(entity);
        return true;
    }
}
