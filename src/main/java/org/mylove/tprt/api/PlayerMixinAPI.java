package org.mylove.tprt.api;

import org.mylove.tprt.api.flying_sword.FlyingSwordManager;
import org.mylove.tprt.entities.flying_sword.FlyingSword;

public interface PlayerMixinAPI {
    // it's public static final, we don't want static
    // FlyingSword[] nineSword$HotbarHolder = new FlyingSword[9];

    // still not work
    // FlyingSword[] getNineSword$HotbarHolder();

    // done
    FlyingSwordManager getFlyingSwordManager();
}
