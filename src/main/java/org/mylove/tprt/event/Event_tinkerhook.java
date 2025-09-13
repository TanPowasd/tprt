package org.mylove.tprt.event;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.hooks.KillingHook;
import org.mylove.tprt.registries.ModHooks;
import org.mylove.tprt.utilities.TinkerToolUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Event_tinkerhook {

    @SubscribeEvent
    public static void onLivingKill(LivingDeathEvent event) {
        Entity entity = event.getSource().getEntity();
        if (entity instanceof LivingEntity attacker) {
            ToolStack tool = TinkerToolUtil.getToolInHand(attacker);
            if (!TinkerToolUtil.isNotBrokenOrNull(tool)) {
                return;
            }

            tool.getModifierList().forEach((e) -> {
                KillingHook hook = e.getHook(ModHooks.KILLING_HOOK);
                hook.onKillLivingTarget(tool, event, attacker, event.getEntity(), e.getLevel());
            });
        }

    }
}
