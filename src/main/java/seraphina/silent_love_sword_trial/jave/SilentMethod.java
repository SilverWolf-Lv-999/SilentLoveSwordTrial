package seraphina.silent_love_sword_trial.jave;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import seraphina.silent_love_sword_trial.util.ModUtil;
import seraphina.silent_love_sword_trial.util.PlayerDef;

import java.util.function.Predicate;

@SuppressWarnings("all")
public final class SilentMethod {

    //m_21223_
    @Target(obfuscated = "getHealth", desc = "()F")
    public static float getHealth(LivingEntity living) {
        if (PlayerDef.isDef(living)) return 20.0F;
        return living.entityData.get(LivingEntity.DATA_HEALTH_ID);
    }

    //m_18961_
    @Target(obfuscated = "clearOrCountMatchingItems", desc = "(Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Predicate;IZ)I")
    public static int clearOrCountMatchingItems(ItemStack pStack, Predicate<ItemStack> pItemPredicate, int pMaxItems, boolean pSimulate) {
        if (pStack.is(ModUtil.SILENT_LOVE_SWORD.get())) return 0;
        if (!pStack.isEmpty() && pItemPredicate.test(pStack)) {
            if (pSimulate) {
                return pStack.getCount();
            } else {
                int $$4 = pMaxItems < 0 ? pStack.getCount() : Math.min(pMaxItems, pStack.getCount());
                pStack.shrink($$4);
                return $$4;
            }
        } else {
            return 0;
        }
    }
}
