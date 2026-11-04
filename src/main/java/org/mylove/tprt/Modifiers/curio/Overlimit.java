package org.mylove.tprt.Modifiers.curio;

import com.ssakura49.tinkercuriolib.hook.TCLibHooks;
import com.ssakura49.tinkercuriolib.hook.armor.CurioEquipmentChangeModifierHook;
import com.ssakura49.tinkercuriolib.hook.armor.CurioTakeDamagePreModifierHook;
import com.ssakura49.tinkercuriolib.hook.armor.CurioTakeDamagePostModifierHook;
import com.ssakura49.tinkercuriolib.hook.combat.CurioDamageTargetPreModifierHook;
import com.ssakura49.tinkercuriolib.hook.combat.CurioDamageTargetPostModifierHook;
import com.ssakura49.tinkercuriolib.hook.interation.CurioInventoryTickModifierHook;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import slimeknights.tconstruct.library.modifiers.ModifierEntry;
import slimeknights.tconstruct.library.modifiers.ModifierId;
import slimeknights.tconstruct.library.modifiers.impl.NoLevelsModifier;
import slimeknights.tconstruct.library.module.ModuleHookMap;
import slimeknights.tconstruct.library.tools.nbt.IToolStackView;
import slimeknights.tconstruct.library.tools.nbt.ToolStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

/** Independent charm trait. Effects are display-only; server state is the authority. */
public final class Overlimit extends NoLevelsModifier implements CurioEquipmentChangeModifierHook,
        CurioInventoryTickModifierHook, CurioDamageTargetPreModifierHook,
        CurioDamageTargetPostModifierHook, CurioTakeDamagePreModifierHook, CurioTakeDamagePostModifierHook {
    public static final ModifierId ID = new ModifierId("tprt", "overlimit");
    private static final TagKey<Item> CHARM = TagKey.create(Registries.ITEM, new ResourceLocation("curios", "tinker_charm"));
    private static final TagKey<DamageType> CAN_PROTECT = TagKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("mantle", "can_protect"));
    private static final ResourceLocation PROGRESS = new ResourceLocation("tprt", "overlimit_progress");
    private static final ResourceLocation EMPOWERED = new ResourceLocation("tprt", "overlimit_empowered");
    private final Map<LivingEntity, State> states = new WeakHashMap<>();
    private final Map<LivingEntity, Deque<Hit>> pending = new WeakHashMap<>();

    @Override
    protected void registerHooks(ModuleHookMap.@NotNull Builder builder) {
        super.registerHooks(builder);
        builder.addHook(this, TCLibHooks.CURIO_EQUIPMENT_CHANGE, TCLibHooks.CURIO_TICK,
                TCLibHooks.CURIO_DAMAGE_TARGET_PRE, TCLibHooks.CURIO_DAMAGE_TARGET_POST,
                TCLibHooks.CURIO_TAKE_DAMAGE_PRE, TCLibHooks.CURIO_TAKE_DAMAGE_POST);
    }

    private static boolean validInSlot(ItemStack stack, String slot) {
        if (!slot.equals("tinker_charm") && !slot.equals("curio")) return false;
        if (stack.isEmpty() || !stack.is(CHARM) || !ToolStack.isInitialized(stack)
                || !stack.is(TagKey.create(Registries.ITEM, new ResourceLocation("curios", slot)))) return false;
        var tool = ToolStack.from(stack);
        return !tool.isBroken() && tool.getModifierLevel(ID) > 0;
    }

    /** Select one valid equipped charm. TCLib dispatch excludes cosmetics, but not broken tools. */
    private boolean wearing(LivingEntity entity, SlotContext excluded) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("sakuratinker")) return false;
        var inventory = CuriosApi.getCuriosInventory(entity).resolve();
        if (inventory.isEmpty()) return false;
        for (var slot : inventory.get().getCurios().entrySet()) {
            String name = slot.getKey();
            if (!name.equals("tinker_charm") && !name.equals("curio")) continue;
            var stacks = slot.getValue().getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                if (excluded != null && excluded.identifier().equals(name) && excluded.index() == i) continue;
                if (validInSlot(stacks.getStackInSlot(i), name)) return true;
            }
        }
        return false;
    }

    @Override
    public void onCurioEquip(IToolStackView curio, ModifierEntry entry, SlotContext context,
                             LivingEntity entity, ItemStack prevStack, ItemStack stack) {
        if (entity.level().isClientSide || context.cosmetic()) return;
        if (!wearing(entity, null)) clear(entity);
    }

    @Override
    public void onCurioUnequip(IToolStackView curio, ModifierEntry entry, SlotContext context,
                               LivingEntity entity, ItemStack newStack, ItemStack stack) {
        if (entity.level().isClientSide || context.cosmetic()) return;
        // Inventory may still expose the outgoing stack while Curios invokes this hook.
        if (!validInSlot(newStack, context.identifier()) && !wearing(entity, context)) clear(entity);
    }

    private State active(LivingEntity entity) {
        if (!entity.isAlive() || !wearing(entity, null)) {
            clear(entity);
            return null;
        }
        long now = entity.level().getGameTime();
        State previous = states.get(entity);
        if (previous != null && (now < previous.lastTick || now - previous.lastTick > 1)) clear(entity);
        State state = states.computeIfAbsent(entity, ignored -> new State());
        state.lastTick = now;
        return state;
    }

    private void clear(LivingEntity entity) {
        states.remove(entity);
        pending.remove(entity);
        removeEffect(entity, PROGRESS);
        removeEffect(entity, EMPOWERED);
    }

    private static LivingEntity directAttacker(DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) return null;
        if (source.getDirectEntity() == attacker && (source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO))) return attacker;
        if (attacker instanceof Player && source.is(DamageTypeTags.IS_PROJECTILE)
                && source.getDirectEntity() instanceof Projectile projectile && projectile.getOwner() == attacker) return attacker;
        return null;
    }

    @Override
    public void onDamageTargetPre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event,
                                  LivingEntity attacker, LivingEntity target) {
        if (attacker != target && directAttacker(event.getSource()) == attacker) pre(event, attacker, true);
    }

    @Override
    public void onCurioTakeDamagePre(IToolStackView curio, ModifierEntry entry, LivingHurtEvent event,
                                     LivingEntity entity, DamageSource source) {
        pre(event, entity, false);
    }

    private void pre(LivingHurtEvent event, LivingEntity wearer, boolean offense) {
        if (wearer.level().isClientSide || event.isCanceled() || event.getAmount() <= 0
                || !Float.isFinite(event.getAmount())) return;
        State state = active(wearer);
        if (state == null) return;
        long now = wearer.level().getGameTime();
        Deque<Hit> hits = pending.computeIfAbsent(wearer, ignored -> new ArrayDeque<>());
        hits.removeIf(hit -> hit.time != now);
        for (Hit hit : hits) if (hit.pre == event && hit.offense == offense) return;
        hits.addLast(new Hit(event, now, offense, wearer.getHealth() >= wearer.getMaxHealth(), state));
        if (offense) event.setAmount(event.getAmount() * (state.empowered(now) ? 1.30F : 1.15F));
        else if (event.getSource().is(CAN_PROTECT))
            event.setAmount(event.getAmount() * (state.empowered(now) ? 0.80F : 0.90F));
    }

    @Override
    public void onCurioDamageTargetPost(IToolStackView curio, ModifierEntry entry, LivingDamageEvent event,
                                       LivingEntity attacker, LivingEntity target) {
        if (attacker != target && directAttacker(event.getSource()) == attacker) post(event, attacker, true);
    }

    @Override
    public void onCurioTakeDamagePost(IToolStackView curio, ModifierEntry entry, LivingDamageEvent event,
                                      LivingEntity entity, DamageSource source) {
        post(event, entity, false);
    }

    /** TCLib post is after absorption, but before health subtraction. Later Forge subscribers
     * can still cancel/change damage; hook-only code cannot observe their final result. */
    private void post(LivingDamageEvent event, LivingEntity wearer, boolean offense) {
        if (wearer.level().isClientSide || event.isCanceled() || event.getAmount() <= 0
                || !Float.isFinite(event.getAmount())) return;
        Deque<Hit> hits = pending.get(wearer);
        if (hits == null) return;
        long now = wearer.level().getGameTime();
        for (Hit hit : hits) if (hit.post == event && hit.offense == offense) return;
        var iterator = hits.descendingIterator();
        while (iterator.hasNext()) {
            Hit hit = iterator.next();
            if (hit.time != now || hit.offense != offense || hit.post != null
                    || hit.pre.getSource() != event.getSource() || hit.pre.getEntity() != event.getEntity()) continue;
            hit.post = event; // Shared identity/direction marker deduplicates all equipped charms.
            if (active(wearer) == hit.state) charge(wearer, hit.full, now);
            return;
        }
    }

    private void charge(LivingEntity wearer, boolean full, long now) {
        State state = active(wearer);
        if (state == null || state.empowered(now) || now < state.nextCharge) return;
        state.nextCharge = now + 20;
        state.stacks = Math.min(5, state.stacks + (full ? 2 : 1));
        if (state.stacks == 5) {
            state.stacks = 0;
            state.until = now + 120;
        }
        sync(wearer, state, now);
    }

    @Override
    public void onCurioTick(IToolStackView curio, ModifierEntry entry, SlotContext context,
                            LivingEntity entity, ItemStack stack) {
        if (entity.level().isClientSide || context.cosmetic()) return;
        long now = entity.level().getGameTime();
        Deque<Hit> hits = pending.get(entity);
        if (hits != null) {
            hits.removeIf(hit -> hit.time != now);
            if (hits.isEmpty()) pending.remove(entity);
        }
        State state = active(entity);
        if (state != null) sync(entity, state, now);
    }

    private static void removeEffect(LivingEntity entity, ResourceLocation id) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect != null && entity.hasEffect(effect)) entity.removeEffect(effect);
    }

    private static void display(LivingEntity entity, ResourceLocation id, int duration, int amplifier) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
        if (effect == null) return;
        MobEffectInstance current = entity.getEffect(effect);
        if (current == null || current.getAmplifier() != amplifier || Math.abs(current.getDuration() - duration) > 2) {
            entity.removeEffect(effect);
            entity.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, true));
        }
    }

    private static void sync(LivingEntity wearer, State state, long now) {
        if (state.empowered(now)) {
            removeEffect(wearer, PROGRESS);
            display(wearer, EMPOWERED, (int) (state.until - now), 0);
        } else {
            removeEffect(wearer, EMPOWERED);
            if (state.stacks > 0) display(wearer, PROGRESS, 40, state.stacks - 1);
            else removeEffect(wearer, PROGRESS);
        }
    }

    private static final class State {
        int stacks;
        long nextCharge, until, lastTick;
        boolean empowered(long now) { return now < until; }
    }

    private static final class Hit {
        final LivingHurtEvent pre;
        final long time;
        final boolean offense, full;
        final State state;
        LivingDamageEvent post;
        Hit(LivingHurtEvent pre, long time, boolean offense, boolean full, State state) {
            this.pre = pre;
            this.time = time;
            this.offense = offense;
            this.full = full;
            this.state = state;
        }
    }
}
