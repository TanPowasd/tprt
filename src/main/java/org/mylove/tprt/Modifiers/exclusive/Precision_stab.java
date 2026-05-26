package org.mylove.tprt.Modifiers.exclusive;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Objects;

public class Precision_stab extends NoLevelsModifier implements MeleeHitModifierHook {

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity entity = context.getLivingTarget();
        if (entity != null && attacker instanceof Player player){
            double x = Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
            entity.invulnerableTime = 0 ;
            entity.hurt(entity.damageSources().outOfBorder(), (float) x);
            entity.setLastHurtByMob(attacker);
        }
    }

    /*public Precision_stab(){
        MinecraftForge.EVENT_BUS.addListener(this::onLivingDamage);
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        DamageSource source = event.getSource();
        float x = event.getAmount();
        if (source.getEntity() instanceof Player player) {
            ItemStack item = player.getMainHandItem();
            int y = (int) (player.getAttributeValue(Attributes.ATTACK_DAMAGE));
            if (this.getLevel(item) > 0 && x < y) {
                if (y > 100){
                    y = 100;
                }
                event.setAmount(y);
            }
        }
    }

    private int getLevel(ItemStack stack) {
        if (stack.getItem() instanceof IModifiable) {
            IToolStackView tool = ToolStack.from(stack);
            return tool.getModifierLevel(this);
        }
        return 0;
    }*/
}
