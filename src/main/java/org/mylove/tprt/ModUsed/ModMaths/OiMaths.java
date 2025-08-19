package org.mylove.tprt.ModUsed.ModMaths;

import java.util.ArrayList;
import java.util.Arrays;

public class OiMaths {

    public class PRIME{
        public final static int MAXR = (int) (1e7+7);
        public static int PrimeNum=0;
        public static ArrayList prime= new ArrayList<>();
        public static boolean[] isprime=new boolean[MAXR+1];
        public static boolean isInit=false;
        public static void InitPrime(){
            if(isInit==true){
                return;
            }
            isInit=true;
            Arrays.fill(isprime,true);
            isprime[1]=false;
            for(int i=2;i<=MAXR;++i){
                if(isprime[i]){
                    prime.add(i);
                    PrimeNum++;
                    for(int j=1;j<=PrimeNum&& i*(int) prime.get(j-1)<=MAXR;++j){
                        isprime[i* (int) prime.get(j-1)]=false;
                        if(i% (int) prime.get(j-1)==0){
                            break;
                        }
                    }
                }
            }
        }
        public static boolean isPrime(int n){
            InitPrime();
            return isprime[n];
        }
        public static int getPrime(int n){
            InitPrime();
            if(n<1||n>PrimeNum){
                return -1;
            }
            return (int) prime.get(n-1);
        }
        public static int biggerPrime(int n){
            InitPrime();
            if(n<1){
                return 2;
            }
            for(int i=0;i<PrimeNum;++i){
                if((int) prime.get(i)>n){
                    return (int) prime.get(i);
                }
            }
            return -1;
        }
        public static int smallerPrime(int n){
            InitPrime();
            if(n<1){
                return -1;
            }
            for(int i=PrimeNum-1;i>=0;--i){
                if((int) prime.get(i)<n){
                    return (int) prime.get(i);
                }
            }
            return -1;
        }
    }

}
