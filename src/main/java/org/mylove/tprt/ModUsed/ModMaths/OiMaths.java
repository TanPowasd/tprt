package org.mylove.tprt.ModUsed.ModMaths;

import java.util.ArrayList;
import java.util.Arrays;

public class OiMaths {
    public final static int MAXR = (int) (1e9+7);
    public static int PrimeNum=0;
    public static ArrayList prime= new ArrayList<>();
    public static boolean[] isprime;
    public static boolean isInit=false;
    public static void InitPrime(){
        if(isInit==true){
            return;
        }
        isInit=true;
        Arrays.fill(isprime,false);

    }
}
