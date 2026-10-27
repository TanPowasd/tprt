package org.mylove.tprt.compat.IronsSpellBooks;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.Tprt;
import org.mylove.tprt.compat.IronsSpellBooks.ModEffects.source_charge;
import org.mylove.tprt.compat.IronsSpellBooks.ModEffects.source_echo;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.*;
import org.mylove.tprt.compat.IronsSpellBooks.Modifiers.Curios.*;
import org.mylove.tprt.registries.ModFluids;
import org.mylove.tprt.registries.item.TPRTItemUtils;
import slimeknights.mantle.registration.deferred.FluidDeferredRegister;
import slimeknights.mantle.registration.object.FlowingFluidObject;
import slimeknights.mantle.registration.object.ItemObject;
import slimeknights.tconstruct.common.registration.ItemDeferredRegisterExtension;
import slimeknights.tconstruct.library.modifiers.util.ModifierDeferredRegister;
import slimeknights.tconstruct.library.modifiers.util.StaticModifier;
import slimeknights.tconstruct.library.tools.definition.ToolDefinition;
import slimeknights.tconstruct.library.tools.helper.ToolBuildHandler;
import slimeknights.tconstruct.library.tools.item.ModifiableItem;
import slimeknights.tconstruct.tools.item.ModifiableSwordItem;

public class IssCompat {

    public static final FluidDeferredRegister ISS_FLUIDS=new FluidDeferredRegister(Tprt.MODID);
    //lr閻楅€涚返 闁句線鐡熷▔鏇′粓閸斻劍绁︽担?(閻旀棁鐎哄ú鍓ф喓婵?/ 閻旀棁鐎烘總銉︽钩闁叉垵鐫?/ 閻旀棁鐎哄┃鎰窛閸氬牓鍣?/ 閻旀棁鐎哄┑鈩冪畭妤犳垵锛?
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_pyrium=ModFluids.registerHotFluid(ISS_FLUIDS,"molten_pyrium",2000,8,5,5,false);
    public static final FlowingFluidObject<ForgeFlowingFluid> molten_arcane_metal=ModFluids.registerHotFluid(ISS_FLUIDS,"molten_arcane_metal",1000,4,5,2,false);
    public static final FlowingFluidObject<ForgeFlowingFluid> SOURCE_ALLOY_FLUID=ModFluids.registerHotFluid(ISS_FLUIDS,"source_alloy_fluid",3500,4,5,2,false);
    public static final FlowingFluidObject<ForgeFlowingFluid> MOLTEN_DARK_KNIGHT=ModFluids.registerHotFluid(ISS_FLUIDS,"molten_dark_knight",2255,4,5,2,false);

    public static ModifierDeferredRegister Iss_MODIFIERS = ModifierDeferredRegister.create(Tprt.MODID);

    public static final DeferredRegister<MobEffect> Iss_MODEFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Tprt.MODID);

    /** 婵傘儲纭堕崶鐐叉惙閻ㄥ嫪澶嶉弮鑸垫煢濞夋洝鈧?(娴犲懎婀柧渚€鐡熷▔鏇炲鏉炶姤妞傚▔銊ュ斀) */
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ISS_ENTITIES =
            DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES, Tprt.MODID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<org.mylove.tprt.compat.IronsSpellBooks.entity.EchoCaster>> ECHO_CASTER =
            ISS_ENTITIES.register("echo_caster", () -> net.minecraft.world.entity.EntityType.Builder
                    .of(org.mylove.tprt.compat.IronsSpellBooks.entity.EchoCaster::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.6F, 1.8F).build("echo_caster"));

    /** 濠ф劘宸濋拑鍕厴鐏炲倹鏆?*/
    public static final RegistryObject<MobEffect> SOURCE_CHARGE=Iss_MODEFFECTS.register("source_charge", org.mylove.tprt.compat.IronsSpellBooks.ModEffects.source_charge::new);
    /** 濠ф劘宸濋崶鐐叉惙: 娑撳顐奸弬鑺ョ《缁涘楠囬幓鎰磳 */
    public static final RegistryObject<MobEffect>SOURCE_ECHO=Iss_MODEFFECTS.register("source_echo", source_echo::new);
    /**
     * 闁句線鐡熷▔鏇′粓閸斻劎澧块崫浣风瑩閻劍鏁為崘灞芥珤: 閸滃矁鐦濋弶?({@link #Iss_MODIFIERS}) 娑撳孩绁︽担?({@link #ISS_FLUIDS}) 娑撯偓閺?
     * 閸欘亝婀佸Λ鈧ù瀣煂闁句線鐡熷▔鏇熸閹靛秳绱伴崷?Tprt 闁插本瀵曢崚棰佺皑娴犺埖鈧崵鍤庢稉?閳ユ柡鈧?濞屄ゎ棅鐏忓崬鐣崗銊ょ瑝濞夈劌鍞?
     * 閹碘偓娴犮儲鏁炲▔鏇♀偓鍛＆娑撳秴婀▔銊ュ斀鐞涖劑鍣? 娑旂喍绗夋导姘毉閻滄澘婀崚娑⑩偓鐘靛⒖閸濅焦鐖妴?
     */
    public static final ItemDeferredRegisterExtension ISS_ITEMS = new ItemDeferredRegisterExtension(Tprt.MODID);
    /** 濞夈劍纭堕懓鍛畱瀹搞儱鍙跨€规矮绠?(鐠虹喓澧块崫浣风鐠? 閸欘亜婀憗鍛啊闁句線鐡熷▔鏇熸閸掓稑缂? */
    public static final ToolDefinition MAGIC_BLADE = ToolDefinition.create(Tprt.getResource("magic_blade"));
    //lr閻楅€涚返 濞夈劍纭堕懓?(闁句線鐡熷▔鏇′粓閸?
    public static final ItemObject<ModifiableItem> magic_blade = ISS_ITEMS.register("magic_blade",
            ()->new ModifiableSwordItem(TPRTItemUtils.UNSTACKABLE_PROPS, MAGIC_BLADE));

    //lr閻楅€涚返 濠ф劘宸濋崥鍫ュ櫨 / 濠碘剝绠嶆鎴濓紜闁?(闁句線鐡熷▔鏇′粓閸斻劍娼楅弬? 閺夋劖鏋＄€规矮绠熼柌灞藉嚒鐢?irons_spellbooks 閺夆€叉)
    public static final ItemObject<Item> SOURCE_ALLOY = ISS_ITEMS.register("source_alloy",
            ()->new org.mylove.tprt.compat.IronsSpellBooks.GlintingItem(new Item.Properties()));
    public static final ItemObject<Item> DARK_KNIGHT_INGOT = ISS_ITEMS.register("dark_knight_ingot",
            ()->new org.mylove.tprt.compat.IronsSpellBooks.GlintingItem(new Item.Properties()));

    /** 閸掓盯鈧姷澧块崫浣圭埉閺夛紕娲? 閻?ItemsRegistry 閸︺劍顥呭ù瀣煂闁句線鐡熷▔鏇熸鐠嬪啰鏁?*/
    public static void addToolTabItems(CreativeModeTab.Output tab) {
        ToolBuildHandler.addVariants(tab::accept, magic_blade.get(), "");
    }
    public static void addCommonTabItems(CreativeModeTab.Output tab) {
        tab.accept(SOURCE_ALLOY);
        tab.accept(DARK_KNIGHT_INGOT);
    }

    public static final StaticModifier<enchanting> enchanting;
    public static StaticModifier<human_turpentine> Human_turpentine;
    public static StaticModifier<Release_spells> Release_spells;
    public static StaticModifier<Immolate> ImmoLate;
    public static StaticModifier<Lightning_upgrade> Lightning_upgrade;
    public static StaticModifier<Imbued> Imbued;
    public static StaticModifier<light_the_fire> light_the_fire;
    public static StaticModifier<flaming_torus> flaming_torus;
    public static StaticModifier<fire_flow> fire_flow;
    public static StaticModifier<flame_paradise> flame_paradise;

    public static final StaticModifier<BreakPhantom> BreakPhantom;
    public static final StaticModifier<Thorough> Thorough;
    public static StaticModifier<necronomicon_spell_book> necronomicon_spell_book;
    /** 娴滄彃鎷? 鐠佲晛浼愰崗鐤箯瀵版ぞ缈伴崪灞肩閹存帞娈戦弫鍫熺亯 (姒涙顓?+1 缁? 娑斿鎮楅崣顖氭躬婵傘儲婀抽柧浣虹墪娑撳﹥娲块幑銏＄《閺? */
    public static StaticModifier<Affinity> affinity;
    /** 濠ф劘宸濋拑鍕厴 */
    public static StaticModifier<Source_charge> source_charge;
    /** 姒勬柨瀵叉稊瀣唲 (閸欐牔鍞潻婊呪柤閻ㄥ嫰鐡熼崝娑樺磳閸? */
    public static StaticModifier<Magic_arrow> magic_arrow;
    /** 姒勬梹绨憗纭呴煩 (閹躲倗鏁? */
    public static StaticModifier<Mana_surge> mana_surge;
    /** 婵傘儲纭堕崶鐐叉惙 (濞夋洘婀虫稊锕€绔? */
    public static StaticModifier<Arcane_echo> arcane_echo;
    public static StaticModifier<bloodstain> bloodstain;
    public static StaticModifier<source_alloy_charm> source_alloy_charm;
    static {
        enchanting = Iss_MODIFIERS.register("enchanting", enchanting::new);

        affinity = Iss_MODIFIERS.register("affinity", Affinity::new);
        source_charge = Iss_MODIFIERS.register("source_charge", Source_charge::new);
        magic_arrow = Iss_MODIFIERS.register("magic_arrow", Magic_arrow::new);
        mana_surge = Iss_MODIFIERS.register("mana_surge", Mana_surge::new);
        arcane_echo = Iss_MODIFIERS.register("arcane_echo", Arcane_echo::new);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(Chaos_Charm::onEntityJoin);
        Human_turpentine = Iss_MODIFIERS.register("human_turpentine", human_turpentine::new);
        Release_spells = Iss_MODIFIERS.register("release_spells", Release_spells::new);
        ImmoLate = Iss_MODIFIERS.register("immolate", Immolate::new);
        Lightning_upgrade = Iss_MODIFIERS.register("lightning_upgrade", Lightning_upgrade::new);
        Imbued = Iss_MODIFIERS.register("imbued", Imbued::new);
        light_the_fire = Iss_MODIFIERS.register("light_the_fire", light_the_fire::new);
        flaming_torus = Iss_MODIFIERS.register("flaming_torus", flaming_torus::new);
        fire_flow = Iss_MODIFIERS.register("fire_flow", fire_flow::new);
        flame_paradise = Iss_MODIFIERS.register("flame_paradise", flame_paradise::new);

        BreakPhantom = Iss_MODIFIERS.register("breakphantom", BreakPhantom::new);
        Thorough = Iss_MODIFIERS.register("thorough", Thorough::new);
        necronomicon_spell_book = Iss_MODIFIERS.register("necronomicon_spell_book", necronomicon_spell_book::new);
        bloodstain = Iss_MODIFIERS.register("bloodstain", bloodstain::new);
        source_alloy_charm = Iss_MODIFIERS.register("source_alloy_charm", source_alloy_charm::new);
    }

    /** 鐎圭偘缍嬬仦鐐粹偓褎鏁為崘?(韫囧懘銆忛崷銊δ佺紒鍕皑娴犺埖鈧崵鍤庢稉濠呯殶閻? */
    public static void onEntityAttributes(net.minecraftforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(ECHO_CASTER.get(), org.mylove.tprt.compat.IronsSpellBooks.entity.EchoCaster.createAttributes().build());
    }
}
