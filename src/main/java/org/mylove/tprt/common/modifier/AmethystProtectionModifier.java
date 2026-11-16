package org.mylove.tprt.common.modifier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mylove.tprt.Tprt;
import slimeknights.tconstruct.library.tools.capability.EntityModifierCapability;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

/** Registered, including its projectile bridge, only by the Cataclysm integration. */
public class AmethystProtectionModifier extends NoLevelsModifier
        implements InventoryTickModifierHook, MeleeDamageModifierHook {
    private static final slimeknights.tconstruct.library.modifiers.ModifierId ID =
            new slimeknights.tconstruct.library.modifiers.ModifierId(Tprt.MODID, "amethyst_protection");
    private static final ResourceLocation BLESSING =
            new ResourceLocation("cataclysm", "blessing_of_amethyst");

    public AmethystProtectionModifier() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(AmethystProtectionModifier::onProjectileDamage);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder builder) {
        builder.addHook(this, ModifierHooks.INVENTORY_TICK, ModifierHooks.MELEE_DAMAGE);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level,
                                LivingEntity holder, int slot, boolean selected,
                                boolean isEquipped, ItemStack stack) {
        if (level.isClientSide || tool.isBroken() || holder.tickCount % 20 != 0) return;
        // Armor's native armor tick supplies isEquipped; carried tools must be in a hand.
        if (!isEquipped && holder.getMainHandItem() != stack && holder.getOffhandItem() != stack) return;
        MobEffect blessing = ForgeRegistries.MOB_EFFECTS.getValue(BLESSING);
        if (blessing != null) {
            holder.addEffect(new MobEffectInstance(blessing, 40, 0, false, false, true));
        }
    }

    private static boolean isProtectedTarget(LivingEntity target) {
        return target != null && (target.getMobType() == MobType.UNDEAD || target.getMobType() == MobType.WATER);
    }

    /**
     * This TC version has no projectile damage-transform hook. Use its launch-time modifier
     * snapshot, not the shooter's current equipment, and never mutate a piercing arrow's base damage.
     */
    @SubscribeEvent
    public static void onProjectileDamage(LivingHurtEvent event) {
        if (!event.getEntity().level().isClientSide
                && event.getSource().getDirectEntity() instanceof Projectile projectile
                && EntityModifierCapability.getOrEmpty(projectile).getLevel(ID) > 0
                && isProtectedTarget(event.getEntity())) {
            event.setAmount(event.getAmount() * 1.25F);
        }
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier,
                                ToolAttackContext context, float baseDamage, float damage) {
        return isProtectedTarget(context.getLivingTarget()) ? damage * 1.25F : damage;
    }
}
