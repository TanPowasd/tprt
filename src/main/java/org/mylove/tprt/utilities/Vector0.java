package org.mylove.tprt.utilities;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Vector0 {
    /// 返回使向量A与向量B重合所需的 xRot 和 yRot 角度
    public static Rot2 getRotateFromVec3(Vec3 from, Vec3 to){
        Vec3 d;
        d = from.vectorTo(to);

        return new Rot2(Vec3.ZERO);

//        double d0 = pTarget.x - vec3.x;
//        double d1 = pTarget.y - vec3.y;
//        double d2 = pTarget.z - vec3.z;
//        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
//        this.setXRot(Mth.wrapDegrees((float)(-(Mth.atan2(d1, d3) * (double)(180F / (float)Math.PI)))));
//        this.setYRot(Mth.wrapDegrees((float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F));
//        this.setYHeadRot(this.getYRot());
//        this.xRotO = this.getXRot();
//        this.yRotO = this.getYRot();
    }

    /// 返回从a到b的θ角(弧度) #实际上是夹角, 没有方向
    public static double getTheta(Vec3 a, Vec3 b){
        double cosTheta = a.dot(b) / (a.length() * b.length());
        double theta = Math.acos(cosTheta);
        return theta;
    }
    /// 返回向量到z轴的θ角
    public static double getTheta(Vec3 a){
        return getTheta(a, new Vec3(0,0,1));
    }

    /// 包含所需的旋转量(角度)
    public static class Rot2 {
        double xD;
        double yD;

        public Rot2(double x, double y){
            xD = x;
            yD = y;
        }
        public Rot2(Vec3 v){
            this(v.x, v.y);
        }
    }
}
