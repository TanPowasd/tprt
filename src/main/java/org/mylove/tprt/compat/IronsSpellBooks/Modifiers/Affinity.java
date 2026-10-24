package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.events.ModifySpellLevelEvent;
import io.redspace.ironsspellbooks.api.item.curios.AffinityData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.mylove.tprt.compat.IronsSpellBooks.AffinityConfig;
import org.mylove.tprt.common.modifier.modifierModule.CuriosModifier;
import org.mylove.tprt.hooks.Curios.CurioInventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import top.theillusivec4.curios.api.SlotContext;

/**
 * 亲和: 记录一个法术, 施放它时获得等级加成。
 *
 * 数据分两份:
 *   1) 物品 NBT (tprt_affinity) = 我们自己的唯一判定依据, 记录时写入;
 *      加成只统计"正在手持"的物品, 背包里的不参与。
 *   2) ISS 的 AffinityData = 只在物品"被戴上饰品栏"时才同步写入,
 *      因为只有饰品能让 ISS 把亲和显示进法术轮盘; 由 ISS 提供饰品那条加成,
 *      我们不再重复计算, 避免同一件饰品加两次。
 *
 * 记录方式: 主手拿法术卷轴 + 副手拿带本词条的物品 + 潜行右键。
 * 加成等级 / 允许的法术由数据包配置 (data/<ns>/affinity/*.json)。
 */
public class Affinity extends CuriosModifier implements InventoryTickModifierHook, CurioInventoryTickModifierHook {

    /** 我们自己的 NBT 键: 记录的法术 id */
    public static final String AFFINITY_TAG = "tprt_affinity";
    /** 数据包加成等级的兜底值 */
    public static final int DEFAULT_BONUS = 1;
    /** 词条 id */
    public static final ModifierId ID = new ModifierId("tprt", "affinity");static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, PlayerInteractEvent.RightClickItem.class, Affinity::onRightClick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ModifySpellLevelEvent.class, Affinity::onModifySpellLevel);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ItemTooltipEvent.class, Affinity::onTooltip);
    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        super.registerHooks(hookBuilder);
        hookBuilder.addHook(this, ModifierHooks.INVENTORY_TICK);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level world, LivingEntity entity,
                                int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        // 手持物品不做额外处理: 数据在记录时写入, 加成按"是否手持"实时判定
    }

    @Override
    public void onCurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context,
                            LivingEntity entity, ItemStack stack) {
        // 只有真正戴在饰品栏的物品才同步 ISS 的亲和数据 (这样轮盘会显示, 背包里的不会)
        if (entity == null || entity.level().isClientSide()) {
            return;
        }
        ResourceLocation spellId = recordedSpell(stack);
        if (spellId == null || AffinityData.hasAffinityData(stack)) {
            return;
        }
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        if (spell != null) {
            AffinityData.set(stack, new AffinityData(spell));
            AffinityData.setAffinityData(stack, spell, affinityLevel(stack) * AffinityConfig.bonus());
        }
    }

    /** 记录亲和法术: 主手卷轴 + 副手带词条物品 + 潜行右键 */
    private static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.isShiftKeyDown()) {
            return;
        }
        ItemStack scroll = event.getItemStack();
        ItemStack tool = player.getOffhandItem();
        AbstractSpell spell = readScrollSpell(scroll);
        if (spell == null || !hasAffinity(tool)) {
            return;
        }
        ResourceLocation spellId = spell.getSpellResource();
        if (!AffinityConfig.allows(spellId)) {
            player.displayClientMessage(Component.translatable("tooltip.tprt.affinity.not_allowed", spell.getDisplayName(player)), true);
            return;
        }
        tool.getOrCreateTag().putString(AFFINITY_TAG, spellId.toString());
        if (!player.getAbilities().instabuild) {
            scroll.shrink(1);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        player.swing(InteractionHand.MAIN_HAND);
        event.setCanceled(true);}

    /** 法术等级计算: 每件正在手持、且记录了该法术的亲和物品 +1 级 (可叠加) */
    private static void onModifySpellLevel(ModifySpellLevelEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        AbstractSpell spell = event.getSpell();
        if (spell == null) {
            return;
        }
        ResourceLocation spellId = spell.getSpellResource();
        // 每件手持物品贡献 "词条等级 × 配置加成" 级 (1:1, 也就是 2 级词条 = +2 级算法术)
        int levels = 0;
        levels += contribution(player.getMainHandItem(), spellId);
        levels += contribution(player.getOffhandItem(), spellId);
        // 穿戴中的盔甲: 每件单独计算 (全身四件可叠加)
        for (ItemStack armor : player.getArmorSlots()) {
            levels += contribution(armor, spellId);
        }
        if (levels > 0) {
            event.addLevels(levels * AffinityConfig.bonus());
        }
    }

    /** tooltip: 显示记录的法术与加成 */
    private static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!hasAffinity(stack)) {
            return;
        }
        ResourceLocation spellId = recordedSpell(stack);
        if (spellId == null) {
            event.getToolTip().add(Component.translatable("tooltip.tprt.affinity.empty"));
            return;
        }
        AbstractSpell spell = SpellRegistry.getSpell(spellId);
        Component name = spell != null ? spell.getDisplayName(event.getEntity()) : Component.literal(spellId.toString());
        int shownBonus = affinityLevel(stack) * AffinityConfig.bonus();
        event.getToolTip().add(Component.translatable("tooltip.tprt.affinity.recorded", name, shownBonus));
    }

    /** 物品带本词条 */
    private static boolean hasAffinity(ItemStack stack) {
        return !stack.isEmpty() && ToolStack.isInitialized(stack)
                && ToolStack.from(stack).getModifierLevel(ID) > 0;
    }

    /** 单件物品给出的等级贡献 = 词条等级 (记录的法术不匹配时为 0) */
    private static int contribution(ItemStack stack, ResourceLocation spellId) {
        if (!matches(stack, spellId)) {
            return 0;
        }
        return Math.max(1, ToolStack.from(stack).getModifierLevel(ID));
    }

    /** 物品上本词条的等级 (没有则 0) */
    private static int affinityLevel(ItemStack stack) {
        if (!hasAffinity(stack)) {
            return 0;
        }
        return Math.max(1, ToolStack.from(stack).getModifierLevel(ID));
    }
    /** 物品带本词条, 且记录的就是这个法术 */
    private static boolean matches(ItemStack stack, ResourceLocation spellId) {
        if (!hasAffinity(stack) || !stack.hasTag() || stack.getTag() == null) {
            return false;
        }
        return spellId.toString().equals(stack.getTag().getString(AFFINITY_TAG));
    }

    /** 物品记录的法术 id, 没有则 null */
    private static ResourceLocation recordedSpell(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || stack.getTag() == null) {
            return null;
        }
        String raw = stack.getTag().getString(AFFINITY_TAG);
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return ResourceLocation.tryParse(raw);
    }

    /** 从法术卷轴取出法术 */
    private static AbstractSpell readScrollSpell(ItemStack stack) {
        if (stack.isEmpty() || !ISpellContainer.isSpellContainer(stack)) {
            return null;
        }
        ISpellContainer container = ISpellContainer.get(stack);
        if (container == null) {
            return null;
        }
        SpellData data = container.getSpellAtIndex(0);
        return data == null ? null : data.getSpell();
    }
}
