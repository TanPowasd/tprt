package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.entity.effect.ScreenShake_Entity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.compat.Cataclysm.CataclysmCompat;
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
import java.util.UUID;

public class cursed_spirit extends NoLevelsModifier implements MeleeHitModifierHook {
    UUID uuid=UUID.fromString("0023BA86-65A2-4B81-AC18-57B995D1C3AB");

    public static final int cursed_spirit_cooldown = 80;//tick

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this,ModifierHooks.MELEE_HIT);
    }

    @Override
    public void afterMeleeHit(@NotNull IToolStackView tool, @NotNull ModifierEntry entry, ToolAttackContext context, float damageDealt) {
        if (!(context.getTarget() instanceof LivingEntity)) return;
        LivingEntity target =context.getLivingTarget();
        LivingEntity attacker=context.getAttacker();
        MobEffect effect = CataclysmCompat.Soul_casting_blade.get();
        MobEffectInstance instance = attacker.getEffect(effect);
        ToolStack mainhand = Modifier.getHeldTool(attacker, EquipmentSlot.MAINHAND);
        if (instance != null && attacker instanceof Player player) {
            double AE;
            int x = instance.getAmplifier();
            int X = x + 1;
            boolean A = instance.isAmbient();
            boolean B = instance.isVisible();
            boolean C = instance.showIcon();
            if (X > 2) {
                X = 2;
            }
            MobEffectInstance newEffect = new MobEffectInstance(
                    effect,
                    200,
                    X,
                    A,
                    B,
                    C);
            player.addEffect(newEffect);
            if (mainhand != null && x >= 2 && !player.getCooldowns().isOnCooldown(mainhand.getItem())) {
                if (target != null) {
                    target.invulnerableTime = 0;
                    AOE(player.level(),player);
                }
                if (target != null && !player.level().isClientSide) {
                    AE = (int) (cursed_spirit_cooldown - 0.5 * Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_SPEED)).getValue());
                    if(AE >= 40){
                        player.getCooldowns().addCooldown(tool.getItem(), (int) AE);
                    }else {
                        player.getCooldowns().addCooldown(tool.getItem(), 40);
                    }

                }
            }
        } else {
            attacker.addEffect(new MobEffectInstance(effect, 200));
        }
    }

    private void AOE(Level world,LivingEntity caster) {
        double radius = 6.0D;
        ScreenShake_Entity.ScreenShake(world, caster.position(), 30, 0.1f, 0, 30);
        world.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.5f, 1F / (caster.getRandom().nextFloat() * 0.4F + 0.8F));
        List<Entity> list = world.getEntities(caster, caster.getBoundingBox().inflate(radius, radius, radius));
        for (Entity entity : list) {
            if (entity instanceof LivingEntity) {
                entity.hurt(world.damageSources().mobAttack(caster), (float) caster.getAttributeValue(Attributes.ATTACK_DAMAGE) * 3F);
            }
        }
    }
}
