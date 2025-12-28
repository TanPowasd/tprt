package org.mylove.tprt.utils;

import net.minecraftforge.fml.ModList;

public class ModListUtil {
    public static class modName{
        public static String ISS = "irons_spellbooks";
        public static String ID = "immortalers_delight";
        public static String Goety = "goety";
        public static String Cataclysm = "cataclysm";
        public static String Iceandfire = "iceandfire";
        public static String Cloudertinker = "cloudertinker";
    }
    public static boolean ISSLoaded = ModList.get().isLoaded(modName.ISS);
    public static boolean IDLoaded = ModList.get().isLoaded(modName.ID);
    public static boolean GTLoaded = ModList.get().isLoaded(modName.Goety);
    public static boolean CataclysmLoaded = ModList.get().isLoaded(modName.Cataclysm);
    public static boolean IceandfireLoaded = ModList.get().isLoaded(modName.Iceandfire);
    public static boolean CloudertinkerLoaded = ModList.get().isLoaded(modName.Cloudertinker);
}
