package dev.cineindex.structure;

/**
 * A custom AVL Tree (Balanced Binary Search Tree).
 * This structure maps a Comparable key to a list of values, accommodating duplicate keys.
 * Provides O(log N) time complexity for insertion and search.
 */
public class AvlTree<K extends Comparable<K>, V> {

    private class Node {
        K key;
        LinkedSequence<V> values;
        int height;
        Node left;
        Node right;

        Node(K key, V initialValue) {
            this.key = key;
            this.values = new LinkedSequence<>();
            this.values.add(initialValue);
            this.height = 1;
        }
    }

    /** Collect at most limit values without materialising every sorted key. */
    public LinkedSequence<V> valuesInOrder(int limit, boolean descending) {
        LinkedSequence<V> values = new LinkedSequence<>();
        collect(root, Math.max(0, limit), descending, values);
        return values;
    }

    private void collect(Node node, int limit, boolean descending, LinkedSequence<V> out) {
        if (node == null || out.size() >= limit) return;
        collect(descending ? node.right : node.left, limit, descending, out);
        for (V value : node.values) {
            if (out.size() >= limit) break;
            out.add(value);
        }
        collect(descending ? node.left : node.right, limit, descending, out);
    }

    private Node root;
    private int size; // Total number of values

    public AvlTree() {
        root = null;
        size = 0;
    }

    /**
     * Helper to get height of a node.
     * Time Complexity: O(1)
     */
    private int height(Node node) {
        return node == null ? 0 : node.height;
    }

    /**
     * Helper to get balance factor of a node.
     * Time Complexity: O(1)
     */
    private int getBalance(Node node) {
        return node == null ? 0 : height(node.left) - height(node.right);
    }

    /**
     * Right rotate subtree rooted with y.
     * Time Complexity: O(1)
     */
    private Node rightRotate(Node y) {
        Node x = y.left;
        Node T2 = x.right;

        // Perform rotation
        x.right = y;
        y.left = T2;

        // Update heights
        y.height = Math.max(height(y.left), height(y.right)) + 1;
        x.height = Math.max(height(x.left), height(x.right)) + 1;

        return x;
    }

    /**
     * Left rotate subtree rooted with x.
     * Time Complexity: O(1)
     */
    private Node leftRotate(Node x) {
        Node y = x.right;
        Node T2 = y.left;

        // Perform rotation
        y.left = x;
        x.right = T2;

        // Update heights
        x.height = Math.max(height(x.left), height(x.right)) + 1;
        y.height = Math.max(height(y.left), height(y.right)) + 1;

        return y;
    }

    /**
     * Inserts a key-value pair into the AVL tree.
     * Time Complexity: O(log N) where N is the number of distinct keys.
     */
    public void put(K key, V value) {
        root = insert(root, key, value);
        size++;
    }

    private Node insert(Node node, K key, V value) {
        // 1. Perform normal BST insertion
        if (node == null) {
            return new Node(key, value);
        }

        int cmp = key.compareTo(node.key);

        if (cmp < 0) {
            node.left = insert(node.left, key, value);
        } else if (cmp > 0) {
            node.right = insert(node.right, key, value);
        } else {
            // Duplicate keys are handled by adding to the values list
            node.values.add(value);
            return node;
        }

        // 2. Update height of this ancestor node
        node.height = 1 + Math.max(height(node.left), height(node.right));

        // 3. Get the balance factor of this ancestor node to check whether it became unbalanced
        int balance = getBalance(node);

        // If this node becomes unbalanced, then there are 4 cases:

        // Left Left Case
        if (balance > 1 && key.compareTo(node.left.key) < 0) {
            return rightRotate(node);
        }

        // Right Right Case
        if (balance < -1 && key.compareTo(node.right.key) > 0) {
            return leftRotate(node);
        }

        // Left Right Case
        if (balance > 1 && key.compareTo(node.left.key) > 0) {
            node.left = leftRotate(node.left);
            return rightRotate(node);
        }

        // Right Left Case
        if (balance < -1 && key.compareTo(node.right.key) < 0) {
            node.right = rightRotate(node.right);
            return leftRotate(node);
        }

        // return the (unchanged) node pointer
        return node;
    }

    /**
     * Searches for a key and returns the list of values associated with it.
     * Time Complexity: O(log N)
     *
     * @return The list of values, or null if the key is not found.
     */
    public LinkedSequence<V> get(K key) {
        Node result = search(root, key);
        return result != null ? result.values : null;
    }

    private Node search(Node node, K key) {
        if (node == null) {
            return null;
        }
        int cmp = key.compareTo(node.key);
        if (cmp == 0) {
            return node;
        }
        if (cmp < 0) {
            return search(node.left, key);
        } else {
            return search(node.right, key);
        }
    }

    /**
     * Performs an in-order traversal to fetch all keys sorted.
     * Time Complexity: O(N) where N is the number of distinct keys.
     */
    public LinkedSequence<K> getKeysInOrder() {
        LinkedSequence<K> list = new LinkedSequence<>();
        inOrderTraversal(root, list);
        return list;
    }

    private void inOrderTraversal(Node node, LinkedSequence<K> list) {
        if (node != null) {
            inOrderTraversal(node.left, list);
            list.add(node.key);
            inOrderTraversal(node.right, list);
        }
    }

    /**
     * Performs a reverse in-order traversal to fetch all keys sorted in descending order.
     * Time Complexity: O(N) where N is the number of distinct keys.
     */
    public LinkedSequence<K> getKeysInReverseOrder() {
        LinkedSequence<K> list = new LinkedSequence<>();
        reverseInOrderTraversal(root, list);
        return list;
    }

    private void reverseInOrderTraversal(Node node, LinkedSequence<K> list) {
        if (node != null) {
            reverseInOrderTraversal(node.right, list);
            list.add(node.key);
            reverseInOrderTraversal(node.left, list);
        }
    }

    /**
     * Returns total number of values stored in the tree.
     * Time Complexity: O(1)
     */
    public int size() {
        return size;
    }

    /**
     * Checks if the tree is empty.
     * Time Complexity: O(1)
     */
    public boolean isEmpty() {
        return root == null;
    }
}
