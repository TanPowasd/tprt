package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.entity.projectile.Sandstorm_Projectile;
import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

import java.util.Objects;


public class Ancient_Spear extends BaseModifier{

    public boolean isNoLevels() {
        return true;
    }

    @Override
    public void onLeftClickEmpty(IToolStackView tool, ModifierEntry entry, Player player, Level level, EquipmentSlot equipmentSlot) {
        ToolStack stack = Modifier.getHeldTool(player, EquipmentSlot.MAINHAND);
        if (stack != null) {
            ItemStack stack1 = stack.createStack();
            if(isCharged(player, stack1)) {
                launchTornado(stack.createStack(), player);
            }
        }
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifierEntry, ToolAttackContext context, float damageDealt) {
        LivingEntity entity = context.getAttacker();
        ToolStack stack = Modifier.getHeldTool(entity, EquipmentSlot.MAINHAND);
        if(entity instanceof Player player) {
            if (stack != null) {
                ItemStack stack1 = stack.createStack();
                if(isCharged(player, stack1)) {
                    launchTornado(stack.createStack(), entity);
                }
            }
        }
    }

    private boolean isCharged(Player player, ItemStack stack){
        return player.getAttackStrengthScale(0.5F) > 0.9F;
    }

    public void launchTornado(ItemStack XY, LivingEntity player) {
        ToolStack y = Modifier.getHeldTool(player, EquipmentSlot.MAINHAND);
        ItemStack stack = null;
        if (y != null) {
            stack = y.createStack();
        }
        Level worldIn = player.level();
        if (!worldIn.isClientSide) {
            if (stack != null) {
                stack.hurtAndBreak(1, player, (entity) -> {
                    entity.broadcastBreakEvent(player.getUsedItemHand());
                });
            }
            float d7 = player.getYRot();
            float d = player.getXRot();
            float d1 = -Mth.sin(d7 * ((float) Math.PI / 180F)) * Mth.cos(d * ((float) Math.PI / 180F));
            float d2 = -Mth.sin(d * ((float) Math.PI / 180F));
            float d3 = Mth.cos(d7 * ((float) Math.PI / 180F)) * Mth.cos(d * ((float) Math.PI / 180F));
            double theta = d7 * (Math.PI / 180);
            theta += Math.PI / 2;
            double vecX = Math.cos(theta);
            double vecZ = Math.sin(theta);
            double x = player.getX() + vecX;
            double Z = player.getZ() + vecZ;
            float X = (float) Objects.requireNonNull(player.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
            Sandstorm_Projectile largefireball = new Sandstorm_Projectile(player, d1, d2, d3, player.level(), (float) (6 + 1.5 * X ));
            largefireball.setState(1);
            largefireball.setPos(x, player.getEyeY() - 0.5D, Z);
            worldIn.addFreshEntity(largefireball);
        }
    }
}
