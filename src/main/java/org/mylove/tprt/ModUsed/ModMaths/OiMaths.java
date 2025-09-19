package org.mylove.tprt.ModUsed.ModMaths;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class OiMaths {
    public class ENCRYPTION{

    }
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
    public class OPTIMIZE{
        public static long QuickPow(long a, long b, long mod) {
            //快速幂算法
            long res=1;
            while (b>0) {
                if ((b&1)==1) {
                    res=(res*a)%mod;
                }
                a=(a*a)%mod;
                b>>=1;
            }
            return res;
        }
        public static int __GCD(int a, int b) {
            // 辗转相除法求最大公约数
            return b == 0 ? a : __GCD(b, a % b);
        }
        public static int euler_phi(int n){
            //欧拉函数
            int res=n;
            for(int i=2;i*i<=n;i++){
                if(n%i==0){
                    res=res/i*(i-1);
                    while(n%i==0){
                        n/=i;
                    }
                }
            }
            if(n>1){
                res=res/n*(n-1);
            }
            return res;
        }
    }
    public class Mat{
        public static long[][] MatMatrix=new long[10][10];
        public static long[][] MatrixMul_2(long[][] a,long[][] b){
            long[][] ans=new long[10][10];
            for(int i=0;i<10;i++){
                for(int j=0;j<10;j++){
                    ans[i][j]=0;
                }
            }
            for(int i=0;i<2;i++){
                for(int j=0;j<2;j++){
                    for(int k=0;k<2;k++){
                        ans[i][j]+= (long) (a[i][k]*b[k][j]%(1e9+7));
                    }
                }
            }
            return ans;
        }
        public static long Fibonacci(long n){
            if(n==0){
                return 0;
            }
            if(n==1){
                return 1;
            }
            long[][] ans=new long[2][2];
            ans[0][0]=1;
            ans[0][1]=1;
            ans[1][0]=1;
            ans[1][1]=0;
            long[][] res=new long[2][2];
            res[0][0]=0;
            res[0][1]=0;
            res[1][0]=0;
            res[1][1]=0;
            n-=2;
            while(n>0){
                if((n&1)==1){
                    res=MatrixMul_2(res,ans);
                }
                ans=MatrixMul_2(ans,ans);
                n>>=1;
            }
            return (long) ((res[0][1])%(1e9+7));
        }
    }
    public class RANDOM{
        public static long TimeRandom(){
            //时间做种子做随机数
            long Time= System.currentTimeMillis();//以当前时间为种子
            return Time;//尚不完善
        }
        public static long IntRandom(long l,long r){
            return TimeRandom()%(r-l+1)+l;
        }
    }
}
