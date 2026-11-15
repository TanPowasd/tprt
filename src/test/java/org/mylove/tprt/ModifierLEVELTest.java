package org.mylove.tprt;

import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mylove.tprt.utils.ModifierLEVEL;
import slimeknights.tconstruct.library.modifiers.ModifierId;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ModifierLEVELTest {

    @Test
    @DisplayName("Verify UUIDFromWeapon generates deterministic and unique UUIDs")
    void testUUIDFromWeapon() {
        ModifierId modA = new ModifierId("tprt", "seniorious");
        ModifierId modB = new ModifierId("tprt", "elegy_of_world_end");

        UUID uuidMainA = ModifierLEVEL.UUIDFromWeapon(EquipmentSlot.MAINHAND, modA);
        UUID uuidMainA_again = ModifierLEVEL.UUIDFromWeapon(EquipmentSlot.MAINHAND, modA);
        UUID uuidOffA = ModifierLEVEL.UUIDFromWeapon(EquipmentSlot.OFFHAND, modA);
        UUID uuidMainB = ModifierLEVEL.UUIDFromWeapon(EquipmentSlot.MAINHAND, modB);

        assertNotNull(uuidMainA);
        assertEquals(uuidMainA, uuidMainA_again, "Same slot and modifier must produce identical UUID");
        assertNotEquals(uuidMainA, uuidOffA, "Different slots must produce different UUIDs");
        assertNotEquals(uuidMainA, uuidMainB, "Different modifiers must produce different UUIDs");
    }

    @Test
    @DisplayName("Verify null entity safely returns 0 or false across all level inspection methods")
    void testNullEntityHandling() {
        ModifierId mod = new ModifierId("tprt", "seniorious");

        assertEquals(0, ModifierLEVEL.getMainhandModifierlevel(null, mod));
        assertEquals(0, ModifierLEVEL.getOffhandModifierlevel(null, mod));
        assertEquals(0, ModifierLEVEL.getEachHandsTotalModifierlevel(null, mod));
        assertFalse(ModifierLEVEL.EachHandsHaveModifierlevel(null, mod));
        assertFalse(ModifierLEVEL.HandsHaveModifierlevel(null, mod));

        assertEquals(0, ModifierLEVEL.getHeadModifierlevel(null, mod));
        assertEquals(0, ModifierLEVEL.getChestModifierlevel(null, mod));
        assertEquals(0, ModifierLEVEL.getLegsModifierlevel(null, mod));
        assertEquals(0, ModifierLEVEL.getFeetsModifierlevel(null, mod));
        assertEquals(0, ModifierLEVEL.getTotalArmorModifierlevel(null, mod));
        assertFalse(ModifierLEVEL.EachaArmorHasModifierlevel(null, mod));
        assertFalse(ModifierLEVEL.EquipHasModifierlevel(null, mod));
    }
}
