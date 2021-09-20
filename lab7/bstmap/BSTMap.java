package bstmap;


import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class BSTMap<K extends Comparable, V> implements Map61B<K, V> {

    private Node<K, V> root;
    private int size;

    public BSTMap() {
        root = null;
    }

    public BSTMap(Node<K, V> n) {
        root = n;
    }
    public void clear() {
        if(root != null) {
            root.clearHelper();
        }
    }

    public boolean containsKey(K key) {
        if(root == null) {
            return false;
        }
        if(root.key == key) {
            return true;
        }
        return root.containsKeyHelper(key);
    }

    public V get(K key) {
        if(root == null) {
            return null;
        }
        if(root == key) {
            return root.value;
        }
        return root.getHelper(key);
    }

    public int size() {
        return size;
    }

    public void put(K putKey, V putValue) {
        if(root == null) {
            root = new Node(putKey, putValue);
        }
        root.putHelper(putKey, putValue);
        size++;
    }

    public void print() {
        root.printInorder(root);
    }

    public Set keySet() {
        throw new UnsupportedOperationException("No KeySet method available for this class.");
    }

    public V remove(K key) {
        throw new UnsupportedOperationException("No remove method available for this class.");
    }

    public V remove(K key, V value) {
        throw new UnsupportedOperationException("No remove method available for this class");
    }

    public Iterator iterator() {
        throw new UnsupportedOperationException("No iterator available for this class.");
    }

    private static class Node<K extends Comparable, V> {
        private K key;
        private V value;
        private Node right;
        private Node left;

        private Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.right = null;
            this.left = null;
        }

        private Node(K key, V value, Node<K, V> left, Node<K, V> right) {
            this.key = key;
            this.value = value;
            this.right = right;
            this.left = left;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public Node<K, V> getRight() {
            return right;
        }

        public Node<K, V> getLeft() {
            return left;
        }

        public boolean containsKeyHelper(Object findKey) {
            if(right == null && left == null) {
                return false;
            }
            if(findKey.equals(key)) {
                return true;
            }
            return left.containsKeyHelper(findKey) || right.containsKeyHelper(findKey);
        }

        public void putHelper(K putKey, V putValue) {
            if(putKey.compareTo(key) > 0) {
                if(right == null) {
                    right = new Node(putKey, putValue);
                } else {
                    right.putHelper(putKey, putValue);
                }
            }
            if(putKey.compareTo(key) < 0) {
                if(left == null) {
                    left = new Node(putKey, putValue);
                } else {
                    left.putHelper(putKey, putValue);
                }
            }
        }

        public V getHelper(K getKey) {
            if(getKey.compareTo(key) > 0 && left != null) {
                right.getHelper(getKey);
            }
            if(getKey.compareTo(key) < 0 && right != null) {
                left.getHelper(getKey);
            }
            return value;
        }

        public void clearHelper() {
            value = null;
            key = null;
            if(right != null) {
                right.clearHelper();
            }
            if(left != null) {
                left.clearHelper();
            }
        }

        public void printInorder(Node n) {
            if(n == null) {
                return;
            }
            printInorder(n.left);
            System.out.println(n.value);
            printInorder(n.right);
        }
    }

    public static void main(String[] args) {
        BSTMap<String, Integer> b = new BSTMap<String, Integer>();
        for (int i = 0; i < 455; i++) {
            b.put("hi" + i, 1+i);
            //make sure put is working via containsKey and get

        }

        b.print();
    }
}
