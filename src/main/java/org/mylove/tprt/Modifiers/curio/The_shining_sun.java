package org.mylove.tprt.Modifiers.curio;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class The_shining_sun extends CuriosModifier {
    public The_shining_sun(){}

    public boolean isNoLevels() {
        return true;
    }

    public static int getLight(Level world, BlockPos pos) {
        return Math.max(world.getBrightness(LightLayer.SKY, pos) - world.getSkyDarken(), world.getBrightness(LightLayer.BLOCK, pos));
    }

    public void onDamageTargetPre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event, LivingEntity attacker, LivingEntity target) {
        if (target != null) {
            if (!(attacker instanceof Player)) {
                return;
            }
            Level world = attacker.getCommandSenderWorld();
            BlockPos pos = attacker.getOnPos().above();
            int light = getLight(world, pos) + 1;
            float x = 1.15F;
            if(light >= 11){
                x = 1.15f + (light - 11) * 0.1f;
            }
            float damage = event.getAmount();
            float newDamage = damage * x;
            event.setAmount(newDamage);
        }
    }
}
