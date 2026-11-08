package org.mylove.tprt.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.Modifiers.re.before_i_vanish;
import org.mylove.tprt.Modifiers.re.elegy_of_world_end;
import org.mylove.tprt.Modifiers.re.even_as_the_sun_sets;
import org.mylove.tprt.Modifiers.re.happiest_girl_in_the_world;
import org.mylove.tprt.Modifiers.re.seniorious;
import org.mylove.tprt.Modifiers.re.what_do_you_do_at_the_end_of_the_world;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.registries.ModifierIds;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IModDataView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * TPRT 通用《命运2》(Destiny 2) 风格状态 HUD:
 * <ul>
 *   <li><b>顶部中央复合护盾与生命状态条 (Top-Center Bar)</b>:
 *       还原《命运2》正上方扁平分段生命/护盾长条，集成「末日三问」记忆锚点刻度线、延迟伤害池预扣段、
 *       绯红狂暴脉冲以及「最幸福的女孩」超载护盾 (Overshield)。</li>
 *   <li><b>屏幕左侧纵向状态流 (Left Status Feed)</b>:
 *       还原《命运2》左侧紧凑 Buff/Debuff/CD 列表（几何菱形/三角图标 + 状态名与层数 + 右侧对齐 0:XX 倒计时 + 底边微缩进度条 + 触发闪光动效）。</li>
 *   <li><b>可扩展架构</b>:
 *       已完整接入「挚爱 (chtholly)」5 大专属词条，后续其他材料词条与药水 Buff 可直接通过 {@link #registerCollector(StatusCollector)} 接入。</li>
 * </ul>
 */
public class TprtDestinyHud {

    public static final IGuiOverlay OVERLAY = (gui, graphics, partialTick, width, height) ->
            render(graphics, partialTick, width, height);

    /** 状态分类 (决定左侧图标造型、配色与排序优先级) */
    public enum Category {
        /** 高光爆发/觉醒态 (绯红/曜金上三角 ▲, 置顶显示) */
        BURST(0),
        /** 正面增益/充能态 (冰青菱形 ◆, 居中显示) */
        BUFF(1),
        /** 冷却/调息/负重态 (暗红/灰蓝下三角 ▼, 底部显示) */
        COOLDOWN(2);

        public final int sortOrder;

        Category(int sortOrder) {
            this.sortOrder = sortOrder;
        }
    }

    /** 单条《命运2》左侧状态栏条目 */
    public record StatusEntry(
            String key,
            Category category,
            Component title,
            @Nullable String badge,
            int remainingSeconds,
            float progressRatio,
            int accentColor
    ) {
    }

    /** 外部词条/Buff 状态收集器接口 (供后续扩展其他材料与 Buff 使用) */
    @FunctionalInterface
    public interface StatusCollector {
        void collect(Player player, long gameTime, List<StatusEntry> out);
    }

    private static final List<StatusCollector> COLLECTORS = new CopyOnWriteArrayList<>();

    /** 记录左侧状态首次出现的客户端 gameTime, 用于绘制《命运2》新状态触发时的左侧白光扫掠动效 */
    private static final Map<String, Long> ACTIVE_SINCE = new HashMap<>();

    // ===== 挚爱词条同步 NBT Key =====
    // 1. 末日三问 (护甲)
    private static final ResourceLocation WE_PHASE = Tprt.getResource("we_phase");
    private static final ResourceLocation WE_POOL_TENTHS = Tprt.getResource("we_pool_tenths");
    private static final ResourceLocation WE_BERSERK_SEC = Tprt.getResource("we_berserk_sec");
    private static final ResourceLocation WE_CD_SEC = Tprt.getResource("we_cd_sec");
    // 2. 最幸福的女孩 (护符)
    private static final ResourceLocation HG_SHIELD_TENTHS = Tprt.getResource("hg_shield_tenths");
    private static final ResourceLocation HG_PEAK_TENTHS = Tprt.getResource("hg_peak_tenths");
    private static final ResourceLocation HG_FINALE_SEC = Tprt.getResource("hg_finale_sec");
    private static final ResourceLocation HG_CD_SEC = Tprt.getResource("hg_cd_sec");
    // 3. 瑟尼欧里斯 (主/副手近战)
    private static final ResourceLocation SN_HEARTFIRE = Tprt.getResource("sn_heartfire");
    private static final ResourceLocation SN_MODE = Tprt.getResource("sn_mode");
    private static final ResourceLocation SN_STEAL_STACKS = Tprt.getResource("sn_steal_stacks");
    private static final ResourceLocation SN_STEAL_UNTIL = Tprt.getResource("sn_steal_until");
    private static final ResourceLocation SN_COMBO = Tprt.getResource("sn_combo");
    private static final ResourceLocation SN_REST_UNTIL = Tprt.getResource("sn_rest_until");
    private static final ResourceLocation SN_TUNE_COUNT = Tprt.getResource("sn_tune_count");
    // 4. 在消失之前 (主手/副手/快捷栏)
    private static final ResourceLocation BV_VANISHING_TICKS = Tprt.getResource("vanish_grace_ticks");
    private static final ResourceLocation BV_VANISHING_CD = Tprt.getResource("vanish_cooldown_ticks");
    private static final ResourceLocation BV_VANISHING_HITS = Tprt.getResource("vanish_reforge_hits");
    private static final ResourceLocation BV_GUARDIAN_ACTIVE = Tprt.getResource("vanish_guardian_active");
    // 5. 终末挽歌 (法术书)
    private static final ResourceLocation EL_ACTIVE_TARGETS = Tprt.getResource("elegy_active_targets");
    private static final ResourceLocation EL_MAX_STORED_TENTHS = Tprt.getResource("elegy_max_stored_tenths");
    private static final ResourceLocation EL_REMAIN_SEC = Tprt.getResource("elegy_remain_sec");

    // 配色常量 (Destiny 2 标志性冷冽青白 + 珂朵莉苍蓝与绯红)
    private static final int COLOR_AZURE = 0xFF6EC6FF;
    private static final int COLOR_CRIMSON = 0xFFFF4D6D;
    private static final int COLOR_GOLD = 0xFFFFD166;
    private static final int COLOR_POOL_AMBER = 0xFFFF8C42;
    private static final int COLOR_COOLDOWN = 0xFF8E9AAF;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    static {
        // 默认注册「挚爱 (chtholly)」全套词条状态收集器
        registerCollector(TprtDestinyHud::collectChthollyStatuses);
    }

    /** 注册自定义状态收集器 (供后续其他材料词条或 Buff 接入命运2 HUD) */
    public static void registerCollector(StatusCollector collector) {
        COLLECTORS.add(collector);
    }

    private static void render(GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator() || !player.isAlive() || player.level() == null) {
            return;
        }

        long gameTime = player.level().getGameTime();

        // 1. 顶部居中《命运2》复合护盾/生命/延迟池状态条
        renderTopDestinyBar(graphics, mc.font, player, gameTime, width);

        // 2. 收集并渲染屏幕左侧《命运2》纵向状态流
        List<StatusEntry> entries = new ArrayList<>();
        for (StatusCollector collector : COLLECTORS) {
            collector.collect(player, gameTime, entries);
        }

        if (entries.isEmpty()) {
            ACTIVE_SINCE.clear();
            return;
        }

        entries.sort(Comparator.comparingInt((StatusEntry e) -> e.category.sortOrder));
        renderLeftStatusFeed(graphics, mc.font, entries, gameTime, height);
    }

    // ============================================================
    //  A. 顶部中央《命运2》分段式生命/超载护盾/延迟池状态条
    // ============================================================

    private static void renderTopDestinyBar(GuiGraphics graphics, Font font, Player player, long gameTime, int screenWidth) {
        int worldEndLevel = what_do_you_do_at_the_end_of_the_world.getTotalLevel(player);
        IModDataView armorData = findFirstArmorModData(player);
        IModDataView charmData = findCharmModData(player);

        if (worldEndLevel <= 0 && charmData == null) {
            return;
        }

        int wePhase = armorData != null ? Math.max(1, armorData.getInt(WE_PHASE)) : 1;
        float delayedPool = armorData != null ? armorData.getInt(WE_POOL_TENTHS) / 10.0f : 0.0f;
        int berserkSec = armorData != null ? armorData.getInt(WE_BERSERK_SEC) : 0;

        float hgShield = charmData != null ? charmData.getInt(HG_SHIELD_TENTHS) / 10.0f : 0.0f;
        int hgFinaleSec = charmData != null ? charmData.getInt(HG_FINALE_SEC) : 0;
        float overshield = Math.max(hgShield, player.getAbsorptionAmount());

        float maxHp = Math.max(1.0f, player.getMaxHealth());
        float hp = Mth.clamp(player.getHealth(), 0.0f, maxHp);
        float hpRatio = Mth.clamp(hp / maxHp, 0.0f, 1.0f);

        // 记忆锚点/红血临界分界线 (穿戴末日三问时对应第二问锚点血线, 否则为命运2标准 1/3 临界线)
        float anchorRatio = worldEndLevel > 0
                ? what_do_you_do_at_the_end_of_the_world.getSecondQuestionHpRatio(worldEndLevel)
                : 0.33f;

        int barWidth = 172;
        int barHeight = 5;
        int x0 = (screenWidth - barWidth) / 2;
        int y0 = 10;

        // 1) 外框与命运2斜切端帽背景
        graphics.fill(x0 - 2, y0 - 1, x0 + barWidth + 2, y0 + barHeight + 1, 0x90090D14);
        graphics.fill(x0 - 1, y0 - 1, x0 + barWidth + 1, y0, 0x889CC5E8);
        graphics.fill(x0 - 1, y0 + barHeight, x0 + barWidth + 1, y0 + barHeight + 1, 0x66567896);
        // 左右端帽高亮竖线
        graphics.fill(x0 - 2, y0 - 1, x0 - 1, y0 + barHeight + 1, 0xCC9CC5E8);
        graphics.fill(x0 + barWidth + 1, y0 - 1, x0 + barWidth + 2, y0 + barHeight + 1, 0xCC9CC5E8);

        // 2) 主生命条填充
        int hpFillWidth = Math.round(barWidth * hpRatio);
        boolean pulseFast = (gameTime % 10) < 5;
        int hpColor;
        if (wePhase == 3) {
            // 第三问【绯红狂暴】: 绯红脉冲
            hpColor = pulseFast ? 0xFFFF3B5C : 0xFFFF758F;
        } else if (hpRatio <= anchorRatio) {
            // 跌入锚点/红血临界区: 命运2警报红
            hpColor = pulseFast ? 0xFFE63946 : 0xFFC1121F;
        } else {
            // 常规状态: 命运2冷白微青
            hpColor = 0xFFEAF6FF;
        }
        if (hpFillWidth > 0) {
            graphics.fill(x0, y0, x0 + hpFillWidth, y0 + barHeight, hpColor);
            // 顶部 1px 高光边
            graphics.fill(x0, y0, x0 + hpFillWidth, y0 + 1, 0x55FFFFFF);
        }

        // 3) 延迟伤害池预扣段 (显示在当前生命条内侧右端或紧贴右侧, 直观对比剩余生命与延迟池)
        if (delayedPool > 0.05f) {
            float poolRatio = Mth.clamp(delayedPool / maxHp, 0.0f, 1.0f);
            int poolWidth = Math.max(2, Math.round(barWidth * poolRatio));
            int poolColor = (delayedPool >= hp) ? 0xDDFF2A55 : 0xDDFF8C42;
            // 画在主条下半部分 (3px 高), 从左端向右展示待结算池量与当前血量的直接水位对比
            graphics.fill(x0, y0 + barHeight - 2, x0 + Math.min(barWidth, poolWidth), y0 + barHeight, poolColor);
        }

        // 4) 第二问「记忆锚点」分界刻度竖线
        int splitX = x0 + Math.round(barWidth * anchorRatio);
        int splitColor = (wePhase == 2) ? COLOR_AZURE : 0xAA090D14;
        graphics.fill(splitX, y0 - 1, splitX + 1, y0 + barHeight + 1, splitColor);
        if (wePhase == 2) {
            // 记忆锚点已立时在刻度线顶端绘制一枚小菱形标记
            graphics.fill(splitX - 1, y0 - 3, splitX + 2, y0 - 1, COLOR_AZURE);
        }

        // 5) 上方超载护盾条 (Overshield —— 幸福护盾 / 金心)
        if (overshield > 0.05f) {
            float shieldRatio = Mth.clamp(overshield / maxHp, 0.0f, 1.0f);
            int shieldWidth = Math.max(2, Math.round(barWidth * shieldRatio));
            int osColor = hgFinaleSec > 0 ? COLOR_GOLD : COLOR_AZURE;
            graphics.fill(x0, y0 - 4, x0 + barWidth, y0 - 2, 0x66090D14);
            graphics.fill(x0, y0 - 4, x0 + shieldWidth, y0 - 2, osColor);
        }

        // 6) 条下居中微缩状态副标题
        Component bannerText;
        int bannerColor;
        if (wePhase == 3) {
            bannerText = Component.translatable("hud.tprt.destiny.top.phase_3", formatTime(berserkSec));
            bannerColor = COLOR_CRIMSON;
        } else if (wePhase == 2) {
            bannerText = delayedPool > 0.05f
                    ? Component.translatable("hud.tprt.destiny.top.phase_2_pool", String.format("%.1f", delayedPool))
                    : Component.translatable("hud.tprt.destiny.top.phase_2");
            bannerColor = COLOR_AZURE;
        } else if (worldEndLevel > 0) {
            bannerText = delayedPool > 0.05f
                    ? Component.translatable("hud.tprt.destiny.top.phase_1_pool", worldEndLevel, String.format("%.1f", delayedPool))
                    : Component.translatable("hud.tprt.destiny.top.phase_1", worldEndLevel);
            bannerColor = 0xFFD0E8FF;
        } else if (hgFinaleSec > 0) {
            bannerText = Component.translatable("hud.tprt.destiny.top.finale", formatTime(hgFinaleSec));
            bannerColor = COLOR_GOLD;
        } else if (overshield > 0.05f) {
            bannerText = Component.translatable("hud.tprt.destiny.top.overshield", String.format("%.1f", overshield));
            bannerColor = COLOR_AZURE;
        } else {
            return;
        }

        graphics.pose().pushPose();
        float scale = 0.85f;
        graphics.pose().scale(scale, scale, 1.0f);
        int textWidth = font.width(bannerText);
        int drawX = Math.round((screenWidth / 2.0f - (textWidth * scale) / 2.0f) / scale);
        int drawY = Math.round((y0 + barHeight + 3) / scale);
        graphics.drawString(font, bannerText, drawX, drawY, bannerColor, true);
        graphics.pose().popPose();
    }

    // ============================================================
    //  B. 屏幕左侧《命运2》纵向状态流 (Left-Side Status Feed)
    // ============================================================

    private static void renderLeftStatusFeed(GuiGraphics graphics, Font font, List<StatusEntry> entries, long gameTime, int screenHeight) {
        Set<String> currentKeys = new HashSet<>();
        for (StatusEntry entry : entries) {
            currentKeys.add(entry.key);
            ACTIVE_SINCE.putIfAbsent(entry.key, gameTime);
        }
        ACTIVE_SINCE.keySet().removeIf(k -> !currentKeys.contains(k));

        int baseX = 10;
        int rowHeight = 14;
        int rowGap = 2;
        int maxVisible = Math.min(entries.size(), 8);
        // 置于屏幕左侧偏中下 (经典《命运2》Buff 栏位置)
        int startY = Math.max(46, (screenHeight / 2) - (maxVisible * (rowHeight + rowGap)) / 2 + 12);

        for (int i = 0; i < maxVisible; i++) {
            StatusEntry entry = entries.get(i);
            int y = startY + i * (rowHeight + rowGap);

            long ageTicks = Math.max(0L, gameTime - ACTIVE_SINCE.getOrDefault(entry.key, gameTime));
            // 刚触发前 8 ticks 的《命运2》高光扫掠动画
            float flashAlpha = ageTicks < 8L ? (1.0f - ageTicks / 8.0f) : 0.0f;

            String timerStr = entry.remainingSeconds > 0 ? formatTime(entry.remainingSeconds) : null;
            String badgeStr = entry.badge != null && !entry.badge.isEmpty() ? entry.badge : null;

            int titleWidth = font.width(entry.title);
            int badgeWidth = badgeStr != null ? font.width(badgeStr) + 6 : 0;
            int timerWidth = timerStr != null ? font.width(timerStr) + 8 : 0;
            int totalWidth = Math.max(118, 18 + titleWidth + badgeWidth + timerWidth + 8);

            // 1) 半透明渐变底条与最左侧 2px 状态色竖条
            int bgColor = entry.category == Category.BURST ? 0xB01E0D16 : 0x980B1118;
            graphics.fill(baseX, y, baseX + totalWidth, y + rowHeight, bgColor);
            graphics.fill(baseX + totalWidth, y, baseX + totalWidth + 6, y + rowHeight, 0x380B1118);
            graphics.fill(baseX, y, baseX + 2, y + rowHeight, entry.accentColor);

            if (flashAlpha > 0.01f) {
                int flashAlphaInt = Mth.clamp(Math.round(flashAlpha * 110.0f), 0, 255);
                int flashOverlay = (flashAlphaInt << 24) | (entry.accentColor & 0x00FFFFFF);
                graphics.fill(baseX + 2, y, baseX + totalWidth, y + rowHeight, flashOverlay);
            }

            // 2) 底边 1px 微缩进度条 (心火比例/连击进度/剩余时间进度)
            if (entry.progressRatio > 0.0f) {
                int progW = Math.round((totalWidth - 2) * Mth.clamp(entry.progressRatio, 0.0f, 1.0f));
                if (progW > 0) {
                    graphics.fill(baseX + 2, y + rowHeight - 1, baseX + 2 + progW, y + rowHeight, entry.accentColor);
                }
            }

            // 3) 左侧《命运2》几何状态图标 (11x11)
            int iconX = baseX + 4;
            int iconY = y + 2;
            renderDestinyIcon(graphics, iconX, iconY, entry.category, entry.accentColor);

            // 4) 状态标题文字
            int textX = iconX + 13;
            int textY = y + 3;
            int titleColor = entry.category == Category.COOLDOWN ? 0xFFCBD5E1 : 0xFFF8FAFC;
            graphics.drawString(font, entry.title, textX, textY, titleColor, true);

            // 5) 层数 / 数值徽标
            int cursorX = textX + titleWidth + 4;
            if (badgeStr != null) {
                graphics.drawString(font, badgeStr, cursorX, textY, entry.accentColor, true);
            }

            // 6) 右端对齐的 0:XX 倒计时 (最后 3 秒跳红闪烁)
            if (timerStr != null) {
                int timeX = baseX + totalWidth - font.width(timerStr) - 4;
                boolean urgent = entry.remainingSeconds <= 3 && entry.category != Category.COOLDOWN;
                int timeColor = urgent
                        ? ((gameTime % 8 < 4) ? 0xFFFF4D6D : 0xFFFFB3C1)
                        : 0xFFE2E8F0;
                graphics.drawString(font, timerStr, timeX, textY, timeColor, true);
            }
        }
    }

    /** 绘制《命运2》风格几何图标 (菱形 ◆ / 爆发上三角 ▲ / 冷却下三角 ▼) */
    private static void renderDestinyIcon(GuiGraphics graphics, int x, int y, Category category, int color) {
        int darkBg = 0xCC070B10;
        graphics.fill(x, y, x + 10, y + 10, darkBg);
        // 外框微光
        int borderColor = (0x88 << 24) | (color & 0x00FFFFFF);
        graphics.fill(x, y, x + 10, y + 1, borderColor);
        graphics.fill(x, y + 9, x + 10, y + 10, borderColor);
        graphics.fill(x, y, x + 1, y + 10, borderColor);
        graphics.fill(x + 9, y, x + 10, y + 10, borderColor);

        int cx = x + 5;
        int cy = y + 5;
        switch (category) {
            case BURST -> {
                // 上三角 ▲ (爆发/觉醒)
                graphics.fill(cx - 1, cy - 3, cx + 1, cy - 2, color);
                graphics.fill(cx - 2, cy - 2, cx + 2, cy, color);
                graphics.fill(cx - 3, cy, cx + 3, cy + 2, color);
            }
            case BUFF -> {
                // 菱形 ◆ (常驻/增益/充能)
                graphics.fill(cx - 1, cy - 3, cx + 1, cy + 3, color);
                graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, color);
                graphics.fill(cx - 3, cy - 1, cx + 3, cy + 1, color);
            }
            case COOLDOWN -> {
                // 下三角 ▼ (冷却/调息/延迟池)
                graphics.fill(cx - 3, cy - 2, cx + 3, cy, color);
                graphics.fill(cx - 2, cy, cx + 2, cy + 2, color);
                graphics.fill(cx - 1, cy + 2, cx + 1, cy + 3, color);
            }
        }
    }

    // ============================================================
    //  C. 「挚爱 (chtholly)」5 大专属词条状态收集器
    // ============================================================

    private static void collectChthollyStatuses(Player player, long gameTime, List<StatusEntry> out) {
        // 1. 瑟尼欧里斯 (主/副手持有时)
        ItemStack snStack = findHeldStackWithModifier(player, ModifierIds.seniorious);
        if (!snStack.isEmpty()) {
            ToolStack snTool = ToolStack.from(snStack);
            IModDataView data = snTool.getPersistentData();
            String mode = data.getString(SN_MODE);
            boolean isCrimson = "crimson".equals(mode);
            float heartfire = data.getFloat(SN_HEARTFIRE);
            float hfRatio = Mth.clamp(heartfire / seniorious.HEARTFIRE_MAX, 0.0f, 1.0f);

            if (isCrimson) {
                int combo = data.getInt(SN_COMBO);
                out.add(new StatusEntry(
                        "sn_crimson",
                        Category.BURST,
                        Component.translatable("hud.tprt.destiny.sn_crimson"),
                        String.format("%d/%d · %.0f", combo, seniorious.COMBO_MAX, heartfire),
                        0,
                        Mth.clamp((float) combo / seniorious.COMBO_MAX, 0.05f, 1.0f),
                        COLOR_CRIMSON
                ));
            } else {
                out.add(new StatusEntry(
                        "sn_azure",
                        Category.BUFF,
                        Component.translatable("hud.tprt.destiny.sn_azure"),
                        String.format("%.0f/%.0f", heartfire, seniorious.HEARTFIRE_MAX),
                        0,
                        hfRatio,
                        COLOR_AZURE
                ));
            }

            // 调音掠夺层数与剩余秒数
            int stealStacks = data.getInt(SN_STEAL_STACKS);
            long stealUntil = parseLong(data.getString(SN_STEAL_UNTIL));
            if (stealStacks > 0 && stealUntil > gameTime && (stealUntil - gameTime) <= seniorious.STEAL_DURATION + 20L) {
                int remainSec = (int) ((stealUntil - gameTime + 19L) / 20L);
                float ratio = Mth.clamp((float) (stealUntil - gameTime) / seniorious.STEAL_DURATION, 0.0f, 1.0f);
                out.add(new StatusEntry(
                        "sn_steal",
                        Category.BUFF,
                        Component.translatable("hud.tprt.destiny.sn_steal"),
                        "x" + stealStacks,
                        remainSec,
                        ratio,
                        COLOR_AZURE
                ));
            }

            // 余温调息倒计时
            long restUntil = parseLong(data.getString(SN_REST_UNTIL));
            if (restUntil > gameTime && (restUntil - gameTime) <= seniorious.REST_DURATION + 20L) {
                int remainSec = (int) ((restUntil - gameTime + 19L) / 20L);
                int tuneCount = data.getInt(SN_TUNE_COUNT);
                float ratio = Mth.clamp((float) (restUntil - gameTime) / seniorious.REST_DURATION, 0.0f, 1.0f);
                out.add(new StatusEntry(
                        "sn_rest",
                        Category.COOLDOWN,
                        Component.translatable("hud.tprt.destiny.sn_rest"),
                        tuneCount + "/" + seniorious.TUNE_REQUIRED,
                        remainSec,
                        ratio,
                        COLOR_COOLDOWN
                ));
            }
        }

        // 2. 在消失之前，我想留在某个人心中 (主手/副手/快捷栏)
        ItemStack bvStack = findHotbarOrHandStackWithModifier(player, ModifierIds.before_i_vanish);
        if (!bvStack.isEmpty()) {
            ToolStack bvTool = ToolStack.from(bvStack);
            IModDataView data = bvTool.getPersistentData();
            int graceTicks = data.getInt(BV_VANISHING_TICKS);
            int cdTicks = data.getInt(BV_VANISHING_CD);
            boolean guardianActive = data.getBoolean(BV_GUARDIAN_ACTIVE);

            if (graceTicks > 0) {
                int hits = data.getInt(BV_VANISHING_HITS);
                int remainSec = (graceTicks + 19) / 20;
                float ratio = Mth.clamp((float) graceTicks / before_i_vanish.VANISHING_DURATION_TICKS, 0.0f, 1.0f);
                out.add(new StatusEntry(
                        "bv_grace",
                        Category.BURST,
                        Component.translatable("hud.tprt.destiny.bv_grace"),
                        hits + "/" + before_i_vanish.REFORGE_REQUIRED_HITS,
                        remainSec,
                        ratio,
                        COLOR_GOLD
                ));
            }
            if (guardianActive) {
                out.add(new StatusEntry(
                        "bv_guardian",
                        Category.BUFF,
                        Component.translatable("hud.tprt.destiny.bv_guardian"),
                        null,
                        0,
                        1.0f,
                        COLOR_AZURE
                ));
            }
            if (cdTicks > 0) {
                int cdSec = (cdTicks + 19) / 20;
                float ratio = Mth.clamp((float) cdTicks / before_i_vanish.VANISHING_COOLDOWN_TICKS, 0.0f, 1.0f);
                out.add(new StatusEntry(
                        "bv_cd",
                        Category.COOLDOWN,
                        Component.translatable("hud.tprt.destiny.bv_cd"),
                        null,
                        cdSec,
                        ratio,
                        COLOR_COOLDOWN
                ));
            }
        }

        // 3. 末日时在做什么？有没有空？可以来拯救吗？ (护甲)
        int weLevel = what_do_you_do_at_the_end_of_the_world.getTotalLevel(player);
        IModDataView weData = findFirstArmorModData(player);
        if (weLevel > 0 && weData != null) {
            int phase = Math.max(1, weData.getInt(WE_PHASE));
            float pool = weData.getInt(WE_POOL_TENTHS) / 10.0f;
            int berserkSec = weData.getInt(WE_BERSERK_SEC);
            int cdSec = weData.getInt(WE_CD_SEC);

            if (phase == 3 && berserkSec > 0) {
                float totalSec = what_do_you_do_at_the_end_of_the_world.getBerserkDurationTicks(weLevel) / 20.0f;
                out.add(new StatusEntry(
                        "we_berserk",
                        Category.BURST,
                        Component.translatable("hud.tprt.destiny.we_berserk"),
                        pool > 0.05f ? String.format("%.1f", pool) : "Lv." + weLevel,
                        berserkSec,
                        Mth.clamp(berserkSec / Math.max(1.0f, totalSec), 0.0f, 1.0f),
                        COLOR_CRIMSON
                ));
            } else if (phase == 2) {
                out.add(new StatusEntry(
                        "we_anchor",
                        Category.BUFF,
                        Component.translatable("hud.tprt.destiny.we_anchor"),
                        "Lv." + weLevel,
                        0,
                        1.0f,
                        COLOR_AZURE
                ));
            }

            if (pool > 0.05f && phase != 3) {
                float ratio = Mth.clamp(pool / Math.max(1.0f, player.getMaxHealth()), 0.05f, 1.0f);
                out.add(new StatusEntry(
                        "we_pool",
                        Category.COOLDOWN,
                        Component.translatable("hud.tprt.destiny.we_pool"),
                        String.format("%.1f", pool),
                        0,
                        ratio,
                        COLOR_POOL_AMBER
                ));
            }

            if (cdSec > 0) {
                float maxCd = what_do_you_do_at_the_end_of_the_world.getRewindCooldownTicks(weLevel) / 20.0f;
                out.add(new StatusEntry(
                        "we_cd",
                        Category.COOLDOWN,
                        Component.translatable("hud.tprt.destiny.we_cd"),
                        null,
                        cdSec,
                        Mth.clamp(cdSec / Math.max(1.0f, maxCd), 0.0f, 1.0f),
                        COLOR_COOLDOWN
                ));
            }
        }

        // 4. 现在的我，不管别人怎么说，都一定是世界上最幸福的女孩 (护符 Curios)
        IModDataView hgData = findCharmModData(player);
        if (hgData != null) {
            float shield = hgData.getInt(HG_SHIELD_TENTHS) / 10.0f;
            float peak = hgData.getInt(HG_PEAK_TENTHS) / 10.0f;
            int finaleSec = hgData.getInt(HG_FINALE_SEC);
            int cdSec = hgData.getInt(HG_CD_SEC);

            if (finaleSec > 0) {
                float totalSec = happiest_girl_in_the_world.FINALE_DURATION_TICKS / 20.0f;
                out.add(new StatusEntry(
                        "hg_finale",
                        Category.BURST,
                        Component.translatable("hud.tprt.destiny.hg_finale"),
                        peak > 0.05f ? String.format("%.1f", peak) : null,
                        finaleSec,
                        Mth.clamp(finaleSec / totalSec, 0.0f, 1.0f),
                        COLOR_GOLD
                ));
            } else if (shield > 0.05f) {
                float ratio = Mth.clamp(shield / Math.max(1.0f, player.getMaxHealth()), 0.05f, 1.0f);
                out.add(new StatusEntry(
                        "hg_shield",
                        Category.BUFF,
                        Component.translatable("hud.tprt.destiny.hg_shield"),
                        String.format("%.1f", shield),
                        0,
                        ratio,
                        COLOR_AZURE
                ));
            }

            if (cdSec > 0) {
                float maxCd = happiest_girl_in_the_world.FINALE_COOLDOWN_TICKS / 20.0f;
                out.add(new StatusEntry(
                        "hg_cd",
                        Category.COOLDOWN,
                        Component.translatable("hud.tprt.destiny.hg_cd"),
                        null,
                        cdSec,
                        Mth.clamp(cdSec / maxCd, 0.0f, 1.0f),
                        COLOR_COOLDOWN
                ));
            }
        }

        // 5. 终末挽歌 (法术书 Curios / 主副手)
        IModDataView elData = findElegyModData(player);
        if (elData != null) {
            int activeTargets = elData.getInt(EL_ACTIVE_TARGETS);
            float maxStored = elData.getInt(EL_MAX_STORED_TENTHS) / 10.0f;
            int remainSec = elData.getInt(EL_REMAIN_SEC);
            if (activeTargets > 0 && maxStored > 0.05f) {
                float totalSec = elegy_of_world_end.ECHO_DURATION_TICKS / 20.0f;
                out.add(new StatusEntry(
                        "el_echo",
                        Category.BURST,
                        Component.translatable("hud.tprt.destiny.el_echo"),
                        String.format("x%d · %.1f", activeTargets, maxStored),
                        Math.max(1, remainSec),
                        Mth.clamp(remainSec / totalSec, 0.1f, 1.0f),
                        COLOR_CRIMSON
                ));
            }
        }

        // 6. 纵使日薄西山，即便看不到未来 (远程弓弩: 守望之矢 / 不归路标 / 斜阳御剑)
        ItemStack bowStack = findHotbarOrHandStackWithModifier(player, ModifierIds.even_as_the_sun_sets);
        if (!bowStack.isEmpty()) {
            ToolStack bowTool = ToolStack.from(bowStack);
            IModDataView bowData = bowTool.getPersistentData();
            int hoverCount = bowData.getInt(even_as_the_sun_sets.SYNC_HOVER_COUNT);
            int hoverSec = bowData.getInt(even_as_the_sun_sets.SYNC_HOVER_SEC);
            int waymarkCount = bowData.getInt(even_as_the_sun_sets.SYNC_WAYMARK_COUNT);
            int waymarkSec = bowData.getInt(even_as_the_sun_sets.SYNC_WAYMARK_SEC);
            String linkedSword = bowData.getString(even_as_the_sun_sets.SYNC_LINKED_SWORD);

            if (hoverCount > 0) {
                out.add(new StatusEntry(
                        "ss_hover",
                        Category.BUFF,
                        Component.translatable("hud.tprt.destiny.ss_hover"),
                        "x" + hoverCount + "/" + even_as_the_sun_sets.MAX_HOVER_ARROWS,
                        hoverSec,
                        Mth.clamp((float) hoverCount / even_as_the_sun_sets.MAX_HOVER_ARROWS, 0.1f, 1.0f),
                        COLOR_AZURE
                ));
            }

            if (waymarkCount > 0) {
                boolean nearAutoRecall = waymarkCount >= even_as_the_sun_sets.AUTO_RECALL_THRESHOLD - 1;
                out.add(new StatusEntry(
                        "ss_waymark",
                        nearAutoRecall ? Category.BURST : Category.BUFF,
                        Component.translatable("hud.tprt.destiny.ss_waymark"),
                        "x" + waymarkCount + "/" + even_as_the_sun_sets.AUTO_RECALL_THRESHOLD,
                        waymarkSec,
                        Mth.clamp((float) waymarkCount / even_as_the_sun_sets.AUTO_RECALL_THRESHOLD, 0.1f, 1.0f),
                        nearAutoRecall ? COLOR_CRIMSON : COLOR_GOLD
                ));
            }

            boolean holdingBow = !findHeldStackWithModifier(player, ModifierIds.even_as_the_sun_sets).isEmpty();
            if (holdingBow && !linkedSword.isEmpty()) {
                String badge = linkedSword.length() > 8 ? linkedSword.substring(0, 8) + "…" : linkedSword;
                out.add(new StatusEntry(
                        "ss_sword",
                        Category.BUFF,
                        Component.translatable("hud.tprt.destiny.ss_sword"),
                        badge,
                        0,
                        1.0f,
                        COLOR_GOLD
                ));
            }
        }
    }

    // ============================================================
    //  工具方法: 槽位扫描与格式化
    // ============================================================

    private static ItemStack findHeldStackWithModifier(Player player, slimeknights.tconstruct.library.modifiers.ModifierId id) {
        ItemStack main = player.getMainHandItem();
        if (hasModifier(main, id)) return main;
        ItemStack off = player.getOffhandItem();
        if (hasModifier(off, id)) return off;
        return ItemStack.EMPTY;
    }

    private static ItemStack findHotbarOrHandStackWithModifier(Player player, slimeknights.tconstruct.library.modifiers.ModifierId id) {
        ItemStack held = findHeldStackWithModifier(player, id);
        if (!held.isEmpty()) return held;
        Inventory inv = player.getInventory();
        for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (hasModifier(stack, id)) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    private static IModDataView findFirstArmorModData(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (hasModifier(stack, ModifierIds.what_do_you_do_at_the_end_of_the_world)) {
                return ToolStack.from(stack).getPersistentData();
            }
        }
        ItemStack off = player.getOffhandItem();
        if (hasModifier(off, ModifierIds.what_do_you_do_at_the_end_of_the_world)) {
            ToolStack tool = ToolStack.from(off);
            if (tool.hasTag(TinkerTags.Items.ARMOR)) {
                return tool.getPersistentData();
            }
        }
        return null;
    }

    @Nullable
    private static IModDataView findCharmModData(Player player) {
        var invOpt = CuriosApi.getCuriosInventory(player).resolve();
        if (invOpt.isEmpty()) return null;
        for (var entry : invOpt.get().getCurios().entrySet()) {
            var stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (hasModifier(stack, ModifierIds.happiest_girl_in_the_world)) {
                    return ToolStack.from(stack).getPersistentData();
                }
            }
        }
        return null;
    }

    @Nullable
    private static IModDataView findElegyModData(Player player) {
        ItemStack held = findHeldStackWithModifier(player, ModifierIds.elegy_of_world_end);
        if (!held.isEmpty()) {
            return ToolStack.from(held).getPersistentData();
        }
        var invOpt = CuriosApi.getCuriosInventory(player).resolve();
        if (invOpt.isEmpty()) return null;
        for (var entry : invOpt.get().getCurios().entrySet()) {
            var stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (hasModifier(stack, ModifierIds.elegy_of_world_end)) {
                    return ToolStack.from(stack).getPersistentData();
                }
            }
        }
        return null;
    }

    private static boolean hasModifier(ItemStack stack, slimeknights.tconstruct.library.modifiers.ModifierId id) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IModifiable) || !ToolStack.isInitialized(stack)) {
            return false;
        }
        ToolStack tool = ToolStack.from(stack);
        return tool.getModifierLevel(id) > 0;
    }

    private static long parseLong(String raw) {
        if (raw == null || raw.isEmpty()) return 0L;
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String formatTime(int totalSeconds) {
        int sec = Math.max(0, totalSeconds);
        return String.format("%d:%02d", sec / 60, sec % 60);
    }
}
