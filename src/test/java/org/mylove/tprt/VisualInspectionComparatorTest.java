package org.mylove.tprt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VisualInspectionComparatorTest {

    record MockEntityDist(String id, double distanceSq) {}

    @Test
    @DisplayName("Verify distance comparator satisfies reflexivity, symmetry, and transitivity")
    void testComparatorContract() {
        Comparator<MockEntityDist> comparator = (e1, e2) -> Double.compare(e1.distanceSq(), e2.distanceSq());

        MockEntityDist d1 = new MockEntityDist("A", 10.5);
        MockEntityDist d2 = new MockEntityDist("B", 10.5);
        MockEntityDist d3 = new MockEntityDist("C", 25.0);
        MockEntityDist d4 = new MockEntityDist("D", 5.0);

        // Reflexivity: compare(x, x) == 0
        assertEquals(0, comparator.compare(d1, d1), "Reflexivity failed for d1");
        assertEquals(0, comparator.compare(d2, d2), "Reflexivity failed for d2");

        // Symmetry: sgn(compare(x, y)) == -sgn(compare(y, x))
        assertEquals(0, comparator.compare(d1, d2));
        assertEquals(0, comparator.compare(d2, d1));
        assertTrue(comparator.compare(d1, d3) < 0);
        assertTrue(comparator.compare(d3, d1) > 0);

        // Transitivity: compare(x, y) > 0 and compare(y, z) > 0 implies compare(x, z) > 0
        assertTrue(comparator.compare(d3, d1) > 0);
        assertTrue(comparator.compare(d1, d4) > 0);
        assertTrue(comparator.compare(d3, d4) > 0);

        // TimSort contract test: sorting a large list with many duplicate values must not throw
        List<MockEntityDist> list = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            list.add(new MockEntityDist("E" + i, (i % 7) * 2.5));
        }
        assertDoesNotThrow(() -> list.sort(comparator), "Sorting duplicate distances must not violate TimSort contract");

        // Ensure list is properly sorted
        for (int i = 0; i < list.size() - 1; i++) {
            assertTrue(list.get(i).distanceSq() <= list.get(i + 1).distanceSq(), "List should be monotonically non-decreasing");
        }
    }
}
