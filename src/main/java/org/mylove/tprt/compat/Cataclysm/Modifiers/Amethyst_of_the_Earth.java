package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.config.CMConfig;
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
import org.mylove.tprt.registries.ModifierIds;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.OnAttackedModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

public class Amethyst_of_the_Earth extends Modifier implements OnAttackedModifierHook, AttributesModifierHook {
    UUID uuid = UUID.fromString("46e02bff-5442-4e20-bb63-a1667a3023e7");

    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
        hookBuilder.addHook(this, ModifierHooks.ATTRIBUTES);
    }

    @Override
    public void addAttributes(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentSlot equipmentSlot, BiConsumer<Attribute, AttributeModifier> biConsumer) {
        int x = (int) (modifierEntry.getLevel() * 0.1);
        biConsumer.accept(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uuid, "Amethyst Of The Earth", x, AttributeModifier.Operation.ADDITION));
    }

    @Override
    public void onAttacked(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentContext context, EquipmentSlot equipmentSlot, DamageSource damageSource, float v, boolean b) {
        LivingEntity living = context.getEntity();
        int x = ModifierLEVEL.getTotalArmorModifierlevel(living, ModifierIds.amethyst_of_the_earth);
        int y = (int) Objects.requireNonNull(living.getAttribute(Attributes.ATTACK_DAMAGE)).getValue();
        int z = (int) Objects.requireNonNull(living.getAttribute(Attributes.ARMOR)).getValue();
        int X = (int) Math.sqrt(y*y+z*z);
        if (living instanceof Player player){
            if (!player.getCooldowns().isOnCooldown(iToolStackView.getItem())) {
                for (int i = 0; i < 8; i++) {
                    float throwAngle = i * Mth.PI / 4F;
                    double sx = player.getX() + (Mth.cos(throwAngle) * 1);
                    double sy = player.getY() + (player.getBbHeight() * 0.5D);
                    double sz = player.getZ() + (Mth.sin(throwAngle) * 1);
                    double vx = Mth.cos(throwAngle);
                    double vy = 0 + player.getRandom().nextFloat() * 0.3F;
                    double vz = Mth.sin(throwAngle);
                    double v3 = Mth.sqrt((float) (vx * vx + vz * vz));
                    Amethyst_Cluster_Projectile_Entity projectile = new Amethyst_Cluster_Projectile_Entity(ModEntities.AMETHYST_CLUSTER_PROJECTILE.get(), player.level(), player, (float) CMConfig.AmethystClusterdamage + x * X);
                    projectile.moveTo(sx, sy, sz, i * 11.25F, player.getXRot());
                    float speed = 0.8F;
                    projectile.shoot(vx, vy + v3 * 0.2D, vz, speed, 1.0F);
                    player.level().addFreshEntity(projectile);
                }
                player.getCooldowns().addCooldown(iToolStackView.getItem(), 240 - 20 * x);
            }
        }
    }
}
