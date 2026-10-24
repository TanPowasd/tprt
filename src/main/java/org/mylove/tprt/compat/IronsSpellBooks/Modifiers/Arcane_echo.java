package org.mylove.tprt.compat.IronsSpellBooks.Modifiers;

import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import org.jetbrains.annotations.Nullable;
import org.mylove.tprt.compat.IronsSpellBooks.IssCompat;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 奥法回响 (法术书布): 施法记录最近 3 条法术 (含施法时瞄准的目标) 并叠加源质蓄能;
 * 满 5 层时消耗全部层数, 每条记录生成一个独立施法者重放。
 */
public class Arcane_echo extends NoLevelsModifier {

    public static final ModifierId ID = new ModifierId("tprt", "arcane_echo");
    public static final int MAX_RECORDS = 3;
    public static final int REPLAY_TICKS = 2;
    public static final int STACK_DURATION = 600;
    public static final int MAX_CHARGE = 5;
    /** 施法时记录目标的射线长度 */
    public static final double AIM_RANGE = 32.0D;

    /** 记录: 法术 + 等级 + 施法那一刻瞄准的目标 UUID */
    private record Recorded(ResourceLocation spellId, int level, @Nullable UUID targetId, float yRot, float xRot) {
    }

    private record Pending(List<Recorded> spells, int ticks) {
    }

    private static final Map<UUID, List<Recorded>> RECORDED = new HashMap<>();
    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    static {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, SpellOnCastEvent.class, Arcane_echo::onCast);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, TickEvent.PlayerTickEvent.class, Arcane_echo::onTick);
    }

    private static void onCast(SpellOnCastEvent event) {
        Player player = event.getEntity();
        if (player == null || player.level().isClientSide) {
            return;
        }
        CastSource source = event.getCastSource();
        if (source == CastSource.COMMAND || source == CastSource.NONE || source == CastSource.MOB) {
            return;   // 重放 / 非玩家施法
        }
        if (!wearingBook(player)) {
            return;
        }

        // 1) 记录法术 + 施法瞬间瞄准的目标
        String raw = event.getSpellId();
        ResourceLocation spellId = raw == null ? null : ResourceLocation.tryParse(raw);
        if (spellId != null) {
            LivingEntity aim = raycastTarget(player);
            List<Recorded> list = RECORDED.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
            list.add(new Recorded(spellId, event.getOriginalSpellLevel(),
                    aim == null ? null : aim.getUUID(), player.getYRot(), player.getXRot()));
            while (list.size() > MAX_RECORDS) {
                list.remove(0);
            }
        }

        // 2) 法术书自身叠 1 层 (武器特性 Source_charge 会独立叠它自己的一层)
        MobEffectInstance current = player.getEffect(IssCompat.SOURCE_CHARGE.get());
        int previous = current == null ? -1 : current.getAmplifier();
        int amplifier = Math.min(MAX_CHARGE - 1, previous + 1);
        player.addEffect(new MobEffectInstance(IssCompat.SOURCE_CHARGE.get(), STACK_DURATION, amplifier, false, true, true));
        if (amplifier == MAX_CHARGE - 1 && previous < MAX_CHARGE - 1) {
            player.displayClientMessage(Component.translatable("tooltip.tprt.source_charge.full"), true);
        }
        if (amplifier < MAX_CHARGE - 1) {
            return;
        }

        // 3) 满层: 消耗全部层数并安排重放
        player.removeEffect(IssCompat.SOURCE_CHARGE.get());
        List<Recorded> snapshot = new ArrayList<>(RECORDED.getOrDefault(player.getUUID(), List.of()));
        RECORDED.remove(player.getUUID());
        if (!snapshot.isEmpty()) {
            PENDING.put(player.getUUID(), new Pending(snapshot, REPLAY_TICKS));
        }
    }

    private static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Pending pending = PENDING.get(player.getUUID());
        if (pending == null) {
            return;
        }
        if (pending.ticks() > 0) {
            PENDING.put(player.getUUID(), new Pending(pending.spells(), pending.ticks() - 1));
            return;
        }
        PENDING.remove(player.getUUID());
        ServerLevel level = (ServerLevel) player.level();
        int index = 0;
        for (Recorded recorded : pending.spells()) {
            index++;
            var spell = SpellRegistry.getSpell(recorded.spellId());
            if (spell == null) {
                continue;
            }
            var caster = new org.mylove.tprt.compat.IronsSpellBooks.entity.EchoCaster(IssCompat.ECHO_CASTER.get(), level);
            caster.setPos(player.getX(), player.getEyeY() - 1.62D, player.getZ());
            caster.setYRot(recorded.yRot());
            caster.setXRot(recorded.xRot());
            caster.setYHeadRot(recorded.yRot());
            caster.copyAttributesFrom(player);
            var queue = new ArrayList<org.mylove.tprt.compat.IronsSpellBooks.entity.EchoCaster.Queued>();
            queue.add(new org.mylove.tprt.compat.IronsSpellBooks.entity.EchoCaster.Queued(spell, Math.max(1, recorded.level())));
            caster.setDelay(index * 6);
            caster.setQueue(queue, serverPlayer);
            // 目标: 用施法那一刻记录下来的目标
            if (recorded.targetId() != null) {
                var entity = level.getEntity(recorded.targetId());
                if (entity instanceof LivingEntity target && target.isAlive()) {
                    caster.setTarget(target);
                }
            }
            level.addFreshEntity(caster);
        }
    }

    /** 施法瞬间瞄准的实体 */
    @Nullable
    private static LivingEntity raycastTarget(Player player) {
        var eye = player.getEyePosition();
        var look = player.getViewVector(1.0F);
        var end = eye.add(look.scale(AIM_RANGE));
        var box = player.getBoundingBox().expandTowards(look.scale(AIM_RANGE)).inflate(1.0D);
        var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(
                player, eye, end, box,
                e -> e instanceof LivingEntity le && le.canBeSeenAsEnemy(), eye.distanceToSqr(end));
        if (hit != null && hit.getEntity() instanceof LivingEntity target) {
            return target;
        }
        return null;
    }

    /** 是否佩戴带本词条的法术书 (法术书在饰品栏) */
    private static boolean wearingBook(Player player) {
        var curios = top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).resolve();
        if (curios.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> entry
                : curios.get().getCurios().entrySet()) {
            top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler stacks = entry.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                net.minecraft.world.item.ItemStack stack = stacks.getStackInSlot(i);
                if (!stack.isEmpty()
                        && slimeknights.tconstruct.library.tools.nbt.ToolStack.isInitialized(stack)
                        && slimeknights.tconstruct.library.tools.nbt.ToolStack.from(stack).getModifierLevel(ID) > 0) {
                    return true;
                }
            }
        }
        return false;
    }
}
