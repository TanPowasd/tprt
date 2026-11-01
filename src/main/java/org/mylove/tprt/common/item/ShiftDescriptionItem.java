package org.mylove.tprt.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 带"按住 Shift 查看详情"的物品。
 *
 * 未按住 Shift 时只显示一行灰色提示；按住后依次显示描述与额外说明。
 * 参考魂樱（sakuratinker）SimpleDescriptiveItem 的交互方式。
 */
public class ShiftDescriptionItem extends Item {

    /** 未按住 Shift 时的提示键 */
    public static final String HOLD_SHIFT_KEY = "tooltip.tprt.hold_shift";

    private final boolean foil;
    private final String descKey;
    private final String[] lineKeys;

    public ShiftDescriptionItem(Properties properties, boolean foil, String descKey, String... lineKeys) {
        super(properties);
        this.foil = foil;
        this.descKey = descKey;
        this.lineKeys = lineKeys;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // 只在客户端追加
        if (level != null && !level.isClientSide) {
            return;
        }
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable(descKey).withStyle(ChatFormatting.GRAY));
            for (String key : lineKeys) {
                tooltip.add(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE));
            }
        } else if (!descKey.isEmpty() || lineKeys.length > 0) {
            tooltip.add(Component.translatable(HOLD_SHIFT_KEY).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
