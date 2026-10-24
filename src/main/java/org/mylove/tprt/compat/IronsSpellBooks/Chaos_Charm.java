package org.mylove.tprt.compat.IronsSpellBooks;

import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;

/**
 * 魔力棱镜 (护符): 佩戴时, 自己的魔法投射物额外弹射一次。
 *
 * 与铁魔法原生的「邪恶骨戒」(wicked_bone_ring) 使用同一套机制 (setRicochetLevel),
 * 因此两者可以叠加: 戒指 +1、护符 +1, 合计 2 次弹射。
 * 只对投射物类法术生效 (AbstractMagicProjectile), 范围法术与自身增益不受影响。
 */
public final class Chaos_Charm {

    public static final ModifierId CHARM_ID = new ModifierId("tprt", "source_alloy_charm");

    private Chaos_Charm() {
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) {
            return;
        }
        if (!(event.getEntity() instanceof AbstractMagicProjectile projectile)) {
            return;
        }
        // 施法者可能是玩家本人, 也可能是奥法回响生成的 EchoCaster
        Player player = null;
        if (projectile.getOwner() instanceof Player ownerPlayer) {
            player = ownerPlayer;
        } else if (projectile.getOwner() instanceof org.mylove.tprt.compat.IronsSpellBooks.entity.EchoCaster caster
                && caster.getSummoner() instanceof Player summoner) {
            player = summoner;
        }
        if (player == null || player.level().isClientSide) {
            return;
        }
        if (!wearingCharm(player)) {
            return;
        }
        projectile.setRicochetLevel(projectile.getRicochetLevel() + 1);
    }

    /** 是否佩戴带本词条的饰品 */
    private static boolean wearingCharm(Player player) {
        var curios = CuriosApi.getCuriosInventory(player).resolve();
        if (curios.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, ICurioStacksHandler> entry : curios.get().getCurios().entrySet()) {
            IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty() && ToolStack.isInitialized(stack)
                        && ToolStack.from(stack).getModifierLevel(CHARM_ID) > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}