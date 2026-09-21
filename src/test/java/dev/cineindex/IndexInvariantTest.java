package dev.cineindex;

import dev.cineindex.structure.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndexInvariantTest {
    private record Collision(int id) { public int hashCode() { return -7; } }

    @Test void collidingKeysSurviveResizeReplacementAndRemoval() {
        HashTable<Collision, Integer> map = new HashTable<>();
        for (int i = 0; i < 500; i++) map.put(new Collision(i), i);
        for (int i = 0; i < 500; i += 2) assertEquals(i, map.remove(new Collision(i)));
        for (int i = 1; i < 500; i += 2) assertEquals(i, map.get(new Collision(i)));
        assertEquals(250, map.size());
    }

    @Test void nullValuesDoNotHidePresentKeys() {
        HashTable<String, String> map = new HashTable<>();
        map.put(null, null); map.put("present", null);
        assertTrue(map.containsKey(null)); assertTrue(map.containsKey("present"));
        assertFalse(map.containsKey("absent"));
        map.remove(null); assertFalse(map.containsKey(null));
    }

    @Test void randomMapOperationsMatchJdkReference() {
        HashTable<Integer, Integer> actual = new HashTable<>();
        Map<Integer, Integer> expected = new HashMap<>();
        Random random = new Random(126);
        for (int i = 0; i < 20_000; i++) {
            int key = random.nextInt(1500) - 750;
            switch (random.nextInt(3)) {
                case 0 -> { Integer value = random.nextBoolean() ? null : i; assertEquals(expected.put(key, value), actual.put(key, value)); }
                case 1 -> assertEquals(expected.remove(key), actual.remove(key));
                default -> { assertEquals(expected.get(key), actual.get(key)); assertEquals(expected.containsKey(key), actual.containsKey(key)); }
            }
            assertEquals(expected.size(), actual.size());
        }
    }

    @Test void avlHandlesOrderedInputAndBoundedDuplicateTraversal() {
        AvlTree<Integer, Integer> tree = new AvlTree<>();
        for (int i = 0; i < 20_000; i++) tree.put(i, i);
        tree.put(19_999, -1);
        assertArrayEquals(new Integer[]{19_999, -1, 19_998}, tree.valuesInOrder(3, true).toArray(new Integer[0]));
        assertArrayEquals(new Integer[]{0, 1, 2}, tree.valuesInOrder(3, false).toArray(new Integer[0]));
        assertTrue(tree.valuesInOrder(-1, true).isEmpty());
        for (int i = 0; i < 20_000; i++) assertEquals(i, tree.get(i).get(0));
    }

    @Test void avlOrderMatchesReferenceForRandomDuplicateKeys() {
        AvlTree<Integer, Integer> tree = new AvlTree<>();
        TreeMap<Integer, List<Integer>> reference = new TreeMap<>();
        Random random = new Random(42);
        for (int i = 0; i < 5000; i++) {
            int key = random.nextInt(300);
            tree.put(key, i); reference.computeIfAbsent(key, ignored -> new ArrayList<>()).add(i);
        }
        assertArrayEquals(reference.descendingMap().values().stream().flatMap(List::stream).toArray(Integer[]::new), tree.valuesInOrder(5000, true).toArray(new Integer[0]));
    }

    @Test void iteratorVisitsNullsAndThrowsAtEnd() {
        LinkedSequence<String> sequence = new LinkedSequence<>();
        sequence.add("first"); sequence.add(null); sequence.add("last");
        Iterator<String> iterator = sequence.iterator();
        assertEquals("first", iterator.next()); assertNull(iterator.next()); assertEquals("last", iterator.next());
        assertFalse(iterator.hasNext()); assertThrows(NoSuchElementException.class, iterator::next);
    }
}
