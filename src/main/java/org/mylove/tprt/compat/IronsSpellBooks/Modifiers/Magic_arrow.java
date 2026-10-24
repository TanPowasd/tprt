package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import org.mylove.tprt.compat.IronsSpellBooks.IssCompat;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;

/**
 * 魔化之箭: 远程命中时把箭矢转化为铁魔法的魔法箭。
 * 转换发生在命中瞬间, 所以飞行阶段的匠魂特性 (穿透/追踪/速度) 全部保留;
 * 伤害 = 原箭 baseDamage (已含匠魂弹射物加成) × 速度长度 × √法术强度。
 * 数值曲线与近战「魔力升华」保持一致 (都用 √法强)。
 */
public class Magic_arrow extends NoLevelsModifier {

    public static final ModifierId ID = new ModifierId("tprt", "magic_arrow");
    /** 爆炸半径 */
    public static final float EXPLOSION_RADIUS = 3.0F;static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ProjectileImpactEvent.class, Magic_arrow::onImpact);
    }

    private static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }
        if (!(arrow.getOwner() instanceof Player player) || player.level().isClientSide) {
            return;
        }
        if (!hasTrait(player.getMainHandItem()) && !hasTrait(player.getOffhandItem())) {
            return;
        }
        Vec3 motion = arrow.getDeltaMovement();
        double speed = motion.length();
        if (speed <= 0.001D) {
            return;
        }
        float spellPower = 1.0F;
        AttributeInstance attribute = player.getAttribute(AttributeRegistry.SPELL_POWER.get());
        if (attribute != null) {
            spellPower = (float) attribute.getValue();
        }
        // 满 5 层时魔法箭伤害额外 +50%
        MobEffectInstance charge = player.getEffect(IssCompat.SOURCE_CHARGE.get());
        boolean full = charge != null && charge.getAmplifier() >= 4;
        float damage = (float) (arrow.getBaseDamage() * speed * Math.sqrt(Math.max(0.0, spellPower)));
        if (full) {
            damage *= 1.5F;
        }

        MagicArrowProjectile magic = new MagicArrowProjectile(player.level(), player);
        magic.setDamage(damage);
        magic.setExplosionRadius(EXPLOSION_RADIUS);
        magic.setOwner(player);
        magic.setPos(arrow.getX(), arrow.getY(), arrow.getZ());
        magic.setDeltaMovement(motion);
        player.level().addFreshEntity(magic);

        // 命中叠加一层源质蓄能 (每次命中 +1, 多重射击收益更高)
        int previous = charge == null ? -1 : charge.getAmplifier();
        int amplifier = Math.min(4, previous + 1);
        player.addEffect(new MobEffectInstance(IssCompat.SOURCE_CHARGE.get(), 600, amplifier, false, true, true));
        if (amplifier == 4 && previous < 4) {
            player.displayClientMessage(Component.translatable("tooltip.tprt.source_charge.full"), true);
        }

        arrow.discard();
        // 跳过原箭的实体命中结算 (setCanceled 已弃用) 
        event.setImpactResult(ProjectileImpactEvent.ImpactResult.SKIP_ENTITY);}

    private static boolean hasTrait(ItemStack stack) {
        return !stack.isEmpty() && ToolStack.isInitialized(stack)
                && ToolStack.from(stack).getModifierLevel(ID) > 0;
    }
}