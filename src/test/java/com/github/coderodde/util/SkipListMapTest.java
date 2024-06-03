package com.github.coderodde.util;

import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class SkipListMapTest {

    private static final Comparator<Integer> CMP = new Comparator<>(){
        @Override
        public int compare(Integer o1, Integer o2) {
            return o1.compareTo(o2);
        }
    };
    
    @Test
    public void containsKey() {
        SkipListMap<Integer, String> list = new SkipListMap<>(CMP);
        
        for (int i = 0; i < 10; i++) {
            assertFalse(list.containsKey(i));
        }
        
        for (int i = 0; i < 10; i++) {
            assertNull(list.put(i, "Hello"));
        }
        
        for (int i = 0; i < 10; i++) {
            assertTrue(list.containsKey(i));
        }
    }
    
    @Test
    public void get() {
        SkipListMap<Integer, String> list = new SkipListMap<>(CMP);
        
        assertNull(list.put(1, "1"));
        assertNull(list.put(2, "2"));
        assertNull(list.put(3, "3"));
        assertNull(list.put(4, "4"));
        
        assertEquals("1", list.get(1));
        assertEquals("2", list.get(2));
        assertEquals("3", list.get(3));
        assertEquals("4", list.get(4));
        
        assertNull(list.get(0));
        assertNull(list.get(5));
    }
    
    @Test
    public void remove() {
        SkipListMap<Integer, String> list = new SkipListMap<>(CMP);
        
        for (int i : new int[]{ 1, 3, 5, 7 }) {
            list.put(i, Integer.toString(i));
        }
        
        assertNull(list.remove(0));
        assertNull(list.remove(2));
        assertNull(list.remove(4));
        assertNull(list.remove(6));
        assertNull(list.remove(8));
        
        for (int i : new int[]{ 1, 3, 5, 7 }) {
            assertEquals(Integer.toString(i), list.remove(i));
        }
    }
    
    @Test
    public void bruteForce() {
        SkipListMap<Integer, String> list1 = new SkipListMap<>(CMP);
        Map<Integer, String> list2 = new ConcurrentSkipListMap<>(CMP);
        
        for (int i = 0; i < 1000; i++) {
            assertFalse(list1.containsKey(i));
            assertNull(list1.get(i));
            
            assertFalse(list2.containsKey(i));
            assertNull(list2.get(i));
            
            assertNull(list1.put(i, Integer.toString(i)));
            assertNull(list2.put(i, Integer.toString(i)));
            
            assertTrue(list1.containsKey(i));
            assertEquals(Integer.toString(i), list1.get(i));
            assertTrue(list2.containsKey(i));
            assertEquals(Integer.toString(i), list2.get(i));
        }
        
        for (int i = -100; i < 0; i++) {
            assertNull(list1.get(i));
            assertFalse(list1.containsKey(i));
            
            assertNull(list2.get(i));
            assertFalse(list2.containsKey(i));
            
            assertNull(list1.remove(i));
            assertNull(list2.remove(i));
        }
        
        for (int i = 1000; i < 1100; i++) {
            assertNull(list1.get(i));
            assertFalse(list1.containsKey(i));
            
            assertNull(list2.get(i));
            assertFalse(list2.containsKey(i));
            
            assertNull(list1.remove(i));
            assertNull(list2.remove(i));
        }
        
        for (int i = 0; i < 200; i++) {
            assertEquals(Integer.toString(i), list1.remove(i));
            assertEquals(Integer.toString(i), list2.remove(i));
        }
    }
    
    @Test
    public void versatile() {
        Random rnd = new Random(10);
        Map<Integer, String> list1 = new SkipListMap<>(CMP);
        Map<Integer, String> list2 = new ConcurrentSkipListMap<>(CMP);
        
        for (int i = 0; i < 300; i++) {
            assertTrue(list1.equals(list2));
            
            int coin = rnd.nextInt(100);
            int key = rnd.nextInt(150);
            
            if (list1.isEmpty() || coin < 50) {
                list1.put(key, Integer.toString(key));
                list2.put(key, Integer.toString(key));
            } else if (coin < 70) {
                assertEquals(list2.remove(key), 
                             list1.remove(key));
            } else {
                assertEquals(list1.containsKey(key), 
                             list2.containsKey(key));
                
                assertEquals(list2.get(key), list1.get(key));
            }
        }
        
        assertTrue(list1.equals(list2));
    }
}