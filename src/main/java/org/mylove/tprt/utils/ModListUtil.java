package org.mylove.tprt.utils;

import net.minecraftforge.fml.ModList;

public class ModListUtil {
    public static class modName{
        public static String ISS = "irons_spellbooks";
    }
    public static boolean ISSLoaded = ModList.get().isLoaded(modName.ISS);
}
