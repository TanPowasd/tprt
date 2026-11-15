package org.mylove.tprt.compat.IronsSpellBooks.entity;

import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

/** EchoCaster 的渲染器: 复用铁魔法的施法生物渲染基类, 实体本身不可见 */
@OnlyIn(Dist.CLIENT)
public class EchoCasterRenderer extends AbstractSpellCastingMobRenderer {
    public EchoCasterRenderer(EntityRendererProvider.Context context) {
        super(context, new EchoCasterModel());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AbstractSpellCastingMob entity) {
        return EchoCasterModel.texture();
    }
}
