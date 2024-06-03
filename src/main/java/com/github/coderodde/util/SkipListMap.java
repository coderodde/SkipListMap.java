package com.github.coderodde.util;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

/**
 * This class implements a map sorted by keys using skip lists.
 * 
 * @version 1.0.0 (May 27, 2024)
 * @since 1.0.0 (May 27, 2024)
 */
public final class SkipListMap<K, V> extends AbstractMap<K, V>
                                     implements Map<K, V> {
    
    private static final class Node<K,V> {
        final K key;
        V val;
        Node<K,V> next;
        Node(K key, V value, Node<K,V> next) {
            this.key = key;
            this.val = value;
            this.next = next;
        }
    }
    
    private static final class Index<K,V> {
        final Node<K,V> node;
        final Index<K,V> down;
        Index<K,V> right;
        Index(Node<K,V> node, Index<K,V> down, Index<K,V> right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }
    }
    
    private final class SkipListIterator implements Iterator<K> {

        private Node<K, V> node = head != null ? head.node.next : null;
        private int iterated;
        
        @Override
        public boolean hasNext() {
            return iterated < size;
        }

        @Override
        public K next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            K ret = node.key;
            node = node.next;
            iterated++;
            return ret;
        }
    }

    private Index<K,V> head;
    private int size;
    private final Random random = new Random(13);
    private final Comparator<K> comparator;
    
    public SkipListMap(Comparator<K> comparator) {
        this.comparator = comparator;
    }
    
    public SkipListMap() {
        this.comparator = null;
    }
    
    @Override
    public V get(Object key) {
        return doGet(key);
    }
    
    @Override
    public boolean containsKey(Object key) {
        return doGet(key) != null;
    }
    
    @Override
    public V put(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        
        if (key == null) {
            throw new NullPointerException();
        }
        
        Comparator<? super K> cmp = comparator;
        
        for (;;) {
            Index<K,V> h; Node<K,V> b;
            int levels = 0;               
            
            if ((h = head) == null) {     
                Node<K,V> base = new Node<>(null, null, null);
                h = new Index<>(base, null, null);
                
                if (head == null) {
                    head = h;
                    b = base;
                } else {
                    b = null;
                }
            } else {
                for (Index<K,V> q = h, r, d;;) { 
                    while ((r = q.right) != null) {
                        Node<K,V> p;
                        K k = null;
                        
                        if ((p = r.node) == null 
                                || (k = p.key) == null 
                                || p.val == null) {
                        
                            if (q.right == r) {
                                q.right = r.right;
                            }
                        } else if (cpr(cmp, key, k) > 0) {
                            q = r;
                        } else {
                            break;
                        }
                    }
                    
                    if ((d = q.down) != null) {
                        ++levels;
                        q = d;
                    } else {
                        b = q.node;
                        break;
                    }
                }
            }
            
            if (b != null) {
                Node<K,V> z = null;
                
                for (;;) {                       
                    Node<K,V> n, p; 
                    K k; 
                    V v; 
                    int c;
                    
                    if ((n = b.next) == null) {
                        c = -1;
                    } else if ((k = n.key) == null) {
                        break;
                    } else if ((v = n.val) == null) {
                        unlinkNode(b, n);
                        c = 1;
                    } else if ((c = cpr(cmp, key, k)) > 0) {
                        b = n;
                    } else if (c == 0) {
                        if (n.val == v) {
                            n.val = value;
                            return v;
                        }
                    }
                    
                    if (c < 0) {
                        if (b.next == n) {
                            b.next = p = new Node<>(key, value, n);
                            z = p;
                            break;
                        }
                    }
                }

                if (z != null) {
                    long rnd = random.nextLong();
                    
                    if ((rnd & 0x3) == 0) {       // add indices with 1/4 prob
                        int skips = levels;    
                        Index<K,V> x = null;
                    
                        for (;;) {               
                            x = new Index<>(z, x, null);
                            
                            if (rnd >= 0L || --skips < 0) {
                                break;
                            } else {
                                rnd <<= 1;
                            }
                        }
                        
                        if (addIndices(h, skips, x, cmp) 
                                && skips < 0 
                                && head == h) {   
                            
                            Index<K,V> hx = new Index<>(z, x, null);
                            Index<K,V> nh = new Index<>(h.node, h, hx);
                            
                            if (head == h) {
                                head = nh;
                            }
                        }
                        
                        if (z.val == null) {
                            findPredecessor(key, cmp);
                        }
                    }
                    
                    size++;
                    return null;
                }
            }
        }
    }
    
    @Override
    public V remove(Object key) {
        return doRemove(key, null);
    }
    
    @Override
    public int size() {
        return size;
    }
    
    @Override
    public boolean isEmpty() {
        return size == 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof Map)) {
            return false;
        }
        
        Map<K, V> otherMap = (Map<K, V>) o;
        
        if (size != otherMap.size()) {
            return false;
        }
        
        Iterator<K> iter1 = otherMap.keySet().iterator();
        Iterator<K> iter2 = new SkipListIterator();
        
        for (;;) {
            boolean b1 = iter1.hasNext();
            boolean b2 = iter2.hasNext();
            
            if (b1 && !b2) {
                return false;
            }
            
            if (!b1 && b2) {
                return false;
            }
            
            if (!b1 && !b2) {
                return true;
            }
            
            K key1 = iter1.next();
            K key2 = iter2.next();
            
            if (!key1.equals(key2)) {
                return false;
            }
        }
    }
    
    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private V doGet(Object key) {
        Index<K,V> q;
        
        if (key == null) {
            throw new NullPointerException();
        }
        
        V result = null;
        
        if ((q = head) != null) {
            outer: 
            for (Index<K,V> r, d;;) {
                while ((r = q.right) != null) {
                    Node<K,V> p; 
                    K k; 
                    V v; 
                    int c;
                    
                    if ((p = r.node) == null 
                            || (k = p.key) == null 
                            || (v = p.val) == null) {
                        
                        if (q.right == r) {
                            q.right = r.right;
                        }
                    } else if ((c = cpr(comparator, key, k)) > 0) {
                        q = r;
                    } else if (c == 0) {
                        result = v;
                        break outer;
                    } else {
                        break;
                    }
                }
                
                if ((d = q.down) != null) {
                    q = d;
                } else {
                    Node<K,V> b, n;
                    
                    if ((b = q.node) != null) {
                        while ((n = b.next) != null) {
                            V v; 
                            int c;
                            K k = n.key;
                            
                            if ((v = n.val) == null 
                                    || k == null 
                                    || (c = cpr(comparator, key, k)) > 0) {
                                b = n;
                            } else {
                                if (c == 0) {
                                    result = v;
                                }
                                    
                                break;
                            }
                        }
                    }
                    
                    break;
                }
            }
        }
        return result;
    }
    
    private Node<K,V> findPredecessor(Object key, Comparator<? super K> cmp) {
        Index<K,V> q;
        
        if ((q = head) == null || key == null) {
            return null;
        } else {
            for (Index<K,V> r, d;;) {
                while ((r = q.right) != null) {
                    Node<K,V> p; 
                    K k = null;
                    
                    if ((p = r.node) == null 
                            || (k = p.key) == null 
                            || p.val == null) {
                        
                        if (q.right == r) {
                            q.right = r.right;
                        }
                    } else if (cpr(cmp, key, k) > 0) {
                        q = r;
                    } else {
                        break;
                    }
                }
                
                if ((d = q.down) != null) {
                    q = d;
                } else {
                    return q.node;
                }
            }
        }
    }

    static <K,V> boolean addIndices(Index<K,V> q, int skips, Index<K,V> x,
                                    Comparator<? super K> cmp) {
        Node<K,V> z; 
        K key;
        
        if (x != null
                && (z = x.node) != null 
                && (key = z.key) != null 
                &&q != null) {                     
            
            boolean retrying = false;
            
            for (;;) {
                Index<K,V> r, d; 
                int c;
                
                if ((r = q.right) != null) {
                    Node<K,V> p; 
                    K k;
                    
                    if ((p = r.node) == null 
                            || (k = p.key) == null 
                            || p.val == null) {
                        
                        if (q.right == r) {
                            q.right = r.right;
                        }
                        
                        c = 0;
                    } else if ((c = cpr(cmp, key, k)) > 0) {
                        q = r;
                    } else if (c == 0) {
                        break;          
                    }
                } else {
                    c = -1;
                }

                if (c < 0) {
                    if ((d = q.down) != null && skips > 0) {
                        --skips;
                        q = d;
                    } else if (d != null 
                                && !retrying 
                                && !addIndices(d, 0, x.down, cmp)) {
                        break;
                    } else {
                        x.right = r;
                        
                        if (q.right == r) {
                            q.right = x;
                            return true;
                        } else {
                            retrying = true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    private static <K,V> void unlinkNode(Node<K,V> b, Node<K,V> n) {
        if (b != null && n != null) {
            Node<K,V> f, p;
            
            for (;;) {
                if ((f = n.next) != null && f.key == null) {
                    p = f.next;
                    break;
                } else if (n.next == f) {
                    n.next = new Node<>(null, null, f);
                    p = f;
                    break;
                }
            }
            
            if (b.next == n) {
                b.next = p;
            }
        }
    }
    
    private final V doRemove(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }
        
        Comparator<? super K> cmp = comparator;
        V result = null;
        Node<K,V> b;
        
        outer: 
        while ((b = findPredecessor(key, cmp)) != null && result == null) {
            for (;;) {
                Node<K,V> n; 
                K k;
                V v; 
                int c;
                
                if ((n = b.next) == null) {
                    break outer;
                } else if ((k = n.key) == null) {
                    break;
                } else if ((v = n.val) == null) {
                    unlinkNode(b, n);
                } else if ((c = cpr(cmp, key, k)) > 0) {
                    b = n;
                } else if (c < 0) {
                    break outer;
                } else if (value != null && !value.equals(v)) {
                    break outer;
                } else if (n.val == v) {
                    n.val = null;
                    result = v;
                    unlinkNode(b, n);
                    break;
                }
            }
        }
        
        if (result != null) {
            tryReduceLevel();
            size--;
        }
        
        return result;
    }
 
    private void tryReduceLevel() {
        Index<K,V> h, d, e;
        if ((h = head) != null && h.right == null &&
            (d = h.down) != null && d.right == null &&
            (e = d.down) != null && e.right == null) {
            
            if (head == h) {
                head = d;
            } else {
                return;
            }
            
            if (h.right != null) {
                if (head == d) {
                    head = h;
                }
            }
        }
    }
 
    @SuppressWarnings({"unchecked", "rawtypes"})
    static int cpr(Comparator c, Object x, Object y) {
        return (c != null) ? c.compare(x, y) : ((Comparable)x).compareTo(y);
    }
    
    public static void main(String[] args) {
        SkipListMap<Integer, String> l =
                new SkipListMap<>(new Comparator<Integer>(){
                    
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        
        System.out.println("a");
        l.put(1, "one");
        System.out.println("b");
        l.put(2, "two");
        System.out.println("c");
        
        System.out.println(l.containsKey(1));
        System.out.println(l.containsKey(2));
        System.out.println(l.containsKey(3));
    }
}