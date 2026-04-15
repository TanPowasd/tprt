package org.mylove.tprt.Modifiers.armor;

import com.ssakura49.sakuratinker.common.tinkering.modifiers.special.PolishModifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.ModifyDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.tools.modules.armor.CounterModule;

import java.util.UUID;

public class Glow_armor extends PolishModifier implements ToolStatsModifierHook, InventoryTickModifierHook, OnAttackedModifierHook, ModifyDamageModifierHook {

    UUID uuid = UUID.fromString("1875d098-ab0d-4244-9bcf-d89d0a08048d");

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
        hookBuilder.addHook(this, ModifierHooks.MODIFY_DAMAGE);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        POLISH_STAT.add(builder, 50*modifier.getLevel());
    }

    @Override
    public void onInventoryTick(IToolStackView iToolStackView, ModifierEntry modifierEntry, Level world, LivingEntity livingEntity, int i, boolean b, boolean b1, ItemStack itemStack) {
        if (world.isClientSide || !(livingEntity instanceof Player player)) return;
        if (player.tickCount % 20 != 0) return;
        int X = ModifierLEVEL.getTotalArmorModifierlevel(player, ModifierIds.Glow_armor);
        if(X >= 1){
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100));
            this.addPolish(iToolStackView, modifierEntry, 5 * modifierEntry.getLevel());
        }
    }

    @Override
    public void onAttacked(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float v, boolean b) {
        int x = getPolish(iToolStackView);
        if (x > 100){
            this.addPolish(iToolStackView, modifierEntry, -20);
            Entity attacker = damageSource.getEntity();
            LivingEntity defender = context.getEntity();
            if (b && attacker != null && attacker != defender && defender instanceof Player player) {
                float y = CounterModule.getLevel(iToolStackView, modifierEntry, equipmentSlot, defender);
                float z = player.getArmorValue();
                attacker.hurt(defender.damageSources().thorns(defender), (float) (z * (0.5 + 0.25f*y)));
            }
        }
    }

    @Override
    public float modifyDamageTaken(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float amount, boolean b) {
        LivingEntity entity =context.getEntity();
        float x = 1f;
        if (entity instanceof Player player){
            if (player.hasEffect(MobEffects.GLOWING)){
                int y = getPolish(iToolStackView);
                if (y >= 100){
                    x = 0.25f;
                }else {
                    x = 0.75f;
                }
            }
        }
        return amount * x ;
    }
}
