package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author Adam Woods
 */
public class MyHashMap<K, V> implements Map61B<K, V> {

    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables */
    private Collection<Node>[] buckets;
    private int size;
    private int numItems;
    private final double loadFactor;
    // You should probably define some more!

    /** Constructors */
    public MyHashMap() {
        size = 16;
        buckets = createTable(size);
        numItems = 0;
        loadFactor = 0.75;
    }

    public MyHashMap(int initialSize) {
        size = initialSize;
        buckets = createTable(size);
        numItems = 0;
        loadFactor = 0.75;
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        size = initialSize;
        buckets = createTable(size);
        numItems = 0;
        loadFactor = maxLoad;
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     */
    protected Collection<Node> createBucket() {
        return new LinkedList<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        return new Collection[tableSize];
    }

    // TODO: Implement the methods of the Map61B Interface below
    // Your code won't compile until you do so!

    public int hash(K key) {
        return Math.floorMod(key.hashCode(), size);
    }

    public void clear() {
        for(int i = 0; i < size; i++) {
            buckets[i] = null;
            numItems = 0;

        }
    }

    public boolean containsKey(K key) {
        if(key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        return get(key) != null;
    }

    public V get(K key) {
        if(key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        int i = hash(key);
        if(buckets[i] == null) {
            return null;
        }
        for(Node item : buckets[i]) {
            if(key.equals(item.key)) {
                return item.value;
            }
        }
        return null;
    }

    public int size() {
        return numItems;
    }

    public void put(K key, V value) {
        if(key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        int i = hash(key);
        if(get(key) != null && buckets[i] != null) {
            for(Node item : buckets[i]) {
                if(key.equals(item.key)) {
                    item.value = value;
                }
            }
        } else {
            buckets[i] = createBucket();
            buckets[i].add(new Node(key, value));
            numItems++;
            if((double) numItems / (double) size > loadFactor) {
                resize();
            }
        }
    }
    public int totalSize() {
        return size;
    }

    public void resize() {
        Collection<Node>[] b = new Collection[size * 2];
        size *= 2;
        for(int i = 0; i < size / 2; i++) {
            if(buckets[i] != null) {
                for(Node n : buckets[i]) {
                    int j = hash(n.key);
                    if(b[j] == null) {
                        b[j] = createBucket();
                    }
                    b[j].add(n);
                }
            }
        }
        buckets = b;
    }

    public Set<K> keySet() {
        HashSet<K> keys = new HashSet<>();
        for(int i = 0; i < size; i++) {
            if(buckets[i] != null) {
                for(Node item : buckets[i]) {
                    keys.add(item.key);
                }
            }
        }
        return keys;
    }

    public Iterator<K> iterator() {
        return new MyHashMapIterator();
    }

    private class MyHashMapIterator implements Iterator<K> {
        private int pos;
        private int i;


        public MyHashMapIterator() {
            pos = 0;
        }

        public boolean hasNext() {
            return pos < numItems;
        }

        public K next() {
            for(Node n : buckets[i]) {
                return n.key;
            }
            i++;
            return null;
        }
    }

    public V remove(K key) {
        throw new UnsupportedOperationException("Not supported.");
    }

    public V remove(K key, V value) {
        throw new UnsupportedOperationException("Not supported.");
    }

}
