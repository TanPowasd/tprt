package org.mylove.tprt.compat.Cloudertinker;

import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.Cloudertinker.Modifiers.Curios.Body_like_glass;
import org.mylove.tprt.compat.Cloudertinker.Modifiers.Curios.Fragile;
import org.mylove.tprt.compat.Cloudertinker.Modifiers.Curios.Unreal_Image;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class CTCompat {
    public static ModifierDeferredRegister CT_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);

    public static final StaticModifier<Body_like_glass> Body_like_glass;
    public static final StaticModifier<Unreal_Image> Unreal_Image;
    public static final StaticModifier<Fragile> Fragile;
    static {
        Body_like_glass = CT_MODIFIERS.register("body_like_glass", Body_like_glass::new);
        Unreal_Image = CT_MODIFIERS.register("unreal_image", Unreal_Image::new);
        Fragile = CT_MODIFIERS.register("fragile", Fragile::new);
    }
}
