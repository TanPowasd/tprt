package org.mylove.tprt.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.mylove.tprt.registries.ModBuffRegistry;

/** 次数盾提示: 画在生命值 UI 正上方, 显示还能免疫几次攻击 */
public class Attack_shield_hud {

    public static final IGuiOverlay OVERLAY = (gui, graphics, partialTick, width, height) -> render(graphics, width, height);

    private static void render(GuiGraphics graphics, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null || minecraft.options.hideGui || player.isSpectator()) {
            return;
        }
        MobEffectInstance shield = player.getEffect(ModBuffRegistry.ATTACK_SHIELD.get());
        if (shield == null) {
            return;
        }
        Component text = Component.translatable("tprt.hud.attack_shield", shield.getAmplifier() + 1);
        int x = width / 2 - minecraft.font.width(text) / 2;
        // 生命条画在 height - 39, 这里往上抬 12 像素
        int y = height - 39 - 12;
        graphics.drawString(minecraft.font, text, x, y, 0x55FFFF, true);
    }
}