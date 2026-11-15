package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.entity.projectile.Amethyst_Cluster_Projectile_Entity;
import com.github.L_Ender.cataclysm.init.ModEntities;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.modules.technical.SlotInChargeModule;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.capability.TinkerDataCapability;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.UUID;
import java.util.function.BiConsumer;

public class Amethyst_of_the_Earth extends Modifier implements OnAttackedModifierHook, AttributesModifierHook {
    public static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge> SLOT_IN_CHARGE = TinkerDataCapability.TinkerDataKey.of(ModifierIds.amethyst_of_the_earth);

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
        hookBuilder.addHook(this, ModifierHooks.ATTRIBUTES);
        hookBuilder.addModule(new SlotInChargeModule(SLOT_IN_CHARGE));
    }

    @Override
    public void addAttributes(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentSlot equipmentSlot, BiConsumer<Attribute, AttributeModifier> biConsumer) {
        double amount = modifierEntry.getLevel() * 0.1;
        UUID slotUuid = ModifierLEVEL.UUIDFromWeapon(equipmentSlot, ModifierIds.amethyst_of_the_earth);
        biConsumer.accept(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(slotUuid, "Amethyst Of The Earth", amount, AttributeModifier.Operation.ADDITION));
    }

    @Override
    public void onAttacked(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float v, boolean b) {
        if (!SlotInChargeModule.isInCharge(context.getTinkerData(), SLOT_IN_CHARGE, equipmentSlot)) {
            return;
        }
        LivingEntity living = context.getEntity();
        if (living instanceof Player player && !player.level().isClientSide) {
            if (player.getCooldowns().isOnCooldown(iToolStackView.getItem())) {
                return;
            }
            int x = ModifierLEVEL.getTotalArmorModifierlevel(living, ModifierIds.amethyst_of_the_earth);
            var attackDamageAttr = living.getAttribute(Attributes.ATTACK_DAMAGE);
            int y = (int) (attackDamageAttr != null ? attackDamageAttr.getValue() : 1.0);
            var armorAttr = living.getAttribute(Attributes.ARMOR);
            int z = (int) (armorAttr != null ? armorAttr.getValue() : 0.0);
            int X = (int) Math.sqrt(y * y + z * z);
            for (int i = 0; i < 8; i++) {
                float throwAngle = i * Mth.PI / 4F;
                double sx = player.getX() + (Mth.cos(throwAngle) * 1);
                double sy = player.getY() + (player.getBbHeight() * 0.5D);
                double sz = player.getZ() + (Mth.sin(throwAngle) * 1);
                double vx = Mth.cos(throwAngle);
                double vy = 0 + player.getRandom().nextFloat() * 0.3F;
                double vz = Mth.sin(throwAngle);
                double v3 = Mth.sqrt((float) (vx * vx + vz * vz));
                Amethyst_Cluster_Projectile_Entity projectile = new Amethyst_Cluster_Projectile_Entity(ModEntities.AMETHYST_CLUSTER_PROJECTILE.get(), player.level(), player, (float) 11 + x * X);
                projectile.moveTo(sx, sy, sz, i * 11.25F, player.getXRot());
                float speed = 0.8F;
                projectile.shoot(vx, vy + v3 * 0.2D, vz, speed, 1.0F);
                player.level().addFreshEntity(projectile);
            }
            int cd = Math.max(20, 240 - 20 * x);
            for (ItemStack armor : player.getArmorSlots()) {
                if (!armor.isEmpty()) {
                    player.getCooldowns().addCooldown(armor.getItem(), cd);
                }
            }
        }
    }
}
