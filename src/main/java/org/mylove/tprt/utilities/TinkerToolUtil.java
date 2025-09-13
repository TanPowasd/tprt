package org.mylove.tprt.utilities;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import slimeknights.tconstruct.common.TinkerTags;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.item.IModifiable;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

public class TinkerToolUtil {
    public static boolean checkTool(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.is(TinkerTags.Items.MODIFIABLE)) {
            Item var2 = stack.getItem();
            if (var2 instanceof IModifiable modifiable) {
                return modifiable.getToolDefinition() != ToolDefinition.EMPTY;
            }
        }
        return false;
    }

    @Nullable
    public static ToolStack getTool(ItemStack stack) {
        if (checkTool(stack)) {
            return ToolStack.from(stack);
        }
        return null;
    }

    @Nullable
    public static ToolStack getToolInHand(LivingEntity entity) {
        ItemStack offItem = entity.getOffhandItem();
        ItemStack mainItem = entity.getMainHandItem();
        ItemStack stack = mainItem.isEmpty() ? (offItem.isEmpty() ? null : offItem) : mainItem;
        return stack != null ? getTool(stack) : null;
    }

    public static boolean isNotBrokenOrNull(IToolStackView tool) {
        return tool != null && !tool.isBroken();
    }
}
