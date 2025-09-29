package org.mylove.tprt.tags;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.hooks.DamageRedirectHook;
import org.mylove.tprt.hooks.KillingHook;
import org.mylove.tprt.hooks.TprtHooks;
import slimeknights.mantle.client.TooltipKey;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.behavior.AttributesModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ToolStatsModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.VolatileDataModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.display.TooltipModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.GeneralInteractionModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InteractionSource;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolDataNBT;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import slimeknights.tconstruct.library.tools.stat.ModifierStatsBuilder;
import slimeknights.tconstruct.tools.stats.ToolType;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;



/*
  09/14:
  高等级保留低等级效果
  E:   + 10% 攻
  D:   提高至 + 30% 攻  + 0.1 攻速
  C:   提高至 + 100% 攻  + 0.4 攻速
  B:   提高至 + 1.0 攻速, 攻击窃取目标2秒时间
  A:   提高至 + 200% 攻, 所有攻击特效额外触发1次
  S:   攻击为真实伤害且无视无敌帧
  SS:  所有其他词条等级为Ⅲ倍, 千年的时间不再减少
  SSS: (被动) + 10 升级槽、能力槽、刻印次数
 */

/// 千年, 即 630,720,000,000 tick
public class millennium extends NoLevelsModifier implements
        KillingHook, TooltipModifierHook, GeneralInteractionModifierHook, InventoryTickModifierHook, ToolStatsModifierHook,
        MeleeHitModifierHook, AttributesModifierHook, MeleeDamageModifierHook, DamageRedirectHook, VolatileDataModifierHook
{
    public static final ToolType[] CAN_BE_USE_ON_TYPES = {ToolType.MELEE};
    public static final int TickConsumePerT = 10;
    public static final int RankAAttackMultiply = 2;

    private static final ResourceLocation MILLENNIUM_TIME = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "millennium_time");
    private static final ResourceLocation MILLENNIUM_ACTIVE = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "millennium_active");
    private static final ResourceLocation MILLENNIUM_RANK = ResourceLocation.fromNamespaceAndPath(Tprt.MODID, "millennium_rank");

    // from @DamageSpeedTradeModifier
    private final Lazy<UUID> uuid = Lazy.of(() -> UUID.nameUUIDFromBytes(getId().toString().getBytes()));
    private final Lazy<String> attack_damage = Lazy.of(() -> {
        ResourceLocation id = getId();
        return id.getPath() + "." + id.getNamespace() + ".attack_damage";
    });
    private final Lazy<String> attack_speed = Lazy.of(() -> {
        ResourceLocation id = getId();
        return id.getPath() + "." + id.getNamespace() + ".attack_speed";
    });

    @Override
    public int getPriority() {
        return 1000;
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, TprtHooks.KILLING_HOOK, ModifierHooks.TOOLTIP, ModifierHooks.GENERAL_INTERACT, ModifierHooks.INVENTORY_TICK, ModifierHooks.TOOL_STATS, ModifierHooks.MELEE_HIT, ModifierHooks.ATTRIBUTES, ModifierHooks.MELEE_DAMAGE, TprtHooks.DAMAGE_REDIRECT_HOOK, ModifierHooks.VOLATILE_DATA);
    }

    @Override
    public @Nullable DamageSource redirectDamageSource(IToolStackView tool, LivingEntity attacker, LivingEntity target, int level, DamageSource source) {
        RANK R = calculateRank(tool);
        return null;
//        return switch (R) {
//            case S, SS, SSS -> new DamageSource(
//                    attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(PURE),
//                    attacker
//            );
//            default -> null;
//        };
    }

    @Override
    public void addVolatileData(IToolContext context, ModifierEntry modifier, ToolDataNBT volatileData) {
        if (context.getPersistentData().getBoolean(MILLENNIUM_ACTIVE)){
//            RANK R = calculateRank(context.getPersistentData().getFloat(MILLENNIUM_TIME));
//            switch (R) {
//                case SSS:
//                    for (ModifierEntry entries : context.getModifierList()){
//                        if (entries.matches(this)) continue;
//                        volatileData.addSlots();
//                    }
//
//            }
        }
    }

    @Override
    public void addAttributes(IToolStackView tool, ModifierEntry modifier, EquipmentSlot slot, BiConsumer<Attribute, AttributeModifier> consumer) {
        // runs everyTick, this method is good.
        if (isActive(tool)){
            RANK R = calculateRank(tool);
            consumer.accept(Attributes.ATTACK_DAMAGE, new AttributeModifier(uuid.get(), attack_damage.get(), R.attackDamage, AttributeModifier.Operation.MULTIPLY_TOTAL));
            consumer.accept(Attributes.ATTACK_SPEED, new AttributeModifier(uuid.get(), attack_speed.get(), R.attackSpeed, AttributeModifier.Operation.ADDITION));
        }
    }

    @Override
    public void addToolStats(IToolContext context, ModifierEntry modifier, ModifierStatsBuilder builder) {
        // "Called whenever tool stats are rebuilt."

        // 09/14 toolStats 好像不能动态更新 ?
        // 09/17 onToolUse 中手动触发了工具更新, 但此函数似乎有客服两份
        /*
        boolean isA = context.getPersistentData().getBoolean(MILLENNIUM_ACTIVE);
        System.out.println("addToolStats: " + context.getPersistentData().getBoolean(MILLENNIUM_ACTIVE)); // false, then stop update
        if(isA){
            String rankName = context.getPersistentData().getString(MILLENNIUM_RANK);
            System.out.println("addToolStats: " + rankName);
            if (rankName.isEmpty()) rankName = RANK.E.name();
            RANK R = RANK.fromName(rankName);
            HookHelper.runAddToolStats(R, modifier, builder);
        }
        */
    }

    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage) {
        if (!isActive(tool)) return damage;

        LivingEntity target = context.getLivingTarget();
        if (target != null) {
            RANK R = calculateRank(tool);
            switch (R) {
                case A, S, SS, SSS -> {
                    // 将所有攻击钩子翻倍
                    List<ModifierEntry> modifiers = tool.getModifierList();
                    for (ModifierEntry entry : modifiers){
                        if (!entry.matches(this)){
                            for (int i = 0; i < RankAAttackMultiply; i++) {
                                damage = entry.getHook(ModifierHooks.MELEE_DAMAGE).getMeleeDamage(tool, entry, context, baseDamage, damage);
                            }
                        }
                    }
                }
            }
        }
        return damage;
    };

    @Override
    public float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        if (!isActive(tool)) return knockback;

        LivingEntity target = context.getLivingTarget();
        if (target != null) {
            RANK R = calculateRank(tool);
            switch (R) {
                case A, S, SS, SSS -> {
                    if (R != RANK.A) target.invulnerableTime = 0;

                    List<ModifierEntry> modifiers = tool.getModifierList();
                    for (ModifierEntry entry : modifiers){
                        if (!entry.matches(this)){
                            for (int i = 0; i < RankAAttackMultiply; i++) {
                                knockback = entry.getHook(ModifierHooks.MELEE_HIT).beforeMeleeHit(tool, entry, context, damage, baseKnockback, knockback);
                            }
                        }
                    }
                }
            }
        }
        return knockback;
    }

    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        if (!isActive(tool)) return;

        LivingEntity target = context.getLivingTarget();
        if (target != null) {
            RANK R = calculateRank(tool);
            switch (R) {
                case B, A, S, SS, SSS -> {
                    //FvUtil.setTimeLock(target, 40);
                    reapTime(tool, 40);

                    if (R != RANK.B) {
                        List<ModifierEntry> modifiers = tool.getModifierList();
                        for (ModifierEntry entry : modifiers){
                            if (!entry.matches(this)){
                                for (int i = 0; i < RankAAttackMultiply; i++) {
                                    entry.getHook(ModifierHooks.MELEE_HIT).afterMeleeHit(tool, entry, context, damageDealt);
                                }
                            }
                        }
                    }
                }
            }

            // LivingEntity attacker = context.getAttacker();
            // ToolDamageUtil.damageAnimated(tool, 2, attacker, context.getSlotType());
        }
    }

    @Override
    public void onKillLivingTarget(IToolStackView tool, LivingDeathEvent event, LivingEntity attacker, LivingEntity target, int level) {
        if (!canModified(tool)) return;

        if(event.getSource().getEntity() == attacker) {
            float addTime = target.tickCount;
            reapTime(tool, addTime);
        }
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        // if (!(holder instanceof Player player)) return;
        if (!canModified(tool)) return;
        if (!isActive(tool)) return;
        // tick会在客户端执行
        if(world.isClientSide && isSelected && holder.tickCount % 20 == 0) {
            // 激活的千年将放出粒子效果
            for (int i = 0; i < 6; i++) {
                RandomSource random = RandomSource.create();
                world.addParticle(
                        ParticleTypes.ENCHANT,
                        holder.getX() + random.nextDouble(),
                        holder.getY() + random.nextDouble(),
                        holder.getZ() + random.nextDouble(),
                        0,
                        .5,
                        0
                );
            }
            return;
        }
        //if (holder.tickCount % 60 == 0) calculateRankAndSave(tool);

        RANK R = calculateRank(tool);
        int cpuSaver = 5;
        if (R != RANK.SS && R != RANK.SSS && holder.tickCount % cpuSaver == 0) {
            // SS 级后, 千年的时间不再减少
            float time = tool.getPersistentData().getFloat(MILLENNIUM_TIME);
            if (time >= TickConsumePerT * cpuSaver) {
                time -= TickConsumePerT * cpuSaver;
            } else {
                time = 0;
                setActive(tool, false, holder);
            }
            tool.getPersistentData().putFloat(MILLENNIUM_TIME, time);
        }
    }

    @Override
    public InteractionResult onToolUse(IToolStackView tool, ModifierEntry modifier, Player player, InteractionHand hand, InteractionSource source) {
        if (source == InteractionSource.RIGHT_CLICK && !tool.isBroken()) {
            GeneralInteractionModifierHook.startUsing(tool, modifier.getId(), player, hand);
            // 触发一次重新构建
            ToolStack tinkerTool = Modifier.getHeldTool(player, hand);
            if (tinkerTool != null) {
                tinkerTool.rebuildStats();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onFinishUsing(IToolStackView tool, ModifierEntry modifier, LivingEntity entity) {
        if (!canModified(tool)) return;

        if (!tool.isBroken() && entity instanceof Player player) {
            triggerActive(tool, player);
        }
    }

    @Override
    public UseAnim getUseAction(IToolStackView tool, ModifierEntry modifier) {
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(IToolStackView tool, ModifierEntry modifier) {
        return 1;
    }

    @Override
    public void addTooltip(IToolStackView tool, ModifierEntry modifier, @Nullable Player player, List<Component> tooltip, TooltipKey tooltipKey, TooltipFlag tooltipFlag) {
        if (!canModified(tool)) return;

        double sec = Math.floor(tool.getPersistentData().getFloat(MILLENNIUM_TIME) / 20);
        RANK R = calculateRank(tool);
        tooltip.add(Component.literal("已积攒: " + (Double.isNaN(sec) ? 0 : sec) + "秒"));
        tooltip.add(Component.literal("位阶: " + R));
        tooltip.add(Component.literal("离下一阶还有: " + (Math.floor(R.nextRank().tickRequired / 20) - sec) + "秒"));
    }


    private static RANK calculateRankAndSave(IToolStackView tool) {
        RANK R = calculateRank(tool);
        tool.getPersistentData().putString(MILLENNIUM_RANK, R.name());
        return R;
    }
    private static RANK calculateRank(IToolStackView tool) {
        float ticks = tool.getPersistentData().getFloat(MILLENNIUM_TIME);
        return calculateRank(ticks);
    }
    private static RANK calculateRank(float ticks) {
        RANK rank = RANK.E;
        for(RANK r : RANK.values()){
            if(ticks >= r.tickRequired) {
                rank = r;
                break;
            }
        }
        return rank;
    }

    private static boolean triggerActive(IToolStackView tool, LivingEntity user) {
        boolean mode = tool.getPersistentData().getBoolean(MILLENNIUM_ACTIVE);
        tool.getPersistentData().putBoolean(MILLENNIUM_ACTIVE, !mode);
        Level pLevel = user.level();
        if(!mode){
            pLevel.playSound((Player)null, user.getX(), user.getY(), user.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            pLevel.playSound((Player)null, user.getX(), user.getY(), user.getZ(), SoundEvents.EGG_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return !mode;
    }

    private static void setActive(IToolStackView tool, boolean mode, LivingEntity user) {
        tool.getPersistentData().putBoolean(MILLENNIUM_ACTIVE, mode);
        if (isActive(tool) != mode) triggerActive(tool, user);
    }

    private static boolean isActive(IToolStackView tool) {
        return tool.getPersistentData().getBoolean(MILLENNIUM_ACTIVE);
    }

    private static boolean canModified(IToolStackView tool) {
        ToolType type = ToolType.from(tool.getItem(), CAN_BE_USE_ON_TYPES);
        return type != null;
    }

    // 死神, 来收人了...
    private static void reapTime(IToolStackView tool, float time) {
        float time0 = tool.getPersistentData().getFloat(MILLENNIUM_TIME);
        System.out.println("reapTime: " + time0 + " += " + time);

        tool.getPersistentData().putFloat(MILLENNIUM_TIME, time0 + time);
    }

    // ****************************************************************************
    public enum RANK {
//        INFINITE(Float.MAX_VALUE, 2.0f, 1.0f),
//        SSS(630720000000f, 2.0f, 1.0f),
//        SS (63072000000f,  2.0f, 1.0f),
//        S  (6307200000f,   2.0f, 1.0f),
//        A  (630720000f,    2.0f, 1.0f),
//        B  (63072000f,     1.0f, 1.0f),
//        C  (6307200f,      1.0f, 0.4f),
//        D  (630720f,       0.3f, 0.1f),
//        E  (0f,            0.1f, 0.0f);

        // for the sake of test
        INFINITE(Float.MAX_VALUE, 2.0f, 0.4f),
        SSS(6307f, 2.0f, 0.4f),
        SS (630f,  2.0f, 0.4f),
        S  (630f,   2.0f, 0.4f),
        A  (630f,    2.0f, 0.4f),
        B  (630f,     2.0f, 0.4f),
        C  (630f,      2.0f, 0.4f),
        D  (63f,       1.3f, 0.1f),
        E  (0f,            1.1f, 0.0f);

        public final float tickRequired;
        public final float attackDamage;
        public final float attackSpeed;

        public RANK nextRank() {
            var l = Arrays.stream(RANK.values()).filter(r -> r.tickRequired > this.tickRequired).toList();
            return l.isEmpty() ? RANK.INFINITE : l.get(l.size() - 1);
        }

        public static RANK fromName(String name) {
            for (RANK R : RANK.values()) {
                if (Objects.equals(R.name(), name)){
                    return R;
                }
            }
            System.out.println("RANK#fromNameErrot: noSuchName");
            return RANK.E;
        }

        RANK(float tickRequired, float attackDamage, float attackSpeed) {
            this.tickRequired = tickRequired;
            this.attackDamage = attackDamage;
            this.attackSpeed = attackSpeed;
        }
    }
}