package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.NotNull;
import org.mylove.tprt.Tprt;
import org.slf4j.Logger;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.combat.MeleeHitModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import java.util.HashMap;
import java.util.Map;

/**
 * 附魔 (enchanting)
 * <p>
 * 结构参考匠魂的「均衡」({@code tconstruct:rebalanced}): 同一个词条, <b>用哪条强化配方贴上, 就得到哪种效果</b>。
 * 这里把「效果」做成了法术流派: 用火焰配方贴上就是火焰附魔, 换成冰霜配方会把流派替换成冰霜
 * (匠魂 {@code tconstruct:swappable_modifier} 的“同流派重复贴会被拦下、换流派则替换”的规则)。
 * <p>
 * <b>实际效果</b>: 近战命中时, 在结算攻击伤害<b>之前</b>先对目标追加一次对应流派的法术伤害:
 * <pre>
 *   伤害 = 本次攻击伤害 × 倍率 × 通用法强(SPELL_POWER) × 对应流派法强 × 目标法术抗性
 *   倍率默认 0.5 (50%), 可在数据包里按流派单独配置
 * </pre>
 * 通用法强与流派法强的取法跟铁魔法本体一致 ({@code SPELL_POWER} × {@code SchoolType#getPowerFor}),
 * 抗性也走本体的 {@link DamageSources#getResist}, 所以加成/减免的算法和铁魔法自己的法术相同。
 * <p>
 * <b>数据包</b>: {@code data/<命名空间>/enchanting_schools/<名字>.json}
 * <pre>
 * {
 *   "school": "irons_spellbooks:fire",   // 铁魔法流派 id
 *   "multiplier": 0.5                     // 倍率, 省略时用默认 0.5
 * }
 * </pre>
 * 配方的 {@code result.value} 写这个文件的名字(可以是 {@code fire}, 也可以是 {@code 命名空间:名字})。
 */
public class enchanting extends NoLevelsModifier implements MeleeHitModifierHook, ModifierRemovalHook {

    /** 流派定义所在的数据包目录 */
    public static final String DIRECTORY = "enchanting_schools";
    /** 倍率默认值: 攻击伤害的 50% */
    public static final float DEFAULT_MULTIPLIER = 0.5F;

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    /** 数据包里的流派定义: 定义 id → 设定 */
    private static final Map<ResourceLocation, Setting> SETTINGS = new HashMap<>();

    static {
        // 只在类首次加载时注册一次; 词条被实例化时必然已经加载过 (IssCompat 注册时),
        // 早于任何一次数据包重载。显式给出事件类型, 不依赖 lambda 的泛型推导。
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, AddReloadListenerEvent.class, enchanting::onAddReloadListener);
    }

    private static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new SettingReloadListener());
    }

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.MELEE_HIT, ModifierHooks.REMOVE);
    }

    /** 词条被洗掉时顺手清掉流派标记 (和匠魂 swappable 模块的做法一致) */
    @Override
    public Component onRemoved(IToolStackView tool, Modifier modifier) {
        tool.getPersistentData().remove(modifier.getId());
        return null;
    }

    /* ============================================================
     *  效果: 在攻击伤害之前追加一次对应流派的法术伤害
     * ============================================================ */

    @Override
    public float beforeMeleeHit(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float damage, float baseKnockback, float knockback) {
        LivingEntity attacker = context.getAttacker();
        LivingEntity target = context.getLivingTarget();
        if (target == null || attacker.level().isClientSide()) {
            return knockback;
        }
        Setting setting = getSetting(tool, modifier);
        if (setting == null) {
            return knockback;
        }
        SchoolType school = SchoolRegistry.getSchool(setting.school());
        if (school == null) {
            return knockback;
        }

        // 通用法强 × 流派法强 × 目标法术抗性 (与铁魔法的 getSpellPower / getResist 一致)
        double power = attacker.getAttributeValue(AttributeRegistry.SPELL_POWER.get()) * school.getPowerFor(attacker);
        float resist = DamageSources.getResist(target, school);
        float amount = (float) (damage * setting.multiplier() * power * resist);
        if (amount <= 0.0F) {
            return knockback;
        }

        DamageSource source = new DamageSource(DamageSources.getHolderFromResource(attacker, school.getDamageType()), attacker, attacker);
        // 先清一次无敌帧, 免得这一击被别的来源留下的无敌帧吃掉
        target.invulnerableTime = 0;
        if (target.hurt(source, amount)) {
            target.setLastHurtByMob(attacker);
        }
        // 再清一次, 让紧随其后的近战伤害按满额结算 (这一步属于“攻击伤害前结算”的收尾)
        target.invulnerableTime = 0;
        return knockback;
    }

    /** 读取配方写在工具上的流派标记: 匠魂的 swappable 机制把 value 存在以词条 id 为键的持久数据上 */
    private static Setting getSetting(IToolStackView tool, ModifierEntry modifier) {
        String value = tool.getPersistentData().getString(modifier.getId());
        if (value.isEmpty()) {
            return null;
        }
        // 允许写 "fire" (默认本模组命名空间) 或者 "命名空间:名字"
        ResourceLocation id = value.indexOf(':') >= 0
                ? ResourceLocation.tryParse(value)
                : ResourceLocation.fromNamespaceAndPath(Tprt.MODID, value);
        return id == null ? null : SETTINGS.get(id);
    }

    /* ============================================================
     *  数据包: 流派定义
     * ============================================================ */

    /** 一条流派定义: 铁魔法流派 + 倍率 */
    public record Setting(ResourceLocation school, float multiplier) {}

    /** 扫描 data/<ns>/enchanting_schools/*.json 并替换当前定义 */
    private static class SettingReloadListener extends SimpleJsonResourceReloadListener {
        private SettingReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> entries, @NotNull ResourceManager manager, @NotNull ProfilerFiller profiler) {
            Map<ResourceLocation, Setting> loaded = new HashMap<>();
            entries.forEach((id, json) -> {
                try {
                    JsonObject object = GsonHelper.convertToJsonObject(json, id.toString());
                    ResourceLocation school = ResourceLocation.tryParse(GsonHelper.getAsString(object, "school"));
                    if (school == null) {
                        LOGGER.error("附魔流派定义 {} 的 school 不是合法 id", id);
                        return;
                    }
                    float multiplier = GsonHelper.getAsFloat(object, "multiplier", DEFAULT_MULTIPLIER);
                    loaded.put(id, new Setting(school, multiplier));
                } catch (Exception exception) {
                    LOGGER.error("解析附魔流派定义 {} 失败", id, exception);
                }
            });
            SETTINGS.clear();
            SETTINGS.putAll(loaded);
            LOGGER.info("已载入 {} 个附魔流派定义", SETTINGS.size());
        }
    }
}
