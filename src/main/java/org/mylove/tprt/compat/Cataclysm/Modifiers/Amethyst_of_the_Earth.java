package org.mylove.tprt.compat.Cataclysm.Modifiers;

import com.github.L_Ender.cataclysm.entity.projectile.Amethyst_Cluster_Projectile_Entity;
import com.github.L_Ender.cataclysm.init.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import java.util.Map;
import java.util.WeakHashMap;
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

public class Amethyst_of_the_Earth extends Modifier implements OnAttackedModifierHook, AttributesModifierHook,
        InventoryTickModifierHook, EquipmentChangeModifierHook {
    private static final ModifierId PROTECTION = new ModifierId("tprt", "amethyst_protection");
    private static final ResourceLocation NATURE_HEAL = new ResourceLocation("cataclysm", "nature_heal");
    private static final UUID SYNERGY_UUID = UUID.fromString("6db8a979-a3c2-4a99-a7cd-a6028ed23ec1");
    // Weak wearer keys, scalar values only: no equipment context or entity lifetime leaks.
    private final Map<LivingEntity, Integer> lastHealTick = new WeakHashMap<>();

    /** Read actual equipment, optionally substituting an in-flight equipment change. */
    private static boolean hasSynergy(LivingEntity wearer, EquipmentChangeContext change) {
        boolean earth = false;
        boolean protection = false;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = change != null && change.getChangedSlot() == slot
                    ? change.getReplacement() : wearer.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.is(TinkerTags.Items.MODIFIABLE)) continue;
            // Held armor is not worn armor; armor in an incorrect slot is not valid either.
            boolean worn = slot.isArmor() && stack.is(TinkerTags.Items.ARMOR)
                    && stack.canEquip(slot, wearer);
            boolean held = !slot.isArmor() && stack.is(TinkerTags.Items.HELD);
            if (!worn && !held) continue;
            IToolStackView equipped = ToolStack.from(stack);
            if (equipped.isBroken()) continue;
            if (worn && equipped.getModifierLevel(ModifierIds.amethyst_of_the_earth) > 0) earth = true;
            if (equipped.getModifierLevel(PROTECTION) > 0) protection = true;
        }
        return earth && protection;
    }

    private boolean updateSynergy(LivingEntity wearer, EquipmentChangeContext change) {
        boolean active = wearer.isAlive() && hasSynergy(wearer, change);
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(NATURE_HEAL);
        var instance = attribute == null ? null : wearer.getAttribute(attribute);
        if (instance != null) {
            if (active) {
                if (instance.getModifier(SYNERGY_UUID) == null) {
                    // Cataclysm ModAttribute.NATURE_HEAL is percentage points, base 0.
                    // FoodDataMixin scales natural healing by max(0, 1 + value / 100).
                    instance.addTransientModifier(new AttributeModifier(SYNERGY_UUID,
                            "Amethyst earth synergy", 50, AttributeModifier.Operation.ADDITION));
                }
            } else {
                instance.removeModifier(SYNERGY_UUID);
            }
        }
        if (!active) lastHealTick.remove(wearer);
        return active;
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level,
                                LivingEntity holder, int slot, boolean selected,
                                boolean isEquipped, ItemStack stack) {
        if (level.isClientSide) return;
        // Deliberately do not return on broken tools: the last broken armor must clean up.
        if (!updateSynergy(holder, null) || holder.tickCount % 20 != 0) return;
        Integer previous = lastHealTick.put(holder, holder.tickCount);
        if (previous == null || previous != holder.tickCount) {
            holder.heal(holder.getMaxHealth() * 0.01F);
        }
    }

    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (!context.getLevel().isClientSide) updateSynergy(context.getEntity(), context);
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
        if (!context.getLevel().isClientSide) updateSynergy(context.getEntity(), context);
    }

    @Override
    public void onEquipmentChange(IToolStackView tool, ModifierEntry modifier,
                                  EquipmentChangeContext context, EquipmentSlot slot) {
        if (!context.getLevel().isClientSide) updateSynergy(context.getEntity(), context);
    }
    public static final TinkerDataCapability.TinkerDataKey<SlotInChargeModule.SlotInCharge> SLOT_IN_CHARGE = TinkerDataCapability.TinkerDataKey.of(ModifierIds.amethyst_of_the_earth);

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.ON_ATTACKED);
        hookBuilder.addHook(this, ModifierHooks.ATTRIBUTES);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK, ModifierHooks.EQUIPMENT_CHANGE);
        hookBuilder.addModule(new SlotInChargeModule(SLOT_IN_CHARGE));
    }

    @Override
    public void addAttributes(IToolStackView iToolStackView, ModifierEntry modifierEntry, EquipmentSlot equipmentSlot, BiConsumer<Attribute, AttributeModifier> biConsumer) {
        double amount = modifierEntry.getLevel() * 0.1;
        UUID slotUuid = ModifierLEVEL.UUIDFromWeapon(equipmentSlot, ModifierIds.amethyst_of_the_earth);
        biConsumer.accept(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(slotUuid, "Amethyst Of The Earth", amount, AttributeModifier.Operation.ADDITION));
        // Conditional nature healing is applied only by updateSynergy after checking the wearer.
        // ATTRIBUTES has no wearer context and must not advertise it as an unconditional modifier.
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
