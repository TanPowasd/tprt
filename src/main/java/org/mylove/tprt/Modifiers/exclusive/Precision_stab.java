package org.mylove.tprt.Modifiers.exclusive;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 精准刺击: 使用对应工具造成伤害时, 伤害至少为玩家攻击力的 50%, 且这个下限作用在**最终伤害**上。
 * <p>
 * 分两步保证:
 * <ol>
 *   <li>伤害修正 hook (MELEE_DAMAGE): 先保证进入结算的伤害不低于下限;</li>
 *   <li>最终结算监听 (LivingDamageEvent): 护甲、抗性、保护附魔等减伤算完之后,
 *       如果最终值被削到下限以下, 再把差额补回来 —— Forge 明确定义此刻的 amount 就是 FINAL value,
 *       所以这一步是越过护甲值与伤害减免生效的。</li>
 * </ol>
 * 两步都只认"对应工具 (配方限定的 sakuratinker:swift_sword) 打出的那次近战命中"。
 */
public class Precision_stab extends NoLevelsModifier implements MeleeDamageModifierHook {

    /** 伤害下限: 玩家攻击力的 50% */
    public static final float MIN_DAMAGE_RATIO = 0.1F;
    /** 该词条对应的工具 (与配方 ability/precision_stab.json 的 tools 一致) */
    public static final ResourceLocation SWIFT_SWORD = ResourceLocation.fromNamespaceAndPath("sakuratinker", "swift_sword");
    /** 记录的有效期(毫秒): 命中与最终结算本就在同一次调用里, 只是防止异常情况下残留 */
    private static final long RECORD_LIFETIME = 1000L;
    /** 记录表超过这个条数就先清理过期项 */
    private static final int CLEANUP_THRESHOLD = 32;

    /** 刚发生的近战命中: 目标UUID -> 该目标这次要吃到的最低最终伤害 */
    private static final Map<UUID, PendingFloor> PENDING_FLOORS = new HashMap<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingDamageEvent.class, Precision_stab::onLivingDamage);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        // 只会在对应工具上生效 (别人用命令/数据包硬加这个词条也不会有效果)
        if (!isCorrespondingTool(context)) {
            return damage;
        }
        Player player = context.getPlayerAttacker();
        if (player == null) {
            return damage;
        }
        // 玩家攻击力: 主手攻击时就是玩家的攻击力属性值 (TC 工具会把自己的攻击力作为属性修正加进去)
        float attackPower = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (attackPower <= 0) {
            return damage;
        }
        float floor = attackPower * MIN_DAMAGE_RATIO;
        // 记下这次命中, 等最终伤害结算时再兜底 (护甲/减伤吃掉的部分要补回来)
        LivingEntity target = context.getLivingTarget();
        if (target != null && !player.level().isClientSide) {
            remember(target.getUUID(), player.getUUID(), floor);
        }
        return Math.max(damage, floor);
    }

    /**
     * 最终伤害结算前触发: 此时护甲值、药水(抗性)、保护附魔、伤害吸收都已经结算完,
     * 直接把最终值抬到下限, 从而绕过这些减免。
     */
    private static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity target = event.getEntity();
        PendingFloor record = PENDING_FLOORS.get(target.getUUID());
        if (record == null || record.isExpired()) {
            return;
        }
        // 只认刚才那次近战造成的伤害: 别的来源 (比如法术) 不动它, 等记录自己过期
        if (!(event.getSource().getEntity() instanceof Player player) || !player.getUUID().equals(record.player())) {
            return;
        }
        PENDING_FLOORS.remove(target.getUUID());
        event.setAmount(Math.max(event.getAmount(), record.floor()));
    }

    private static void remember(UUID target, UUID player, float floor) {
        if (PENDING_FLOORS.size() >= CLEANUP_THRESHOLD) {
            PENDING_FLOORS.values().removeIf(PendingFloor::isExpired);
        }
        PENDING_FLOORS.put(target, new PendingFloor(player, floor, System.currentTimeMillis()));
    }

    /** 这次攻击拿的是不是对应的那把工具 */
    private static boolean isCorrespondingTool(ToolAttackContext context) {
        Item swiftSword = ForgeRegistries.ITEMS.getValue(SWIFT_SWORD);
        if (swiftSword == null) {
            return false;
        }
        ItemStack held = context.getAttacker().getItemInHand(context.getHand());
        return held.is(swiftSword);
    }

    /** 一次待结算的伤害下限 */
    private record PendingFloor(UUID player, float floor, long time) {
        boolean isExpired() {
            return System.currentTimeMillis() - time > RECORD_LIFETIME;
        }
    }
}
