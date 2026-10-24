package org.mylove.tprt.compat.IronsSpellBooks.entity;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.minecraft.resources.ResourceLocation;

public class EchoCasterModel extends AbstractSpellCastingMobModel {
    private static final ResourceLocation TEXTURE = AbstractSpellCastingMob.textureResource;

    public static ResourceLocation texture() {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getTextureResource(AbstractSpellCastingMob object) {
        return TEXTURE;
    }
}