package org.mylove.tprt.compat.IronsSpellBooks.entity;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
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