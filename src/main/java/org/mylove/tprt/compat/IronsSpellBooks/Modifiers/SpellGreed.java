package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import com.ssakura49.sakuratinker_tools.compat.irons_spellbooks.library.hook.ISSHooks;
import com.ssakura49.sakuratinker_tools.compat.irons_spellbooks.library.hook.spellbook.SpellCastModifierHook;
import com.ssakura49.sakuratinker_tools.compat.irons_spellbooks.tool.context.SpellAttackContext;
import com.ssakura49.sakuratinker_tools.compat.irons_spellbooks.tool.item.base.ModifiableSpellBookItem;
import com.ssakura49.tinkercuriolib.hook.TCLibHooks;
import com.ssakura49.tinkercuriolib.hook.behavior.CurioAttributeModifierHook;
import com.ssakura49.tinkercuriolib.hook.interation.CurioInventoryTickModifierHook;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.mylove.tprt.registries.ModBuffRegistry;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import top.theillusivec4.curios.api.SlotContext;
import java.util.*;
import java.util.function.BiConsumer;

/** Player-wide progress; only the spellbook selected by Iron's own resolver is effective. */
public class SpellGreed extends NoLevelsModifier implements SpellCastModifierHook, CurioAttributeModifierHook, CurioInventoryTickModifierHook {
    private static final UUID POWER = UUID.fromString("e3b54890-3c32-47e1-a783-5e4d0c521a81");
    private static final UUID COOLDOWN = UUID.fromString("9625e9d7-0447-4e1c-bc65-590c4c879377");
    private static final Map<LivingEntity, Progress> PROGRESS = new WeakHashMap<>();
    private static class Progress { int stacks, pending; long cast, activate, tick = Long.MIN_VALUE; }

    @Override protected void registerHooks(ModuleHookMap.Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, ISSHooks.ON_SPELL_CAST, TCLibHooks.CURIO_ATTRIBUTE, TCLibHooks.CURIO_TICK);
    }
    private static boolean effective(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        ItemStack book = Utils.getPlayerSpellbookStack(player);
        return book != null && !book.isEmpty() && book.getItem() instanceof ModifiableSpellBookItem
                && ToolStack.from(book).getModifierLevel(org.mylove.tprt.compat.IronsSpellBooks.IssCompat.spell_greed.get()) > 0;
    }
    @Override public void onCast(IToolStackView tool, ModifierEntry entry, SpellAttackContext context, CastSource source) {
        LivingEntity entity = context.getAttacker();
        if (entity == null || entity.level().isClientSide) return;
        Progress p = PROGRESS.computeIfAbsent(entity, ignored -> new Progress());
        p.pending = Math.min(5, p.pending + 1);
        p.cast = entity.level().getGameTime();
        p.activate = p.cast + 1; // The library calls onCast BEFORE the triggering spell executes.
    }
    @Override public void modifyCurioAttribute(IToolStackView tool, ModifierEntry entry, SlotContext context,
                                               UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        LivingEntity entity = context.entity();
        if (entity == null || !effective(entity)) return;
        Progress p = PROGRESS.get(entity);
        int stacks = p == null ? 0 : p.stacks;
        consumer.accept(AttributeRegistry.SPELL_POWER.get(), new AttributeModifier(POWER, "spell_greed", power(entity, stacks), AttributeModifier.Operation.ADDITION));
        consumer.accept(AttributeRegistry.COOLDOWN_REDUCTION.get(), new AttributeModifier(COOLDOWN, "spell_greed", stacks * 0.04, AttributeModifier.Operation.ADDITION));
    }
    @Override public void onCurioTick(IToolStackView tool, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack stack) {
        tick(entity);
    }
    private static double power(LivingEntity entity, int stacks) {
        return (entity.getHealth() >= entity.getMaxHealth() ? 0.10 : 0) + stacks * 0.04;
    }
    private static void attribute(LivingEntity entity, Attribute type, UUID id, double amount) {
        AttributeInstance instance = entity.getAttribute(type);
        if (instance == null) return;
        AttributeModifier old = instance.getModifier(id);
        if (old != null && old.getAmount() == amount) return;
        if (old != null) instance.removeModifier(id);
        if (amount != 0) instance.addTransientModifier(new AttributeModifier(id, "spell_greed", amount, AttributeModifier.Operation.ADDITION));
    }
    /** Also called by the marker effect, so losing the last effective book cannot strand attributes. */
    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        if (!effective(entity)) {
            PROGRESS.remove(entity);
            attribute(entity, AttributeRegistry.SPELL_POWER.get(), POWER, 0);
            attribute(entity, AttributeRegistry.COOLDOWN_REDUCTION.get(), COOLDOWN, 0);
            entity.removeEffect(ModBuffRegistry.SPELL_GREED.get());
            return;
        }
        Progress p = PROGRESS.computeIfAbsent(entity, ignored -> new Progress());
        long now = entity.level().getGameTime();
        if (p.tick == now) return;
        p.tick = now;
        if (p.pending > 0 && now >= p.activate) {
            p.stacks = Math.min(5, p.stacks + p.pending);
            p.pending = 0;
        }
        // A cast refreshes the whole stack's eight-second lifetime.
        if (now >= p.cast + 160) {
            p.stacks = 0;
            p.pending = 0;
        }
        attribute(entity, AttributeRegistry.SPELL_POWER.get(), POWER, power(entity, p.stacks));
        attribute(entity, AttributeRegistry.COOLDOWN_REDUCTION.get(), COOLDOWN, p.stacks * 0.04);
        MobEffectInstance effect = entity.getEffect(ModBuffRegistry.SPELL_GREED.get());
        if (p.stacks == 0) {
            if (effect != null) entity.removeEffect(ModBuffRegistry.SPELL_GREED.get());
        } else {
            int remaining = (int) Math.max(1, p.cast + 160 - now);
            if (effect == null || effect.getAmplifier() != p.stacks - 1
                    || Math.abs(effect.getDuration() - remaining) > 1) {
                entity.removeEffect(ModBuffRegistry.SPELL_GREED.get());
                entity.addEffect(new MobEffectInstance(ModBuffRegistry.SPELL_GREED.get(), remaining, p.stacks - 1, false, false, true));
            }
        }
    }
}
