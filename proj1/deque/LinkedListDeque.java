package deque;

import java.util.Iterator;

public class LinkedListDeque<T> implements Iterable<T>, Deque<T> {

    /* Declaring Node class that makes up the list */
    private static class Node<T> {
        private T item;
        private Node next;
        private Node last;

        Node(T item, Node next, Node last) {
            this.item = item;
            this.next = next;
            this.last = last;
        }

    }

    /* Declaring sentinel, dummy node that holds first and last items of the list */
    private Node sentinel;
    private int size;

    /* Constructors */
    public LinkedListDeque() {
        sentinel = new Node(null, null, null);
        sentinel.next = sentinel;
        sentinel.last = sentinel;
        size = 0;
    }


    /* Add to front and back of list */
    @Override
    public void addFirst(T item) {
        Node t = new Node(item, sentinel.next, sentinel);
        sentinel.next.last = t;
        sentinel.next = t;
        size++;
    }

    @Override
    public void addLast(T item) {
        Node t = new Node(item, sentinel, sentinel.last);
        sentinel.last.next = t;
        sentinel.last = t;
        size++;
    }

    @Override
    public int size() {
        return size;
    }

    /* Prints items in the deque from front to back */
    @Override
    public void printDeque() {
        String str = "";
        Node p = sentinel.next;
        while (p != sentinel) {
            str += p.item + "  ";
            p = p.next;
        }
        str = str.trim();
        System.out.println(str);
    }

    /* Removes first item from list */
    @Override
    public T removeFirst() {
        if (size == 0) {
            return null;
        }
        Node p = sentinel.next;
        sentinel.next = sentinel.next.next;
        sentinel.next.last = sentinel;
        size--;
        return (T) p.item;
    }

    /* Removes last item from list */
    @Override
    public T removeLast() {
        if (size == 0) {
            return null;
        }
        Node p = sentinel.last;
        sentinel.last = sentinel.last.last;
        sentinel.last.next = sentinel;
        size--;
        return (T) p.item;
    }

    /* Returns item at given index */
    @Override
    public T get(int index) {
        int current = 0;
        Node p = sentinel.next;
        while (current < index) {
            p = p.next;
            current++;
        }
        return current == index ? (T) p.item :  null;
    }

    /* Returns item at given index... but recursively */
    public T getRecursive(int index) {
        Node p = sentinel.next;
        return (T) getRecursiveHelper(p, index).item;
    }

    private Node getRecursiveHelper(Node p, int index) {
        if (index == 0){
            return p;
         } else {
            return getRecursiveHelper(p.next, index - 1);
        }
    }

    //Iterator
    public Iterator<T> iterator() {
        return new LinkedListIterator();
    }

    private class LinkedListIterator implements Iterator<T> {
        private Node p = sentinel.next;

        public T next() {
            T toReturn = (T) p.item;
            p = p.next;
            return toReturn;
        }

        public boolean hasNext() {
            return p != sentinel;
        }
    }

    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (!(other instanceof Deque)) {
            return false;
        }
        Deque<T> otherList = (Deque<T>) other;
        if (this.size() != otherList.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (!this.get(i).equals(otherList.get(i))) {
                return false;
            }
        }
        return true;
    }
}
