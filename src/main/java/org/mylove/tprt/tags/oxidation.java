package org.mylove.tprt.tags;

import com.ssakura49.sakuratinker.generic.BaseModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.mylove.tprt.Tprt;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public class oxidation extends BaseModifier {
    public boolean isNoLevels() {
        return true;
    }
    int ticks=0;
    ResourceLocation oxg_in_tprt_modifier_oxidation=ResourceLocation.fromNamespaceAndPath(Tprt.MODID,"oxg_in_tprt_modifier_oxidation");
    @Override
    public void modifierOnInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack itemStack) {
        int oxgintprt_modifier_oxidation =tool.getPersistentData().getInt(oxg_in_tprt_modifier_oxidation);
        int maxDurable=tool.getCurrentDurability();
        int nowDurable=0;
        int CFf=maxDurable-nowDurable;
        //氧化值最多是以损失耐久的1/3
        if(CFf/3< oxgintprt_modifier_oxidation){
            oxgintprt_modifier_oxidation =CFf/3;
        }
        ticks++;
        if(ticks%60==0){
            oxgintprt_modifier_oxidation++;
            ticks=0;
        }
        tool.getPersistentData().putInt(oxg_in_tprt_modifier_oxidation,oxgintprt_modifier_oxidation);
    }
}
