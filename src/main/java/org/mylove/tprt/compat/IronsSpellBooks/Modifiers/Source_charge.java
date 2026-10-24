package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
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
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 源质蓄能 (武器): 近战命中叠层, 满层引爆并回响下一次施法。
 *
 * 叠层在 MeleeHit 钩子里完成 (一次攻击只叠一层);
 * 引爆在 MeleeDamage 钩子里判定, 等近战伤害真正结算后 (LivingDamageEvent)
 * 再清无敌帧并造成独立魔法伤害, 这样引爆能吃到最终的近战伤害。
 *
 * 引爆伤害 = 最终近战伤害 × 层数 × 40% × min(√法术强度, 2)
 * 回响     = 下一次施法等级 +50%
 */
public class Source_charge extends NoLevelsModifier implements MeleeDamageModifierHook, MeleeHitModifierHook {

    public static final ModifierId ID = new ModifierId("tprt", "source_charge");
    public static final int MAX_CHARGE = 5;
    public static final int CHARGE_DURATION = 600;
    public static final float DETONATE_RATE = 0.40F;
    public static final float SPELL_POWER_CAP = 2.0F;
    public static final int ECHO_DURATION = 200;
    public static final float ECHO_LEVEL_RATE = 0.5F;

    /** 待引爆登记: 玩家 -> (目标, 层数) */
    private record Pending(LivingEntity target, int layers) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    /** 本次攻击已经触发了引爆, 命中后不再叠层 */
    private static final Set<UUID> DETONATED = new HashSet<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ModifySpellLevelEvent.class, Source_charge::onModifySpellLevel);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, LivingDamageEvent.class, Source_charge::onLivingDamage);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_DAMAGE, ModifierHooks.MELEE_HIT);
    }

    /** 满层时登记引爆 (不改伤害, 也不在这里叠层) */
    @Override
    public float getMeleeDamage(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context,
                                float baseDamage, float damage) {
        Player player = context.getPlayerAttacker();
        LivingEntity target = context.getLivingTarget();
        if (player == null || target == null || player.level().isClientSide) {
            return damage;
        }
        MobEffectInstance charge = player.getEffect(IssCompat.SOURCE_CHARGE.get());
        int current = charge == null ? -1 : charge.getAmplifier();
        if (current < MAX_CHARGE - 1) {
            return damage;   // 未满层: 交给 afterMeleeHit 叠层
        }
        DETONATED.add(player.getUUID());
        PENDING.put(player.getUUID(), new Pending(target, current + 1));
        player.removeEffect(IssCompat.SOURCE_CHARGE.get());
        player.addEffect(new MobEffectInstance(IssCompat.SOURCE_ECHO.get(), ECHO_DURATION, 0, false, true, true));
        return damage;
    }

    /** 命中后叠一层; 触发引爆的那一次不叠 */
    @Override
    public void afterMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damageDealt) {
        Player player = context.getPlayerAttacker();
        if (player == null || player.level().isClientSide) {
            return;
        }
        if (DETONATED.remove(player.getUUID())) {
            return;
        }
        MobEffectInstance charge = player.getEffect(IssCompat.SOURCE_CHARGE.get());
        int current = charge == null ? -1 : charge.getAmplifier();
        int amplifier = Math.min(MAX_CHARGE - 1, current + 1);
        player.addEffect(new MobEffectInstance(IssCompat.SOURCE_CHARGE.get(), CHARGE_DURATION, amplifier, false, true, true));
        if (amplifier == MAX_CHARGE - 1 && current < MAX_CHARGE - 1) {
            player.displayClientMessage(Component.translatable("tooltip.tprt.source_charge.full"), true);
        }
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
        if (!target.isAlive()) {
            return;
        }
        float spellPower = 1.0F;
        AttributeInstance attribute = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
        if (attribute != null) {
            spellPower = (float) attribute.getValue();
        }
        float power = Math.min(SPELL_POWER_CAP, (float) Math.sqrt(Math.max(0.0, spellPower)));
        float detonate = event.getAmount() * pending.layers() * DETONATE_RATE * power;
        if (detonate <= 0.0F) {
            return;
        }
        target.invulnerableTime = 0;
        target.hurt(player.level().damageSources().magic(), detonate);
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
        int bonus = Math.max(1, Math.round(level * ECHO_LEVEL_RATE));
        event.setLevel(level + bonus);
    }
}
