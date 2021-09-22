package deque;

import java.util.Iterator;

public class ArrayDeque<T> implements Iterable<T>, Deque<T>{
    private T[] items;
    private int nextFirst;
    private int nextLast;
    private int size;

    // Default constructor
    public ArrayDeque() {
        items = (T[])new Object[8];
        size = 0;
        nextFirst = 3;
        nextLast = 4;
    }

    // Resizes array when there is not enough room left
    public void resize(int newSize) {
        T[] newItems = (T[])new Object[newSize];
        int start = newSize / 4;
        for(int i = 0; i < size; i++) {
            newItems[start + i] = get(i);
        }
        items = newItems;
        nextFirst = newSize / 4 - 1;
        nextLast = size + (size / 2);
    }

    // Shrinks underlying array length when removing elements, if necessary
    public void downscale() {
        if(items.length > 16 && (double)size / items.length < 0.25){
            resize(items.length / 2);
        }
    }

    // Adds element to start of array
    @Override
    public void addFirst(T item) {
        if(size == items.length) {
            resize(size * 2);
        }
        items[nextFirst] = item;
        nextFirst --;
        nextFirst = Math.floorMod(nextFirst, items.length);
        size++;
    }

    // Adds element to end of array
    @Override
    public void addLast(T item) {
        if(size == items.length) {
            resize(size * 2);
        }
        items[nextLast] = item;
        nextLast++;
        nextLast = Math.floorMod(nextLast, items.length);
        size++;
    }

    // Returns number of items currently being stored in the array.
    @Override
    public int size() {
        return size;
    }

    // Prints each item in the array, starting from first element and stopping at the last.
    @Override
    public void printDeque() {
        String str = "";
        int start = Math.floorMod(nextFirst + 1, items.length);
        for(int i = 0; i < size; i++) {
            str += items[Math.floorMod(start + i, items.length)] + " ";
        }
        str = str.trim();
        System.out.println(str);
    }

    // Removes first item in the array
    @Override
    public T removeFirst() {
        T returnItem = items[Math.floorMod(nextFirst + 1, items.length)];
        nextFirst = Math.floorMod(nextFirst + 1, items.length);
        size--;
        downscale();
        return returnItem;
    }

    // Removes last item in the array
    @Override
    public T removeLast() {
        T returnItem = items[Math.floorMod(nextLast - 1, items.length)];
        nextLast = Math.floorMod(nextLast - 1, items.length);
        size--;
        return returnItem;
    }

    // Gets the item at the specified index
    @Override
    public T get(int index) {
        if(index > size) return null;
        return items[Math.floorMod(nextFirst + 1 + index, items.length)];
    }

    //Iterator
    public Iterator<T> iterator() {
        return new ArrayDequeIterator();
    }

    private class ArrayDequeIterator implements Iterator<T> {
        private int index = 0;

        public T next() {
            T toReturn = items[Math.floorMod(nextFirst + 1 + index, items.length)];
            index++;
            return toReturn;
        }

        public boolean hasNext() {
            return index < size;
        }
    }

    // Compares self to other object, returns TRUE if equal and FALSE if unequal
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (!(other instanceof ArrayDeque)) {
            return false;
        }
        ArrayDeque<T> otherList = (ArrayDeque<T>) other;
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
