package org.mylove.tprt.utilities;

public class Math0 {
    /**
     * call to clamp a value, 使输出结果落在由上下界构成的闭区间内
     * @param value    目标值
     * @param min      下界
     * @param max      上界
     */
    public static <T extends Comparable<T>> T clamp(T value, T min, T max) {
        if (value.compareTo(min) < 0) {
            return min;
        } else if (value.compareTo(max) > 0) {
            return max;
        } else {
            return value;
        }
    }

    /**
     * is the given value between the two values? 开区间
     * @param value    比较值
     * @param min      下界
     * @param max      上界
     */
//    public static <T extends Comparable<Number>> T isBetween(T value, T min, T max) {
//        return  min < value && value < max;
//    }

    /**
     * is the given value between the two values? 闭区间
     * @param value    比较值
     * @param min      下界
     * @param max      上界
     *
     */
    public static boolean isBetweenAnd(int value, int min, int max) {
        return min <= value && value <= max;
    }
}
