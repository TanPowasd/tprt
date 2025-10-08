package org.mylove.tprt.hooks;

import java.util.Collection;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.tools.context.ToolAttackContext;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;

public interface MeleeDamagePercentModifierHook {
    default void getMeleeDamageModifier(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage, DamageModifier damagemodifier) {
    }

    public static record AllMerger(Collection<MeleeDamagePercentModifierHook> modules) implements MeleeDamagePercentModifierHook {
        public void getMeleeDamageModifier(IToolStackView tool, ModifierEntry modifier, ToolAttackContext context, float baseDamage, float damage, DamageModifier damagemodifier) {
            for(MeleeDamagePercentModifierHook module : this.modules) {
                module.getMeleeDamageModifier(tool, modifier, context, baseDamage, damage, damagemodifier);
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

        public DamageModifier(AttackerWithEquipmentModifyDamageModifierHook.DamageModifier other) {
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

