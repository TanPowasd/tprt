package org.mylove.tprt.Modifiers.armor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModDamageSource;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/** Forge only transports the split and observes successful fire; damage reductions stay in TC hooks. */
@Mod.EventBusSubscriber(modid = Tprt.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlazeforgedArmorEvents {
    private static final String STACKS = "tprt.quenching.stacks";
    private static final String UNTIL = "tprt.quenching.until";
    private static final ThreadLocal<Boolean> CONVERTING = ThreadLocal.withInitial(() -> false);
    private static final Map<LivingEntity, List<Split>> PENDING = new WeakHashMap<>();
    // Vanilla compares future hits against this field as well as invulnerableTime.
    private static final Field LAST_HURT = ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20898_");

    private BlazeforgedArmorEvents() {}

    private record Split(LivingHurtEvent original, DamageSource source, float amount, long due) {}

    public static int getStacks(LivingEntity entity) {
        var data = entity.getPersistentData();
        if (entity.level().getGameTime() >= data.getLong(UNTIL)) return 0;
        return Math.min(10, Math.max(0, data.getInt(STACKS)));
    }

    private static boolean wearing(LivingEntity entity, boolean quenching) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !ToolStack.isInitialized(stack)) continue;
            ToolStack tool = ToolStack.from(stack);
            if (!tool.isBroken() && ModifierUtil.validArmorSlot(tool, slot)
                    && tool.getModifierLevel(quenching ? QuenchingModifier.ID : FlameBathedModifier.ID) > 0) return true;
        }
        return false;
    }

    /** Split BEFORE TC's normal-priority armor math, so neither portion is armored twice. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void split(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        if (entity.level().isClientSide || CONVERTING.get() || !(event.getAmount() > 0)
                || source.is(DamageTypeTags.IS_FIRE) || !FlameBathedModifier.CAN_PROTECT.matches(source)
                || !wearing(entity, false)) return;
        float half = event.getAmount() * 0.5F;
        event.setAmount(half); // Never cancel/replay the original nonfire damage.
        // This fire portion still goes through armor; vanilla on_fire bypasses armor.
        DamageSource fire = new DamageSource(entity.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModDamageSource.CONVERTED_FIRE),
                source.getEntity());
        PENDING.computeIfAbsent(entity, ignored -> new ArrayList<>())
                .add(new Split(event, fire, half, entity.level().getGameTime() + 1));
    }

    /** Snapshot the final positive amount; never call hurt() or feed an event back into itself here. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFireDamage(LivingDamageEvent event) {
        float actual = event.getAmount();
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(actual > 0) || !event.getSource().is(DamageTypeTags.IS_FIRE)
                || !wearing(entity, true)) return;
        var data = entity.getPersistentData();
        data.putInt(STACKS, Math.min(10, getStacks(entity) + 1));
        data.putLong(UNTIL, entity.level().getGameTime() + 200);
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) PENDING.remove(living);
    }

    /** Next entity tick avoids the first transaction's vanilla invulnerability window. */
    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        List<Split> pending = PENDING.get(entity);
        if (pending == null) return;
        long now = entity.level().getGameTime();
        List<Split> ready = new ArrayList<>();
        pending.removeIf(split -> {
            if (split.due() > now) return false;
            ready.add(split);
            return true;
        });
        if (pending.isEmpty()) PENDING.remove(entity);
        for (Split split : ready) {
            if (!entity.isAlive() || entity.isRemoved() || split.original().isCanceled()) continue;
            int invulnerable = entity.invulnerableTime;
            int hurt = entity.hurtTime;
            int duration = entity.hurtDuration;
            try {
                float lastHurt = LAST_HURT.getFloat(entity);
                try {
                    CONVERTING.set(true);
                    entity.invulnerableTime = 0;
                    entity.hurt(split.source(), split.amount());
                } finally {
                    LAST_HURT.setFloat(entity, lastHurt);
                    entity.invulnerableTime = invulnerable;
                    entity.hurtTime = hurt;
                    entity.hurtDuration = duration;
                    CONVERTING.remove();
                }
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Cannot preserve vanilla damage frames for flame_bathed", exception);
            }
        }
    }
}
