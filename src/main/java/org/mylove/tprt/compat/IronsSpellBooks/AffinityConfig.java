package org.mylove.tprt.compat.IronsSpellBooks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 亲和的数据包配置。
 *
 * 文件: data/<命名空间>/affinity/<名字>.json
 * {
 *   "bonus": 1,                 // 亲和带来的等级加成, 默认 1
 *   "mode": "free",             // free = 任意法术卷轴都能记录; directed = 只允许 spells 列表里的法术
 *   "spells": [                 // mode = directed 时生效
 *     "irons_spellbooks:fireball"
 *   ]
 * }
 */
public final class AffinityConfig extends SimpleJsonResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("tprt-affinity");
    private static final Gson GSON = new GsonBuilder().setLenient().create();

    private static volatile int bonus = 1;
    private static volatile boolean directed = false;
    private static volatile Set<ResourceLocation> spells = Set.of();

    static {
        MinecraftForge.EVENT_BUS.addListener(AffinityConfig::onAddReloadListeners);
    }

    private static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AffinityConfig());
    }

    public AffinityConfig() {
        super(GSON, "affinity");
    }

    /** 加成等级 */
    public static int bonus() {
        return Math.max(1, bonus);
    }

    /** 是否允许记录该法术 */
    public static boolean allows(ResourceLocation spell) {
        if (!directed || spells.isEmpty()) {
            return true;
        }
        return spells.contains(spell);
    }

    /** 是否定向模式 (仅用于 tooltip 提示) */
    public static boolean isDirected() {
        return directed && !spells.isEmpty();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager manager, ProfilerFiller profiler) {
        int newBonus = 1;
        boolean newDirected = false;
        Set<ResourceLocation> newSpells = new HashSet<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "affinity");
                if (json.has("bonus")) {
                    newBonus = Math.max(1, GsonHelper.getAsInt(json, "bonus", 1));
                }
                String mode = GsonHelper.getAsString(json, "mode", "free");
                newDirected = "directed".equalsIgnoreCase(mode);
                if (json.has("spells")) {
                    for (JsonElement element : GsonHelper.getAsJsonArray(json, "spells")) {
                        ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                        if (id != null) {
                            newSpells.add(id);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[tprt-affinity] 解析数据包配置失败: {} -> {}", entry.getKey(), e.getMessage());
            }
        }
        bonus = newBonus;
        directed = newDirected;
        spells = Set.copyOf(newSpells);
        LOGGER.info("[tprt-affinity] 已加载数据包配置: bonus={} mode={} spells={}", bonus, directed ? "directed" : "free", spells.size());
    }
}