package org.mylove.tprt.tags.modifier;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.mylove.tprt.entities.flying_sword.FlyingSword;
import org.mylove.tprt.utilities.Abbr;
import org.mylove.tprt.utilities.DeBug;
import slimeknights.tconstruct.library.modifiers.Modifier;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.armor.EquipmentChangeModifierHook;
import slimeknights.tconstruct.library.modifiers.hook.build.ModifierRemovalHook;
import slimeknights.tconstruct.library.modifiers.hook.interaction.InventoryTickModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.context.EquipmentChangeContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

import javax.annotation.Nullable;

public class flying_sword_tag extends SingleLevelModifier implements
        EquipmentChangeModifierHook, InventoryTickModifierHook, ModifierRemovalHook
{
    // 此类为所有强化所共有，不要在这里加特指某个工具的数据
    // private FlyingSword flyingSwordEntity;
    // private UUID swordUUID;

    // ResourceLocation仅可包含: a-z0-9/.-_
    /** tinker工具上保存飞剑ID的key */
    public static final ResourceLocation PERSISTENT_UUID_KEY = ResourceLocation.parse("flying-sword-uuid-key");
    public static final ResourceLocation PERSISTENT_SLOT = ResourceLocation.parse("flying-sword-slot");

    // 生成飞剑，一个工具对应的飞剑应当是唯一的
    private String generateFlyingSword(IToolStackView tool, Level level, Player player, int itemSlot, ItemStack stack, @Nullable String uuid){
        FlyingSword flyingSword = new FlyingSword(tool, level, player, itemSlot, stack);

        level.addFreshEntity(flyingSword);
        DeBug.Console(player, "生成飞剑: isClientSide = "+level.isClientSide+ itemSlot+"号\n"+stack.toString());
        return flyingSword.getStringUUID();
    }

    private void degenerateFlyingSword(){

    }

    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.EQUIPMENT_CHANGE, ModifierHooks.INVENTORY_TICK, ModifierHooks.REMOVE);
    }

    @Override
    public void onEquip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
//        degenerateFlyingSword(tool, context);
    }

    @Override
    public void onUnequip(IToolStackView tool, ModifierEntry modifier, EquipmentChangeContext context) {
//        generateFlyingSword(tool, context);
    }

    @Override
    public void onInventoryTick(IToolStackView tool, ModifierEntry modifier, Level level, LivingEntity holder, int itemSlot, boolean isSelected, boolean isCorrectSlot, ItemStack stack) {
        if(!(holder instanceof Player player)) return;
        // todo: 处理位于副手的情况 08/21
        // if(itemSlot==0 && player.getOffhandItem().equals(tool)) return;


        // DeBug.Console(player, uuid);
//        if(uuid.isEmpty()){
//            // 服务端判断（空） -> 覆写uuid-NBT -> 客户端判断（有） -> 不生成飞剑
//            String uuid1 = generateFlyingSword(tool, level, player, itemSlot, stack, null);
//            tool.getPersistentData().putString(PERSISTENT_UUID_KEY, uuid1);
//        } else {
//            // 没有找到工具离开物品栏的钩子，因此逻辑改为飞剑需定期判定工具存在以续命
//            // level.getEntity(UUID.fromString(uuid));
//        }

        if(level.isClientSide) return; // uuid生成有随机性，为了避免两端不同步问题只用服务端创实体，然后通过某种神秘的minecraft力量同步
        if(player.tickCount % FlyingSword.maxLifetime != 19) return; // 此处有魔法数19
        // 将生成的飞剑实体id存到工具本身上, 再存一个当前槽位方便飞剑那边访问
        String uuid =  tool.getPersistentData().getString(PERSISTENT_UUID_KEY);
        int slot0 = tool.getPersistentData().getInt(PERSISTENT_SLOT);
        if(slot0 != itemSlot) tool.getPersistentData().putInt(PERSISTENT_SLOT, itemSlot);

        if(!Inventory.isHotbarSlot(itemSlot)) return; // 只有位于快捷栏且不在副手时生成飞剑
        FlyingSword flyingSword = Abbr.getSword(player, itemSlot);
        if(flyingSword == null){
            String uuid1 = generateFlyingSword(tool, level, player, itemSlot, stack, null);
            tool.getPersistentData().putString(PERSISTENT_UUID_KEY, uuid1);
        } else if (uuid.isEmpty() || !uuid.equals(flyingSword.getStringUUID()) || itemSlot != flyingSword.getSlotNumber()) {
            flyingSword.setToDiscard("in list but no uuid on tool || uuid not match");
            String uuid1 = generateFlyingSword(tool, level, player, itemSlot, stack, null);
            tool.getPersistentData().putString(PERSISTENT_UUID_KEY, uuid1);
        } else {
            // 完全匹配, 你的剑就是我的剑

        }

        // if(stack!=null && player.tickCount % 20 == 0) DeBug.Console(player, stack.toString());
        // DeBug.Console(player, "客户端："+level.isClientSide);
    }

    @Override
    public @Nullable Component onRemoved(IToolStackView tool, Modifier modifier) {
        tool.getPersistentData().remove(PERSISTENT_UUID_KEY);
        tool.getPersistentData().remove(PERSISTENT_SLOT);
        return null;
    }
}
