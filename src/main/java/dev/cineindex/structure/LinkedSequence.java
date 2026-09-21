package dev.cineindex.structure;

/**
 * A doubly linked sequence with linear-time iteration and bulk conversion.
 * O(1) for adding to the end, O(N) for searching by value or index.
 */
public class LinkedSequence<E> implements Iterable<E> {
    private class Node {
        E data;
        Node next;
        Node prev;

        Node(E data) {
            this.data = data;
        }
    }

    @Override
    public java.util.Iterator<E> iterator() {
        return new java.util.Iterator<>() {
            private Node current = head;
            public boolean hasNext() { return current != null; }
            public E next() {
                if (current == null) throw new java.util.NoSuchElementException();
                E value = current.data;
                current = current.next;
                return value;
            }
        };
    }

    private Node head;
    private Node tail;
    private int size;

    public LinkedSequence() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Adds element to the end of the list.
     * Time Complexity: O(1)
     */
    public boolean add(E element) {
        Node newNode = new Node(element);
        if (head == null) {
            head = tail = newNode;
        } else {
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }
        size++;
        return true;
    }

    /**
     * Clears the list.
     * Time Complexity: O(1)
     */
    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }

    /**
     * Checks if element is in the list.
     * Time Complexity: O(N)
     */
    public boolean contains(E element) {
        return indexOf(element) != -1;
    }

    /**
     * Checks if the list is empty.
     * Time Complexity: O(1)
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Removes the first occurrence of element.
     * Time Complexity: O(N)
     */
    public boolean remove(E element) {
        Node current = head;
        while (current != null) {
            if ((element == null && current.data == null) ||
                (element != null && element.equals(current.data))) {
                if (current.prev != null) {
                    current.prev.next = current.next;
                } else {
                    head = current.next;
                }

                if (current.next != null) {
                    current.next.prev = current.prev;
                } else {
                    tail = current.prev;
                }
                size--;
                return true;
            }
            current = current.next;
        }
        return false;
    }

    /**
     * Returns the size.
     * Time Complexity: O(1)
     */
    public int size() {
        return size;
    }

    /**
     * Returns the element at position index.
     * Time Complexity: O(N)
     */
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        Node current;
        if (index < size / 2) {
            current = head;
            for (int i = 0; i < index; i++) {
                current = current.next;
            }
        } else {
            current = tail;
            for (int i = size - 1; i > index; i--) {
                current = current.prev;
            }
        }
        return current.data;
    }

    /**
     * Returns the index of the first occurrence of element.
     * Time Complexity: O(N)
     */
    public int indexOf(E element) {
        Node current = head;
        int index = 0;
        while (current != null) {
            if ((element == null && current.data == null) ||
                (element != null && element.equals(current.data))) {
                return index;
            }
            current = current.next;
            index++;
        }
        return -1;
    }

    /**
     * Sets the element at position index.
     * Time Complexity: O(N)
     */
    public E set(int index, E element) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }
        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        E oldData = current.data;
        current.data = element;
        return oldData;
    }

    /**
     * Converts list to array for fast iteration when necessary.
     * Time Complexity: O(N)
     */
    @SuppressWarnings("unchecked")
    public E[] toArray(E[] a) {
        if (a.length < size) {
            a = (E[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }
        int i = 0;
        Object[] result = a;
        for (Node x = head; x != null; x = x.next) {
            result[i++] = x.data;
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }
}
