package org.mylove.tprt.tags.modifier;

import slimeknights.tconstruct.library.modifiers.ModifierHooks;
import slimeknights.tconstruct.library.modifiers.hook.interaction.UsingToolModifierHook;
import slimeknights.tconstruct.library.modifiers.impl.SingleLevelModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;

public class nine_sword extends SingleLevelModifier implements UsingToolModifierHook {
    @Override
    protected void registerHooks(ModuleHookMap.Builder hookBuilder) {
        hookBuilder.addHook(this, ModifierHooks.TOOL_USING);
    }
}
