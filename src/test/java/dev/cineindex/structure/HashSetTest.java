package dev.cineindex.structure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashSetTest {
    private HashSet<String> set;

    @BeforeEach
    void setUp() {
        set = new HashSet<>();
    }

    @Test
    void testEmptySet() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains("A"));
    }

    @Test
    void testAddAndContains() {
        assertTrue(set.add("A"));
        assertTrue(set.add("B"));
        assertFalse(set.add("A")); // Duplicate

        assertEquals(2, set.size());
        assertTrue(set.contains("A"));
        assertTrue(set.contains("B"));
        assertFalse(set.contains("C"));
    }

    @Test
    void testRemove() {
        set.add("A");
        set.add("B");

        assertTrue(set.remove("A"));
        assertEquals(1, set.size());
        assertFalse(set.contains("A"));

        assertFalse(set.remove("C"));
    }

    @Test
    void testClear() {
        set.add("A");
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains("A"));
    }

    @Test
    void testToList() {
        set.add("A");
        set.add("B");

        LinkedSequence<String> list = set.toList();
        assertEquals(2, list.size());
        assertTrue(list.contains("A"));
        assertTrue(list.contains("B"));
    }
}
