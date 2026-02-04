package org.mylove.tprt.Modifiers;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.List;
import java.util.Objects;

import static net.minecraft.world.effect.MobEffects.POISON;

public class Poisonous_explosion extends Modifier implements MeleeHitModifierHook {
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry entry, ToolAttackContext context, float damageDealt) {
        LivingEntity target = context.getLivingTarget();
        LivingEntity attacker = context.getAttacker();
        if (target != null && target.hasEffect(Objects.requireNonNull(MobEffect.byId(MobEffect.getId(POISON)))) && attacker instanceof Player) {
            target.invulnerableTime = 0;
            MobEffectInstance instance = target.getEffect(POISON);
//            List<MobEffectInstance> effectInstances = new ArrayList<>((Collection) POISON);
            if (instance != null) {
                int x = instance.getAmplifier()+1;
                int y = instance.getDuration()/20;
                int X = (int) Math.sqrt(x * x * x);
                int Y = y+100/(y+400);
                if (context.getLivingTarget() != null) {
//                    for (MobEffectInstance effectlist : effectInstances){
//                        target.removeEffect(effectlist.getEffect());
                        target.removeEffect(POISON);
                    Level world = attacker.level();
                    AABB area = new AABB(target.getX() - 2, target.getY() - 1, target.getZ() - 2,
                            target.getX() + 2, target.getY() + 1, target.getZ() + 2);
                    List<LivingEntity> nearbyEntities = world.getEntitiesOfClass(LivingEntity.class, area);
                    DamageSource damageSource = world.damageSources().magic();
                    float damage = 5 * X * Y;
                    for (LivingEntity entity : nearbyEntities) {
                            entity.hurt(damageSource, damage);
                    }
                }
            }
        }
    }
}
