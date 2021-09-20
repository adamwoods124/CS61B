package deque;

import org.checkerframework.checker.units.qual.A;
import org.junit.Test;

import java.lang.reflect.Array;

import static org.junit.Assert.*;


/** Performs some basic linked list tests. */
public class ArrayDequeTest {

    @Test
    public void addIsEmptySizeTest() {
        ArrayDeque<Integer> list1 = new ArrayDeque<>();

        for(int i = 0; i < 10; i++) {
            list1.addLast(i);
        }
    }

    @Test
    public void equalsTest() {
        ArrayDeque<Integer> list1 = new ArrayDeque<>();
        ArrayDeque<Integer> list2 = new ArrayDeque<>();
        ArrayDeque<Integer> list3 = new ArrayDeque<>();
        ArrayDeque<Integer> list4 = new ArrayDeque<>();

        for(int i = 0; i < 100; i++){
            list1.addLast(i);
            list2.addLast(i);
            list3.addLast(i);
            list4.addLast(i);
        }
        list4.removeLast();



        assertEquals(true, list1.equals(list2));
        assertEquals(true, list2.equals(list1));
        assertEquals(false, list3.equals(list4));
        assertEquals(false, list4.equals(list3));
    }
}
