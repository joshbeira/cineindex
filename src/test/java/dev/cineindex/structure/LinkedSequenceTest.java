package dev.cineindex.structure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LinkedSequenceTest {
    private LinkedSequence<String> list;

    @BeforeEach
    void setUp() {
        list = new LinkedSequence<>();
    }

    @Test
    void testEmptyList() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertFalse(list.contains("A"));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0));
    }

    @Test
    void testAddAndGet() {
        assertTrue(list.add("A"));
        assertTrue(list.add("B"));
        assertTrue(list.add("C"));

        assertFalse(list.isEmpty());
        assertEquals(3, list.size());
        assertEquals("A", list.get(0));
        assertEquals("B", list.get(1));
        assertEquals("C", list.get(2));
    }

    @Test
    void testRemove() {
        list.add("A");
        list.add("B");
        list.add("C");

        assertTrue(list.remove("B"));
        assertEquals(2, list.size());
        assertEquals("C", list.get(1));

        assertFalse(list.remove("D"));
        assertEquals(2, list.size());

        assertTrue(list.remove("A"));
        assertTrue(list.remove("C"));
        assertTrue(list.isEmpty());
    }

    @Test
    void testIndexOf() {
        list.add("A");
        list.add("B");
        list.add("A");

        assertEquals(0, list.indexOf("A"));
        assertEquals(1, list.indexOf("B"));
        assertEquals(-1, list.indexOf("C"));
    }

    @Test
    void testSet() {
        list.add("A");
        list.add("B");

        assertEquals("A", list.set(0, "Z"));
        assertEquals("Z", list.get(0));
        assertEquals("B", list.get(1));

        assertThrows(IndexOutOfBoundsException.class, () -> list.set(2, "X"));
    }

    @Test
    void testClear() {
        list.add("A");
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    void testToArray() {
        list.add("A");
        list.add("B");

        String[] arr = new String[2];
        list.toArray(arr);
        assertEquals("A", arr[0]);
        assertEquals("B", arr[1]);

        String[] largeArr = new String[3];
        list.toArray(largeArr);
        assertNull(largeArr[2]);
    }
}
