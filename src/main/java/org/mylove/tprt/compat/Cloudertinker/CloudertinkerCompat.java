package org.mylove.tprt.compat.Cloudertinker;

import com.ssakura49.sakuratinker.generic.CurioModifier;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.Cloudertinker.Modifiers.Curios.Body_like_glass;
import org.mylove.tprt.compat.Cloudertinker.Modifiers.Curios.Unreal_Image;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;

public class CloudertinkerCompat {
    public static ModifierDeferredRegister Cloudertinker_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);

    public static final StaticModifier<CurioModifier> Body_like_glass;
    public static final StaticModifier<CurioModifier> Unreal_Image;
    static {
        Body_like_glass = Cloudertinker_MODIFIERS.register("the_dragon_lord", Body_like_glass::new);
        Unreal_Image = Cloudertinker_MODIFIERS.register("the_dragon_power", Unreal_Image::new);
    }
}
