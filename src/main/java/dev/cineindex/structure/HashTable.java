package dev.cineindex.structure;

/**
 * A custom HashMap implementation using chaining for collision resolution.
 * Provides expected O(1) time complexity for basic operations like get and put.
 */
public class HashTable<K, V> {

    private static class Entry<K, V> {
        final K key;
        V value;
        Entry<K, V> next;

        Entry(K key, V value, Entry<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Entry<K, V>[] table;
    private int size;
    private static final int DEFAULT_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * Initializes the map with default capacity.
     */
    @SuppressWarnings("unchecked")
    public HashTable() {
        table = new Entry[DEFAULT_CAPACITY];
        size = 0;
    }

    /**
     * Internal hash function to compute the table index.
     * Time Complexity: O(1)
     */
    private int hash(Object key) {
        if (key == null) return 0;
        int h = key.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        h ^= (h >>> 7) ^ (h >>> 4);
        return Math.abs(h % table.length);
    }

    /**
     * Resizes the internal table when the load factor exceeds 0.75.
     * Time Complexity: O(N) where N is the current number of elements.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        Entry<K, V>[] oldTable = table;
        table = new Entry[oldTable.length * 2];
        size = 0; // reset size, put will increment

        for (int i = 0; i < oldTable.length; i++) {
            Entry<K, V> e = oldTable[i];
            while (e != null) {
                put(e.key, e.value);
                e = e.next;
            }
        }
    }

    /**
     * Associates the specified value with the specified key in this map.
     * Time Complexity: O(1) expected, O(N) worst case.
     */
    public V put(K key, V value) {
        if (size >= table.length * LOAD_FACTOR) {
            resize();
        }

        int index = hash(key);
        Entry<K, V> current = table[index];

        while (current != null) {
            if ((key == null && current.key == null) ||
                (key != null && key.equals(current.key))) {
                V oldValue = current.value;
                current.value = value;
                return oldValue;
            }
            current = current.next;
        }

        table[index] = new Entry<>(key, value, table[index]);
        size++;
        return null;
    }

    /**
     * Returns the value to which the specified key is mapped.
     * Time Complexity: O(1) expected, O(N) worst case.
     */
    public V get(Object key) {
        int index = hash(key);
        Entry<K, V> current = table[index];

        while (current != null) {
            if ((key == null && current.key == null) ||
                (key != null && key.equals(current.key))) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     * Time Complexity: O(1) expected, O(N) worst case.
     */
    public V remove(Object key) {
        int index = hash(key);
        Entry<K, V> current = table[index];
        Entry<K, V> prev = null;

        while (current != null) {
            if ((key == null && current.key == null) ||
                (key != null && key.equals(current.key))) {
                if (prev == null) {
                    table[index] = current.next;
                } else {
                    prev.next = current.next;
                }
                size--;
                return current.value;
            }
            prev = current;
            current = current.next;
        }
        return null;
    }

    /**
     * Returns true if this map contains a mapping for the specified key.
     * Time Complexity: O(1) expected, O(N) worst case.
     */
    public boolean containsKey(Object key) {
        Entry<K, V> entry = table[hash(key)];
        while (entry != null) {
            if (java.util.Objects.equals(key, entry.key)) return true;
            entry = entry.next;
        }
        return false;
    }

    /**
     * Returns the number of key-value mappings in this map.
     * Time Complexity: O(1)
     */
    public int size() {
        return size;
    }

    /**
     * Returns true if this map contains no key-value mappings.
     * Time Complexity: O(1)
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Removes all of the mappings from this map.
     * Time Complexity: O(1) assuming garbage collection handles the detached array quickly.
     */
    @SuppressWarnings("unchecked")
    public void clear() {
        table = new Entry[DEFAULT_CAPACITY];
        size = 0;
    }

    /**
     * Returns a list containing all the keys in the map.
     * Time Complexity: O(N + capacity)
     */
    public LinkedSequence<K> keys() {
        LinkedSequence<K> keyList = new LinkedSequence<>();
        for (int i = 0; i < table.length; i++) {
            Entry<K, V> e = table[i];
            while (e != null) {
                keyList.add(e.key);
                e = e.next;
            }
        }
        return keyList;
    }

    /**
     * Returns a list containing all the values in the map.
     * Time Complexity: O(N + capacity)
     */
    public LinkedSequence<V> values() {
        LinkedSequence<V> valueList = new LinkedSequence<>();
        for (int i = 0; i < table.length; i++) {
            Entry<K, V> e = table[i];
            while (e != null) {
                valueList.add(e.value);
                e = e.next;
            }
        }
        return valueList;
    }
}
