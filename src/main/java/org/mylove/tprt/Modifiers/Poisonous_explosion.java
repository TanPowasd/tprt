package org.mylove.tprt.Modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.List;
import java.util.Objects;

import static net.minecraft.world.effect.MobEffects.POISON;

public class Poisonous_explosion extends NoLevelsModifier implements MeleeHitModifierHook {

    public static final int Poisonous_explosionCoolDown = 50;//tick

    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry entry, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target != null && target.hasEffect(Objects.requireNonNull(MobEffect.byId(MobEffect.getId(POISON)))) && attacker instanceof Player player) {
            target.invulnerableTime = 0;
            MobEffectInstance instance = target.getEffect(POISON);
//            List<MobEffectInstance> effectInstances = new ArrayList<>((Collection) POISON);
            if (instance != null) {
                ToolStack mainhand = Modifier.getHeldTool(player, EquipmentSlot.MAINHAND);
                if (mainhand != null) {
                    if (player.getCooldowns().isOnCooldown(mainhand.getItem())) return;
                    else {
                int x = instance.getAmplifier()+1;
                int y = instance.getDuration()/20;
                int X = (int) Math.sqrt(x * x * x);
                int Y = (int) Math.sqrt(y);
                if (context.getLivingTarget() != null) {
//                    for (MobEffectInstance effectlist : effectInstances){
//                        target.removeEffect(effectlist.getEffect());
                        target.removeEffect(POISON);
                    Level world = attacker.level();
                    AABB area = new AABB(target.getX() - 2, target.getY() - 1, target.getZ() - 2,
                            target.getX() + 2, target.getY() + 1, target.getZ() + 2);
                    List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(LivingEntity.class, area);
                    DamageSource damageSource = world.damageSources().magic();
                    float z = target.getHealth();
                    float damage = (float) (10 * X * Y + 0.03 * x * z);
                    for (LivingEntity entity : nearbyEntities) {
                            entity.hurt(damageSource, damage);
                    }
                    player.getCooldowns().addCooldown(mainhand.getItem(), Poisonous_explosionCoolDown);
                }
            }
        }
    }
}}}
