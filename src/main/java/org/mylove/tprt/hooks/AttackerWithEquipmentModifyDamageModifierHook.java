package org.mylove.tprt.hooks;

import net.minecraft.world.entity.LivingEntity;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import java.util.Collection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import slimeknights.tconstruct.library.module.ModuleHook;
import slimeknights.tconstruct.library.tools.context.EquipmentContext;
import slimeknights.tconstruct.library.tools.helper.ModifierUtil;

public interface AttackerWithEquipmentModifyDamageModifierHook {
    void attackermodifyDamageTaken(IToolStackView var1, ModifierEntry var2, LivingEntity var3, EquipmentContext var4, EquipmentSlot var5, DamageSource var6, float var7, DamageModifier var8, boolean var9);

    static void attackermodifyDamageTaken(ModuleHook<AttackerWithEquipmentModifyDamageModifierHook> hook, LivingEntity target, EquipmentContext context, DamageSource source, float baseamount, DamageModifier damageModifier, boolean isDirectDamage) {
        for(EquipmentSlot slotType : EquipmentSlot.values()) {
            IToolStackView toolStack = context.getToolInSlot(slotType);
            if (toolStack != null && !toolStack.isBroken() && ModifierUtil.validArmorSlot(toolStack, slotType)) {
                for(ModifierEntry entry : toolStack.getModifierList()) {
                    ((AttackerWithEquipmentModifyDamageModifierHook)entry.getHook(hook)).attackermodifyDamageTaken(toolStack, entry, target, context, slotType, source, baseamount, damageModifier, isDirectDamage);
                }
            }
        }

    }

    public static record AllMerger(Collection<AttackerWithEquipmentModifyDamageModifierHook> modules) implements AttackerWithEquipmentModifyDamageModifierHook {
        public void attackermodifyDamageTaken(IToolStackView tool, ModifierEntry modifier, LivingEntity target, EquipmentContext context, EquipmentSlot slotType, DamageSource source, float baseamount, DamageModifier damageModifier, boolean isDirectDamage) {
            for(AttackerWithEquipmentModifyDamageModifierHook module : this.modules) {
                module.attackermodifyDamageTaken(tool, modifier, target, context, slotType, source, baseamount, damageModifier, isDirectDamage);
            }

        }
    }

    public static class DamageModifier {
        float baseamount;
        float add;
        float percent;
        float multiply;
        float fixed;

        public float getBaseamount() {
            return this.baseamount;
        }

        public float getAdd() {
            return this.add;
        }

        public float getPercent() {
            return this.percent;
        }

        public float getMultiply() {
            return this.multiply;
        }

        public float getFixed() {
            return this.fixed;
        }

        public float getamount() {
            return (this.baseamount + this.add) * (1.0F + this.percent) * this.multiply + this.fixed;
        }

        public DamageModifier(float baseamount) {
            this.baseamount = baseamount;
            this.add = 0.0F;
            this.percent = 0.0F;
            this.multiply = 1.0F;
            this.fixed = 0.0F;
        }

        public DamageModifier(DamageModifier other) {
            this.baseamount = other.baseamount;
            this.add = other.add;
            this.percent = other.percent;
            this.multiply = other.multiply;
            this.fixed = other.fixed;
        }

        public void addAdd(float add) {
            this.add += add;
        }

        public void addPercent(float percent) {
            this.percent += percent;
        }

        public void addMultiply(float multiply) {
            this.multiply *= multiply;
        }

        public void addFixed(float fixed) {
            this.fixed += fixed;
        }

        public void setBaseamount(float baseamount) {
            this.baseamount = baseamount;
        }

        public void setAdd(float add) {
            this.add = add;
        }

        public void setPercent(float percent) {
            this.percent = percent;
        }

        public void setMultiply(float multiply) {
            this.multiply = multiply;
        }

        public void setFixed(float fixed) {
            this.fixed = fixed;
        }
    }
}
