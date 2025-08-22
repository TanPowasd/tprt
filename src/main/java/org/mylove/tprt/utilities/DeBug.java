package org.mylove.tprt.utilities;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** 仅用作开发调试，最终代码中不应包含任何此中内容 */
public class DeBug {
    /** 向玩家发送聊天信息，方法内未作客户端判定 */
    public static void Console(Player player, String message){
        player.sendSystemMessage(Component.literal(message));
    }
}
