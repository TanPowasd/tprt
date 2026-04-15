package org.mylove.tprt.Modifiers.melee;

import com.ssakura49.sakuratinker.common.tinkering.modifiers.special.PolishModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;

import java.util.UUID;

public class Shine_at_night extends PolishModifier implements ToolStatsModifierHook,InventoryTickModifierHook, MeleeDamageModifierHook, MeleeHitModifierHook {

    UUID uuid = UUID.fromString("9709563e-f02b-47e2-8c05-9dfc14689e46");

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
        hookBuilder.addHook(this, ModifierHooks.TOOL_STATS);
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        POLISH_STAT.add(builder, 25*modifier.getLevel());
    }

    @Override
    public float getMeleeDamage(IToolStackView iToolStackView, ModifierEntry modifierEntry, ToolAttackContext context, float baseDamage, float damage) {
        LivingEntity entity=context.getLivingTarget();
        LivingEntity attacker=context.getAttacker();
        double x = 1f;
        if (entity != null && attacker instanceof Player){
            if (iToolStackView.getStats().get(POLISH_STAT) > 100){
                x = 1 + (0.25 * (2 + modifierEntry.getLevel()));
            }
        }
        return (float) (baseDamage * x);
    }

    @Override
    public void onInventoryTick(IToolStackView iToolStackView, ModifierEntry modifierEntry, Level world, LivingEntity livingEntity, int i, boolean b, boolean b1, ItemStack itemStack) {
        if (world.isClientSide || !(livingEntity instanceof Player player)) return;
        if (player.tickCount % 20 != 0) return;
        int x = getPolish(iToolStackView);
        if (x > 100){
            this.addPolish(iToolStackView, modifierEntry, -25);
        }
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity entity=context.getLivingTarget();
        LivingEntity attacker=context.getAttacker();
        if (entity != null && attacker instanceof Player){
            this.addPolish(tool, modifier, 5 + 5 * modifier.getLevel());
        }
    }
}
