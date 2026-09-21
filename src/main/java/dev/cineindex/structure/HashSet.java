package dev.cineindex.structure;

/**
 * A custom HashSet implementation backed by HashTable.
 * Provides expected O(1) time complexity for basic operations like add and contains.
 */
public class HashSet<E> {

    private HashTable<E, Object> map;
    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    public HashSet() {
        map = new HashTable<>();
    }

    /**
     * Adds the specified element to this set if it is not already present.
     * Time Complexity: O(1) expected
     *
     * @return true if this set did not already contain the specified element
     */
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    /**
     * Removes the specified element from this set if it is present.
     * Time Complexity: O(1) expected
     *
     * @return true if the set contained the specified element
     */
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    /**
     * Returns true if this set contains the specified element.
     * Time Complexity: O(1) expected
     */
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    /**
     * Returns the number of elements in this set.
     * Time Complexity: O(1)
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns true if this set contains no elements.
     * Time Complexity: O(1)
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Removes all of the elements from this set.
     * Time Complexity: O(1)
     */
    public void clear() {
        map.clear();
    }

    /**
     * Returns a list containing all of the elements in this set.
     * Time Complexity: O(N + capacity)
     */
    public LinkedSequence<E> toList() {
        return map.keys();
    }
}
