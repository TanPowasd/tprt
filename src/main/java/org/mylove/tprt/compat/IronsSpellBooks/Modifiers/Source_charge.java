package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.mylove.tprt.compat.IronsSpellBooks.IssCompat;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeDamageModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 源质蓄能: 施法叠层(buff), 满层近战引爆, 引爆后回响下一次施法。
 *
 * 引爆采用"延后结算": 近战钩子里只登记, 等近战伤害真正结算后
 * (LivingDamageEvent) 再清无敌帧并造成独立魔法伤害,
 * 这样引爆能吃到最终的近战伤害, 也不会干扰近战本身的结算。
 *
 * 引爆伤害 = 最终近战伤害 × 层数 × 40% × √法术强度
 * 回响     = 下一次施法等级 +50% (无上限)
 */
public class Source_charge extends NoLevelsModifier implements MeleeDamageModifierHook {public static final ModifierId ID = new ModifierId("tprt", "source_charge");
    public static final int MAX_CHARGE = 5;
    public static final int CHARGE_DURATION = 600;
    public static final float DETONATE_RATE = 0.40F;
    public static final int ECHO_DURATION = 200;
    public static final float ECHO_LEVEL_RATE = 0.5F;

    /** 待引爆登记: 玩家 -> (目标, 层数) */
    private record Pending(LivingEntity target, int layers) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, SpellOnCastEvent.class, Source_charge::onSpellCast);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ModifySpellLevelEvent.class, Source_charge::onModifySpellLevel);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDamageEvent.class, Source_charge::onLivingDamage);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE);
    }

    /** 施法: 叠一层蓄能 (上限 5, 刷新持续时间)。回响由 ModifySpellLevelEvent 消费。 */
    private static void onSpellCast(SpellOnCastEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide) {
            return;
        }MobEffectInstance current = player.getEffect(IssCompat.SOURCE_CHARGE.get());
        int amplifier = current == null ? 0 : Math.min(MAX_CHARGE - 1, current.getAmplifier() + 1);
        player.addEffect(new MobEffectInstance(IssCompat.SOURCE_CHARGE.get(), CHARGE_DURATION, amplifier, false, true, true));
        if (amplifier == MAX_CHARGE - 1 && (current == null || current.getAmplifier() < MAX_CHARGE - 1)) {
            player.displayClientMessage(Component.translatable("tooltip.tprt.source_charge.full"), true);
        }
    }

    /** 回响: 下一次施法等级 +50% */
    private static void onModifySpellLevel(ModifySpellLevelEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        MobEffectInstance echo = player.getEffect(IssCompat.SOURCE_ECHO.get());
        if (echo == null || echo.getAmplifier() != 0) {
            return;
        }
        AbstractSpell spell = event.getSpell();
        if (spell == null) {
            return;
        }
        player.removeEffect(IssCompat.SOURCE_ECHO.get());
        int level = event.getLevel();
        // 放开等级上限: 与亲和之戒一致
        int bonus = Math.max(1, Math.round(level * ECHO_LEVEL_RATE));
        int boosted = level + bonus;
        event.setLevel(boosted);}

    /** 近战: 满层时只登记待引爆, 不给目标造成伤害 */
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                float baseDamage, float damage) {
        LivingEntity attacker = context.getPlayerAttacker();
        LivingEntity target = context.getLivingTarget();
        if (!(attacker instanceof Player player) || target == null || player.level().isClientSide) {
            return damage;
        }
        MobEffectInstance charge = player.getEffect(IssCompat.SOURCE_CHARGE.get());if (charge == null || charge.getAmplifier() < MAX_CHARGE - 1) {
            return damage;
        }
        int layers = charge.getAmplifier() + 1;
        PENDING.put(player.getUUID(), new Pending(target, layers));
        player.removeEffect(IssCompat.SOURCE_CHARGE.get());
        player.addEffect(new MobEffectInstance(IssCompat.SOURCE_ECHO.get(), ECHO_DURATION, 0, false, true, true));
        return damage;
    }

    /** 近战伤害结算后: 清无敌帧并引爆 */
    private static void onLivingDamage(LivingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        Pending pending = PENDING.remove(player.getUUID());
        if (pending == null || pending.target() != event.getEntity()) {
            return;
        }
        LivingEntity target = pending.target();
        if (!target.isAlive()) {return;
        }
        float spellPower = 1.0F;
        AttributeInstance attribute = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
        if (attribute != null) {
            spellPower = (float) attribute.getValue();
        }
        float detonate = event.getAmount() * pending.layers() * DETONATE_RATE * (float) Math.sqrt(Math.max(0.0, spellPower));
        if (detonate <= 0.0F) {
            return;
        }
        // 清无敌帧, 防止延后结算的伤害被吞掉
        target.invulnerableTime = 0;
        target.hurt(player.level().damageSources().magic(), detonate);}
}