package dev.cineindex.structure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HashTableTest {
    private HashTable<Integer, String> map;

    @BeforeEach
    void setUp() {
        map = new HashTable<>();
    }

    @Test
    void testEmptyMap() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get(1));
        assertFalse(map.containsKey(1));
    }

    @Test
    void testPutAndGet() {
        assertNull(map.put(1, "One"));
        assertEquals("One", map.put(1, "Uno")); // returns old value
        assertEquals("Uno", map.get(1));

        map.put(2, "Two");
        assertEquals(2, map.size());
        assertEquals("Two", map.get(2));
    }

    @Test
    void testRemove() {
        map.put(1, "One");
        map.put(2, "Two");

        assertEquals("One", map.remove(1));
        assertNull(map.get(1));
        assertEquals(1, map.size());

        assertNull(map.remove(3));
    }

    @Test
    void testResize() {
        // Trigger resize (default cap 16, load factor 0.75 -> resizes at 12)
        for (int i = 0; i < 20; i++) {
            map.put(i, "Value" + i);
        }
        assertEquals(20, map.size());
        for (int i = 0; i < 20; i++) {
            assertEquals("Value" + i, map.get(i));
        }
    }

    @Test
    void testKeysAndValues() {
        map.put(1, "One");
        map.put(2, "Two");

        LinkedSequence<Integer> keys = map.keys();
        assertEquals(2, keys.size());
        assertTrue(keys.contains(1));
        assertTrue(keys.contains(2));

        LinkedSequence<String> values = map.values();
        assertEquals(2, values.size());
        assertTrue(values.contains("One"));
        assertTrue(values.contains("Two"));
    }

    @Test
    void testClear() {
        map.put(1, "One");
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get(1));
    }
}
