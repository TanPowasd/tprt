package org.mylove.tprt.compat.Cataclysm.Modifiers;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 海之珠泪: 举盾后 0.2 秒内成功格挡攻击 (完美格挡) 时,
 * 击退攻击者约 2 格, 分别给予目标 10 秒缓慢 III、自身 10 秒力量 III,
 * 并额外获得最大生命值 10% 的临时生命值 (伤害吸收)。
 * <p>
 * 触发条件与匠魂自己的格挡处理一致: Forge 的 {@link ShieldBlockEvent}
 * + 正在使用的物品是本词条所在的工具 (匠魂工具才有的格挡)。
 */
public class tears_of_the_pearl_of_the_sea extends NoLevelsModifier {

    /** 完美格挡窗口: 0.2 秒 = 4 tick */
    public static final int PERFECT_BLOCK_TICKS = 4;
    /** 效果时长: 10 秒 = 200 tick */
    public static final int EFFECT_DURATION = 200;
    /** III 级 -> amplifier 2 */
    public static final int EFFECT_AMPLIFIER = 2;
    /** 击退强度: 1.0 约等于 2 格位移 (位移约为强度的 2.2 倍, 受摩擦影响) */
    public static final double KNOCKBACK_STRENGTH = 1.0D;
    /** 完美格挡额外获得的临时生命值: 最大生命值的 10% */
    public static final float TEMPORARY_HEALTH_RATIO = 0.1F;

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ShieldBlockEvent.class, tears_of_the_pearl_of_the_sea::onShieldBlock);
    }

    /** 成功格挡时触发 (匠魂的 InteractionHandler 用的也是这个事件) */
    private static void onShieldBlock(ShieldBlockEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        ItemStack shield = player.getUseItem();
        // 挡下攻击的必须是本词条所在的工具 (且没损坏)
        if (shield.isEmpty() || !shield.is(TinkerTags.Items.MODIFIABLE)) {
            return;
        }
        ToolStack tool = ToolStack.from(shield);
        if (tool.isBroken() || tool.getModifierLevel(ModifierIds.TEARS_OF_THE_PEARL_OF_THE_SEA) <= 0) {
            return;
        }
        // 完美格挡判定: 举盾 0.2 秒内挡下
        if (getUseTicks(player) > PERFECT_BLOCK_TICKS) {
            return;
        }
        // 目标就是打过来的那只
        LivingEntity attacker = null;
        if (event.getDamageSource().getDirectEntity() instanceof LivingEntity direct) {
            attacker = direct;
        } else if (event.getDamageSource().getEntity() instanceof LivingEntity causing) {
            attacker = causing;
        }
        if (attacker != null && attacker != player) {
            knockAway(player, attacker);
            attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION, EFFECT_AMPLIFIER));
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, EFFECT_DURATION, EFFECT_AMPLIFIER));
        // 完美格挡额外获得最大生命值 10% 的临时生命值 (伤害吸收, 会先于血量被消耗掉)
        float temporaryHealth = player.getMaxHealth() * TEMPORARY_HEALTH_RATIO;
        if (temporaryHealth > 0.0F) {
            player.setAbsorptionAmount(player.getAbsorptionAmount() + temporaryHealth);
        }
    }

    /** 举盾已经持续了多少 tick */
    private static int getUseTicks(Player player) {
        ItemStack shield = player.getUseItem();
        return Math.max(shield.getUseDuration() - player.getUseItemRemainingTicks(), 0);
    }

    /** 把目标沿背离玩家的方向击退 (约 2 格) */
    private static void knockAway(Player player, LivingEntity target) {
        Vec3 offset = target.position().subtract(player.position());
        Vec3 flat = new Vec3(offset.x, 0.0D, offset.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            return;
        }
        flat = flat.normalize();
        // knockback 内部是 delta - vec, 所以传反方向才是"推离玩家"
        target.knockback(KNOCKBACK_STRENGTH, -flat.x, -flat.z);
    }
}
