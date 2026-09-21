package dev.cineindex.structure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AvlTreeTest {
    private AvlTree<Integer, String> tree;

    @BeforeEach
    void setUp() {
        tree = new AvlTree<>();
    }

    @Test
    void testEmptyTree() {
        assertTrue(tree.isEmpty());
        assertEquals(0, tree.size());
        assertNull(tree.get(1));
    }

    @Test
    void testPutAndGet() {
        tree.put(10, "Ten");
        tree.put(20, "Twenty");
        tree.put(5, "Five");

        assertEquals(3, tree.size());

        LinkedSequence<String> tenList = tree.get(10);
        assertNotNull(tenList);
        assertEquals(1, tenList.size());
        assertEquals("Ten", tenList.get(0));

        assertNull(tree.get(100));
    }

    @Test
    void testDuplicateKeys() {
        tree.put(10, "Ten1");
        tree.put(10, "Ten2");

        assertEquals(2, tree.size());
        LinkedSequence<String> list = tree.get(10);
        assertEquals(2, list.size());
        assertTrue(list.contains("Ten1"));
        assertTrue(list.contains("Ten2"));
    }

    @Test
    void testBalancing() {
        // Force right-right case
        tree.put(1, "1");
        tree.put(2, "2");
        tree.put(3, "3");

        assertEquals(3, tree.size());
        assertEquals("2", tree.get(2).get(0));

        // Let's add more to test rotation
        for (int i = 4; i <= 10; i++) {
            tree.put(i, String.valueOf(i));
        }

        assertEquals(10, tree.size());
        for (int i = 1; i <= 10; i++) {
            assertNotNull(tree.get(i));
        }
    }

    @Test
    void testTraversals() {
        tree.put(10, "10");
        tree.put(5, "5");
        tree.put(20, "20");

        LinkedSequence<Integer> inOrder = tree.getKeysInOrder();
        assertEquals(3, inOrder.size());
        assertEquals(5, inOrder.get(0));
        assertEquals(10, inOrder.get(1));
        assertEquals(20, inOrder.get(2));

        LinkedSequence<Integer> reverseInOrder = tree.getKeysInReverseOrder();
        assertEquals(3, reverseInOrder.size());
        assertEquals(20, reverseInOrder.get(0));
        assertEquals(10, reverseInOrder.get(1));
        assertEquals(5, reverseInOrder.get(2));
    }
}
