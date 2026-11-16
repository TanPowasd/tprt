package org.mylove.tprt.Modifiers.melee;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/**
 * 自燃：命中时点燃自身 5 秒；燃烧期间每秒对周围 4 格内的生物造成等同自身攻击力的火焰伤害。
 */
public class self_ignition extends NoLevelsModifier implements MeleeHitModifierHook, InventoryTickModifierHook {
    private static final int FIRE_SECONDS = 5;
    private static final double RADIUS = 4.0D;

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void afterMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                              @NotNull ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        // TC already confirmed a successful hit; damageDealt is only the health delta.
        // Absorption and damage conversion can make that delta zero without failing the hit.
        if (attacker.level().isClientSide || context.getLivingTarget() == null || tool.isBroken()) {
            return;
        }
        attacker.setSecondsOnFire(FIRE_SECONDS);
    }

    private static ItemStack selectedAuraStack(LivingEntity holder) {
        // Only one aura per wearer: main hand wins if it has an unbroken self-ignition tool.
        ItemStack main = holder.getMainHandItem();
        if (main.is(slimeknights.tconstruct.common.TinkerTags.Items.MODIFIABLE)) {
            var tool = slimeknights.tconstruct.library.tools.nbt.ToolStack.from(main);
            if (!tool.isBroken() && tool.getModifierLevel(
                    new slimeknights.tconstruct.library.modifiers.ModifierId("tprt", "self_ignition")) > 0) return main;
        }
        return holder.getOffhandItem();
    }

    @Override
    public void onInventoryTick(@NotNull IToolStackView tool, @NotNull ModifierEntry modifier,
                                @NotNull Level world, @NotNull LivingEntity holder, int itemSlot,
                                boolean isSelected, boolean isEquipped, @NotNull ItemStack stack) {
        if (world.isClientSide || !isEquipped || tool.isBroken() || !holder.isOnFire()
                || (holder.getMainHandItem() != stack && holder.getOffhandItem() != stack)
                || selectedAuraStack(holder) != stack) {
            return;
        }
        long now = world.getGameTime();
        var data = holder.getPersistentData();
        String tickKey = "tprt:self_ignition_tick";
        if (data.contains(tickKey) && data.getLong(tickKey) == now) return;
        data.putLong(tickKey, now);
        if (holder.tickCount % 10 == 0) org.mylove.tprt.common.BlazeforgedFireEvents.circle(holder);
        if (holder.tickCount % 20 != 0) return;
        float damage = (float) holder.getAttributeValue(Attributes.ATTACK_DAMAGE)
                * (1.0F + BlazeforgedTraitData.damageBonus(tool, world.getGameTime()));
        if (damage <= 0) {
            return;
        }
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class,
                holder.getBoundingBox().inflate(RADIUS), entity -> entity.distanceToSqr(holder) <= RADIUS * RADIUS
                        && org.mylove.tprt.common.BlazeforgedFireEvents.enemy(holder, entity))) {
            org.mylove.tprt.common.BlazeforgedFireEvents.pulse(holder, target, damage);
        }
    }
}
