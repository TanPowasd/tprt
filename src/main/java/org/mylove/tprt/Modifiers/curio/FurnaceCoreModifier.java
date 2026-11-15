package org.mylove.tprt.Modifiers.curio;

import com.ssakura49.tinkercuriolib.hook.TCLibHooks;
import com.ssakura49.tinkercuriolib.hook.armor.CurioEquipmentChangeModifierHook;
import com.ssakura49.tinkercuriolib.hook.behavior.CurioAttributeModifierHook;
import com.ssakura49.tinkercuriolib.hook.combat.CurioDamageTargetPreModifierHook;
import com.ssakura49.tinkercuriolib.hook.interation.CurioInventoryTickModifierHook;
import com.ssakura49.tinkercuriolib.hook.ranged.CurioProjectileLaunchModifierHook;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;

/** Burning-only charm trait. No global subscribers or CuriosModifier constructor listeners. */
public final class FurnaceCoreModifier extends NoLevelsModifier implements CurioAttributeModifierHook,
        CurioInventoryTickModifierHook, CurioEquipmentChangeModifierHook,
        CurioProjectileLaunchModifierHook, CurioDamageTargetPreModifierHook {
    public static final ModifierId ID = new ModifierId("tprt", "furnace_core");
    private static final TagKey<Item> CHARM = TagKey.create(Registries.ITEM, new ResourceLocation("curios", "tinker_charm"));
    private static final ResourceLocation LAUNCH_SNAPSHOT = new ResourceLocation("tprt", "furnace_core_launch");
    private static final UUID ATTACK_SPEED_ID = UUID.fromString("6343ab64-c9a9-4e2c-87ca-73767b83c1ba");
    private static final UUID MOVEMENT_SPEED_ID = UUID.fromString("db4e868f-de6f-4551-bd4e-d063fdbce217");
    private static final AttributeModifier ATTACK_SPEED = new AttributeModifier(ATTACK_SPEED_ID,
            "tprt.furnace_core.attack_speed", 0.20D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final AttributeModifier MOVEMENT_SPEED = new AttributeModifier(MOVEMENT_SPEED_ID,
            "tprt.furnace_core.movement_speed", 0.15D, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private final Map<LivingEntity, BurnState> burning = new WeakHashMap<>();
    private final Map<LivingHurtEvent, Boolean> boostedDamage = new WeakHashMap<>();

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, TCLibHooks.CURIO_ATTRIBUTE, TCLibHooks.CURIO_TICK,
                TCLibHooks.CURIO_EQUIPMENT_CHANGE, TCLibHooks.CURIO_PROJECTILE_LAUNCH,
                TCLibHooks.CURIO_DAMAGE_TARGET_PRE);
    }

    private static boolean validInSlot(ItemStack stack, String slot) {
        if (!slot.equals("tinker_charm") && !slot.equals("curio")) return false;
        if (stack.isEmpty() || !stack.is(CHARM) || !ToolStack.isInitialized(stack)
                || !stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("curios", slot)))) return false;
        ToolStack tool = ToolStack.from(stack);
        return !tool.isBroken() && tool.getModifierLevel(ID) > 0;
    }

    /** Only functional Curios stacks count, never cosmetic, held or inventory stacks. */
    private static boolean wearing(LivingEntity entity, SlotContext excluded) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("sakuratinker")) return false;
        var inventory = CuriosApi.getCuriosInventory(entity).resolve();
        if (inventory.isEmpty()) return false;
        for (var slot : inventory.get().getCurios().entrySet()) {
            String name = slot.getKey();
            if (!name.equals("tinker_charm") && !name.equals("curio")) continue;
            var stacks = slot.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (excluded != null && excluded.identifier().equals(name) && excluded.index() == i) continue;
                if (validInSlot(stacks.getStackInSlot(i), name)) return true;
            }
        }
        return false;
    }

    private static boolean active(LivingEntity entity) {
        return entity.isAlive() && entity.isOnFire() && wearing(entity, null);
    }

    @Override
    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context,
                                     UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        if (!context.cosmetic() && !curio.isBroken() && active(context.entity())) {
            // Fixed trait UUIDs, not slot UUIDs: duplicate charms cannot stack.
            consumer.accept(Attributes.ATTACK_SPEED, ATTACK_SPEED);
            consumer.accept(Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED);
        }
    }

    private static void updateAttribute(LivingEntity entity, Attribute attribute,
                                        AttributeModifier modifier, boolean enabled) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return;
        if (!enabled) instance.removeModifier(modifier.getId());
        else if (instance.getModifier(modifier.getId()) == null) instance.addTransientModifier(modifier);
    }

    private void sync(LivingEntity entity, boolean enabled) {
        updateAttribute(entity, Attributes.ATTACK_SPEED, ATTACK_SPEED, enabled);
        updateAttribute(entity, Attributes.MOVEMENT_SPEED, MOVEMENT_SPEED, enabled);
        if (!enabled) burning.remove(entity);
    }

    @Override
    public void onCurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context,
                            LivingEntity entity, ItemStack stack) {
        if (entity.level().isClientSide) return;
        boolean enabled = active(entity);
        sync(entity, enabled);
        if (context.cosmetic() || !enabled) return;
        long now = entity.level().getGameTime();
        BurnState state = burning.computeIfAbsent(entity, ignored -> new BurnState());
        if (state.lastTick == now) return; // One heal clock across all duplicate charms.
        if (state.lastTick != Long.MIN_VALUE && now - state.lastTick != 1) state.ticks = 0;
        state.lastTick = now;
        if (++state.ticks >= 20) {
            state.ticks = 0;
            float missing = entity.getMaxHealth() - entity.getHealth();
            if (missing > 0 && Float.isFinite(missing)) entity.heal(missing * 0.02F);
        }
    }

    @Override
    public void onCurioEquip(IToolStackView curio, ModifierEntry entry, SlotContext context,
                             LivingEntity entity, ItemStack prevStack, ItemStack stack) {
        if (!entity.level().isClientSide) sync(entity, active(entity));
    }

    @Override
    public void onCurioUnequip(IToolStackView curio, ModifierEntry entry, SlotContext context,
                               LivingEntity entity, ItemStack newStack, ItemStack stack) {
        if (entity.level().isClientSide) return;
        // Curios can still expose the outgoing functional stack during this callback.
        boolean remains = context.cosmetic() ? wearing(entity, null)
                : validInSlot(newStack, context.identifier()) || wearing(entity, context);
        sync(entity, entity.isAlive() && entity.isOnFire() && remains);
    }

    @Override
    public void onDamageTargetPre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event,
                                  LivingEntity attacker, LivingEntity target) {
        if (attacker.level().isClientSide || event.isCanceled() || !active(attacker)
                || event.getAmount() <= 0 || !Float.isFinite(event.getAmount())) return;
        if (boostedDamage.put(event, Boolean.TRUE) == null) event.setAmount(event.getAmount() * 1.20F);
    }

    @Override
    public void onCurioProjectileLaunch(IToolStackView curio, ModifierEntry entry, LivingEntity shooter,
                                        Projectile projectile, AbstractArrow arrow, ModDataNBT persistentData) {
        if (shooter.level().isClientSide || arrow == null) return;
        String marker = LAUNCH_SNAPSHOT.toString();
        if (persistentData.getBoolean(LAUNCH_SNAPSHOT) || projectile.getPersistentData().getBoolean(marker)) {
            // Keep both stores in agreement when another modifier clones an arrow's ForgeData.
            persistentData.putBoolean(LAUNCH_SNAPSHOT, true);
            projectile.getPersistentData().putBoolean(marker, true);
            return;
        }
        // TCLib dispatches at EntityJoinLevelEvent, after vanilla bow/crossbow shoot().
        // Record even an inactive snapshot so later reloads cannot enable the bonus.
        persistentData.putBoolean(LAUNCH_SNAPSHOT, true);
        projectile.getPersistentData().putBoolean(marker, true);
        if (active(shooter)) projectile.setDeltaMovement(projectile.getDeltaMovement().scale(1.20D));
    }

    private static final class BurnState {
        long lastTick = Long.MIN_VALUE;
        int ticks;
    }
}
