package org.mylove.tprt.registries;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.mylove.tprt.spells.fire.Fire_additionSpell;

public class SpellsRegistry {
    public static final DeferredRegister<AbstractSpell> SPELLS;
    public static final RegistryObject<AbstractSpell> fire_addition;

    public static RegistryObject<AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    public static void register(IEventBus eventBus) {
        SPELLS.register(eventBus);
    }

    static {
        SPELLS = DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, "tprt");
        fire_addition = registerSpell(new Fire_additionSpell());
    }
}

