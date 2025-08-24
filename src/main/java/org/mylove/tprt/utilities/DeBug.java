package org.mylove.tprt.utilities;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.text.DecimalFormat;

/** 仅用作开发调试，最终代码中不应包含任何此中内容 */
public class DeBug {
    /** 向玩家发送聊天信息，方法内未作客户端判定 */
    public static void Console(Player player, String message){
        player.sendSystemMessage(Component.literal(message));
    }
    public static void Console(Player player, Number message){
        Console(player, ""+message);
    }

    /** 展平一个三维向量，并限制小数点位数 */
    public static String FlatXYZ(Vec3 v3) {

        DecimalFormat df= new DecimalFormat("0.00");
        return df.format(v3.x) +" x  " +df.format(v3.y) +" y  " +df.format(v3.z) +" z  ";
    }
}
