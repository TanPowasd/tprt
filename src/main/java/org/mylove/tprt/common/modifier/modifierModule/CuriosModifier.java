package org.mylove.tprt.common.modifier.modifierModule;

import com.ssakura49.tinkercuriolib.content.ProjectileImpactContent;
import com.ssakura49.tinkercuriolib.event.ItemStackDamageEvent;
import com.ssakura49.tinkercuriolib.event.LivingDamageCalculationEvent;
import com.ssakura49.tinkercuriolib.hook.TCLibHooks;
import com.ssakura49.tinkercuriolib.hook.armor.CurioEquipmentChangeModifierHook;
import com.ssakura49.tinkercuriolib.hook.armor.CurioTakeDamagePostModifierHook;
import com.ssakura49.tinkercuriolib.hook.armor.CurioTakeDamagePreModifierHook;
import com.ssakura49.tinkercuriolib.hook.behavior.*;
import com.ssakura49.tinkercuriolib.hook.combat.*;
import com.ssakura49.tinkercuriolib.hook.interation.CurioInventoryTickModifierHook;
import com.ssakura49.tinkercuriolib.hook.mining.CurioBreakSpeedModifierHook;
import com.ssakura49.tinkercuriolib.hook.ranged.CurioProjectileDamageModifierHook;
import com.ssakura49.tinkercuriolib.hook.ranged.CurioProjectileHitModifierHook;
import com.ssakura49.tinkercuriolib.hook.ranged.CurioProjectileLaunchModifierHook;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ModDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ModifierNBT;
import top.theillusivec4.curios.api.SlotContext;

import java.util.UUID;
import java.util.function.BiConsumer;

public abstract class CuriosModifier extends Modifier implements CurioFortuneModifierHook, CurioEquipmentChangeModifierHook, CurioInventoryTickModifierHook, CurioLootingModifierHook, CurioAttributeModifierHook, CurioGetToolDamageModifierHook, CurioTakeHealModifierHook, CurioBreakSpeedModifierHook, CurioTakeDamagePreModifierHook, CurioTakeDamagePostModifierHook, CurioProjectileLaunchModifierHook, CurioProjectileHitModifierHook, CurioDamageTargetPreModifierHook, CurioDamageTargetPostModifierHook, CurioDamageCalculateModifierHook, CurioKillTargetModifierHook, CurioDropRuleModifierHook, CurioProjectileDamageModifierHook {

    protected void registerHooks(ModuleHookMap.@NotNull Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, TCLibHooks.CURIO_FORTUNE, TCLibHooks.CURIO_EQUIPMENT_CHANGE,TCLibHooks.CURIO_TICK,TCLibHooks.CURIO_LOOTING,TCLibHooks.CURIO_ATTRIBUTE,TCLibHooks.CURIO_TOOL_DAMAGE,TCLibHooks.CURIO_TAKE_HEAL,TCLibHooks.CURIO_BREAK_SPEED,TCLibHooks.CURIO_TAKE_DAMAGE_PRE,TCLibHooks.CURIO_TAKE_DAMAGE_POST,TCLibHooks.CURIO_PROJECTILE_LAUNCH,TCLibHooks.CURIO_PROJECTILE_HIT,TCLibHooks.CURIO_DAMAGE_TARGET_PRE,TCLibHooks.CURIO_DAMAGE_TARGET_POST,TCLibHooks.CURIO_CALCULATE_DAMAGE,TCLibHooks.CURIO_KILL_TARGET,TCLibHooks.CURIO_DROP_RULE,TCLibHooks.CURIO_PROJECTILE_DAMAGE);
    }

    public CuriosModifier() {
        MinecraftForge.EVENT_BUS.addListener(this::LivingHurtEvent);
        MinecraftForge.EVENT_BUS.addListener(this::LivingAttackEvent);
        MinecraftForge.EVENT_BUS.addListener(this::LivingDamageEvent);
    }

    public void LivingHurtEvent(LivingHurtEvent target) {
    }

    public void LivingAttackEvent(LivingAttackEvent target) {
    }

    public void LivingDamageEvent(LivingDamageEvent target) {
    }

    public boolean isNoLevels() {
        return false;
    }

    public int getPriority() {
        return 100;
    }

    public @NotNull Component getDisplayName(int level) {
        return this.isNoLevels() ? super.getDisplayName() : super.getDisplayName(level);
    }

    @Override
    public void onCurioEquip(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack prevStack, ItemStack stack) {
        CurioEquipmentChangeModifierHook.super.onCurioEquip(curio, entry, context, entity, prevStack, stack);
    }

    @Override
    public void onCurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack stack) {
        CurioInventoryTickModifierHook.super.onCurioTick(curio, entry, context, entity, stack);
    }

    @Override
    public int onCurioUpdateFortune(IToolStackView iToolStackView, ModifierEntry modifierEntry, SlotContext slotContext, LootContext lootContext, ItemStack itemStack, int i) {
        return 0;
    }

    @Override
    public void onCurioUnequip(IToolStackView curio, ModifierEntry entry, SlotContext context, LivingEntity entity, ItemStack newStack, ItemStack stack) {
        CurioEquipmentChangeModifierHook.super.onCurioUnequip(curio, entry, context, entity, newStack, stack);
    }

    @Override
    public int onCurioUpdateLooting(IToolStackView iToolStackView, ModifierEntry modifierEntry, SlotContext slotContext, DamageSource damageSource, LivingEntity livingEntity, ItemStack itemStack, int i) {
        return 0;
    }

    @Override
    public void modifyCurioAttribute(IToolStackView curio, ModifierEntry entry, SlotContext context, UUID uuid, BiConsumer<Attribute, AttributeModifier> consumer) {
        CurioAttributeModifierHook.super.modifyCurioAttribute(curio, entry, context, uuid, consumer);
    }

    @Override
    public void onCurioGetToolDamage(IToolStackView curio, ModifierEntry entry, LivingEntity entity, ItemStackDamageEvent event) {
        CurioGetToolDamageModifierHook.super.onCurioGetToolDamage(curio, entry, entity, event);
    }

    @Override
    public void onCurioTakeHeal(IToolStackView curio, ModifierEntry entry, LivingHealEvent event, LivingEntity entity) {
        CurioTakeHealModifierHook.super.onCurioTakeHeal(curio, entry, event, entity);
    }

    @Override
    public void onCurioBreakSpeed(IToolStackView curio, ModifierEntry entry, PlayerEvent.BreakSpeed event, Player player) {
        CurioBreakSpeedModifierHook.super.onCurioBreakSpeed(curio, entry, event, player);
    }

    @Override
    public void onCurioCalculateDamage(IToolStackView curio, ModifierEntry entry, LivingDamageCalculationEvent event, LivingEntity attacker, LivingEntity target) {
        CurioDamageCalculateModifierHook.super.onCurioCalculateDamage(curio, entry, event, attacker, target);
    }

    @Override
    public void onCurioProjectileHit(IToolStackView curio, ModifierEntry entry, LivingEntity shooter, ProjectileImpactContent content, HitResult hit) {
        CurioProjectileHitModifierHook.super.onCurioProjectileHit(curio, entry, shooter, content, hit);
    }

    @Override
    public void onCurioDamageTargetPost(IToolStackView curio, ModifierEntry entry, LivingDamageEvent event, LivingEntity attacker, LivingEntity target) {
        CurioDamageTargetPostModifierHook.super.onCurioDamageTargetPost(curio, entry, event, attacker, target);
    }

    @Override
    public void onCurioProjectileLaunch(IToolStackView curio, ModifierEntry entry, LivingEntity shooter, Projectile projectile, @Nullable AbstractArrow arrow, ModDataNBT persistentData) {
        CurioProjectileLaunchModifierHook.super.onCurioProjectileLaunch(curio, entry, shooter, projectile, arrow, persistentData);
    }

    @Override
    public float getProjectileDamage(ModDataNBT persistentData, ModifierEntry entry, ModifierNBT modifiers, @NotNull Projectile projectile, @javax.annotation.Nullable AbstractArrow arrow, @javax.annotation.Nullable LivingEntity attacker, @NotNull Entity target, float baseDamage, float damage){
        return baseDamage;
    }

    @Override
    public void onCurioTakeDamagePost(IToolStackView curio, ModifierEntry entry, LivingDamageEvent event, LivingEntity entity, DamageSource source) {
        CurioTakeDamagePostModifierHook.super.onCurioTakeDamagePost(curio, entry, event, entity, source);
    }

    @Override
    public void onCurioToKillTarget(IToolStackView curio, ModifierEntry entry, LivingDeathEvent event, LivingEntity attacker, LivingEntity target) {
        CurioKillTargetModifierHook.super.onCurioToKillTarget(curio, entry, event, attacker, target);
    }

    @Override
    public void onCurioTakeDamagePre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event, LivingEntity entity, DamageSource source) {
        CurioTakeDamagePreModifierHook.super.onCurioTakeDamagePre(curio, entry, event, entity, source);
    }

    @Override
    public void onDamageTargetPre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event, LivingEntity attacker, LivingEntity target) {
        CurioDamageTargetPreModifierHook.super.onDamageTargetPre(curio, entry, event, attacker, target);
    }

    @Override
    public void clearCache(@NotNull PackType packType) {
        super.clearCache(packType);
    }
}
