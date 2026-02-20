package org.mylove.tprt.Modifiers;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class Tide_Guardian extends Modifier implements InventoryTickModifierHook {

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(IToolStackView iToolStackView, ModifierEntry modifier, Level world, LivingEntity entity, int in, boolean boo, boolean bo1, ItemStack itemStack) {
        if (!world.isClientSide() && entity instanceof ServerPlayer player&&player.tickCount %20 ==0 && !(entity instanceof FakePlayer)) {
            int X = ModifierLEVEL.getTotalArmorModifierlevel(entity,ModifierIds.Tide_Guardian);
            if(X > 0){
                player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40));
                if (player.isInWaterOrRain()){
                    if ( X >= 8 ){
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40,7));
                    }else {
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,40,X-1));
                    }
                }
            }
        }
    }
}
