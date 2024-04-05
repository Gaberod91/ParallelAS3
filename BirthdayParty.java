import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class BirthdayParty {
    static final int THREAD_COUNT = 4;
    static final int NUM_GUESTS = 500_000;

    static final int TASK_ADD_PRESENT = 0;
    static final int TASK_WRITE_CARD = 1;
    static final int TASK_SEARCH_FOR_PRESENT = 2;

    static final Lock mutex = new ReentrantLock();

    static Set<Integer> generateSet(int size) {
        Set<Integer> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
        for (int i = 0; i < size; i++) {
            set.add(i);
        }
        return set;
    }

    static void completeTask(ConcurrentLinkedQueue<Integer> list, Set<Integer> giftBag, Set<Integer> cards) {
        while (cards.size() < NUM_GUESTS) {
            int task = generateRandomNumber(0, 2);

            if (task == TASK_ADD_PRESENT) {
                if (!giftBag.isEmpty()) {
                    Integer num = giftBag.iterator().next();
                    if (giftBag.remove(num)) {
                        list.offer(num);
                    }
                }
            } else if (task == TASK_WRITE_CARD) {
                Integer guest;
                while ((guest = list.poll()) != null) {
                    cards.add(guest);
                }
            } else if (task == TASK_SEARCH_FOR_PRESENT) {
            }
        }
    }

    class Node {
        public int data;
        public Node next;
        public Node prev;
        public Lock mutex;
    
        public Node(int n) {
            this.data = n;
            this.next = null;
            this.prev = null;
            this.mutex = new ReentrantLock();
        }
    
        @Override
        public String toString() {
            return Integer.toString(data);
        }
    }
    
    public class ConcurrentLinkedList {
        private Node head;
        private Node tail;
        private int size;
        private final Lock mutex;
    
        public ConcurrentLinkedList() {
            this.head = null;
            this.tail = null;
            this.size = 0;
            this.mutex = new ReentrantLock();
        }
    
        public void remove(int key) {
            if (head == null) {
                return;
            }
    
            mutex.lock();
            try {
                Node curr = head;
    
                if (curr.data == key) {
                    head = head.next;
                    if (head != null) {
                        head.prev = null;
                    }
                    size--;
                    return;
                }
    
                while (curr.next != null) {
                    if (curr.next.data == key) {
                        curr.next = curr.next.next;
                        if (curr.next != null) {
                            curr.next.prev = curr;
                        }
                        size--;
                        return;
                    }
                    curr = curr.next;
                }
            } finally {
                mutex.unlock();
            }
        }
    
        public int removeHead() {
            mutex.lock();
            try {
                if (head == null) {
                    return Integer.MIN_VALUE;
                }
                int value = head.data;
                head = head.next;
                if (head != null) {
                    head.prev = null;
                }
                size--;
                return value;
            } finally {
                mutex.unlock();
            }
        }
    
        public int size() {
            return size;
        }
    
        public boolean isEmpty() {
            return head == null;
        }
    
        public void insert(int data) {
            mutex.lock();
            try {
                Node newNode = new Node(data);
    
                if (head == null) {
                    head = newNode;
                    tail = newNode;
                    size++;
                    return;
                }
    
                if (head.data >= newNode.data) {
                    newNode.next = head;
                    head.prev = newNode;
                    head = newNode;
                    size++;
                    return;
                }
    
                Node curr = head;
                while (curr.next != null && curr.next.data < newNode.data) {
                    curr = curr.next;
                }
    
                newNode.next = curr.next;
                if (curr.next != null) {
                    curr.next.prev = newNode;
                }
                curr.next = newNode;
                newNode.prev = curr;
    
                if (newNode.next == null) {
                    tail = newNode;
                }
    
                size++;
            } finally {
                mutex.unlock();
            }
        }
    
        public boolean contains(int key) {
            mutex.lock();
            try {
                Node temp = head;
                while (temp != null) {
                    if (temp.data == key) {
                        return true;
                    }
                    temp = temp.next;
                }
                return false;
            } finally {
                mutex.unlock();
            }
        }
    
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Node temp = head;
            while (temp != null) {
                sb.append(temp.data).append(" -> ");
                temp = temp.next;
            }
            sb.append("null");
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        ConcurrentLinkedQueue<Integer> list = new ConcurrentLinkedQueue<>();
        Set<Integer> cards = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Thread[] threads = new Thread[THREAD_COUNT];

        System.out.println("Generating " + NUM_GUESTS + " numbers...");
        Set<Integer> giftBag = generateSet(NUM_GUESTS);

        for (int i = 0; i < THREAD_COUNT; i++) {
            threads[i] = new Thread(() -> completeTask(list, giftBag, cards));
            threads[i].start();
        }

        System.out.println("Running " + THREAD_COUNT + " threads...");
        long start = System.currentTimeMillis();

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long end = System.currentTimeMillis();
        long duration = end - start;

        System.out.println("Finished in " + duration + "ms");
    }

    static int generateRandomNumber(int min, int max) {
        return (int) (Math.random() * (max - min + 1) + min);
    }
}