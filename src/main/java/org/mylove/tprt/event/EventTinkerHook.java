package org.mylove.tprt.event;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.hooks.DamageRedirectHook;
import org.mylove.tprt.hooks.KillingHook;
import org.mylove.tprt.hooks.TprtHooks;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.HashSet;

@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventTinkerHook {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void triggerKillingHook(LivingDeathEvent event) {
        //if (event.isCanceled()) return;
        // bug: 死亡钩子在有伤害源重定向时会触发两次

        Entity entity = event.getSource().getEntity();
        if (entity instanceof LivingEntity attacker) {
            HashSet<ToolStack> s = new HashSet<>();
            ToolStack tinkerTool1 = Modifier.getHeldTool(attacker, InteractionHand.MAIN_HAND);
            ToolStack tinkerTool2 = Modifier.getHeldTool(attacker, InteractionHand.OFF_HAND);
            if (tinkerTool1 != null) s.add(tinkerTool1);
            if (tinkerTool2 != null) s.add(tinkerTool2);

            s.forEach(t -> {
                t.getModifierList().forEach((entry) -> {
                    KillingHook hook = entry.getHook(TprtHooks.KILLING_HOOK);
                    hook.onKillLivingTarget(t, event, attacker, event.getEntity(), entry.getLevel());
                });
            });

        }

    }

    /// {@link LivingEntity#actuallyHurt(DamageSource, float)}
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void triggerDamageRedirectHook(LivingHurtEvent event) {
        if (!event.getSource().is(DamageTypes.PLAYER_ATTACK)) return;
        DamageSource oldSource = event.getSource();
        Entity entity = oldSource.getEntity();
        if (entity instanceof Player attacker) {
            LivingEntity target = event.getEntity();
            ToolStack tinkerTool = Modifier.getHeldTool(attacker, InteractionHand.MAIN_HAND);
            if (tinkerTool != null) {
                DamageSource newSource;

                for (ModifierEntry entry : tinkerTool.getModifierList()) {
                    DamageRedirectHook hook = entry.getHook(TprtHooks.DAMAGE_REDIRECT_HOOK);
                    newSource = hook.redirectDamageSource(tinkerTool, attacker, target, entry.getLevel(), oldSource);
                    // 只有优先级最高的钩子会生效
                    if (newSource != null && !newSource.is(DamageTypes.PLAYER_ATTACK)) {
                        System.out.println("RedirectDamageSource: " + oldSource + " --> " + newSource + "( " + event.getAmount());
                        // 重定向伤害
                        event.setCanceled(true);
                        target.invulnerableTime = 0;
                        target.hurt(newSource, event.getAmount());
                        break;
                    }
                }
            }
        }
    }
}
