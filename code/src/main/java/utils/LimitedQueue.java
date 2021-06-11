package utils;

import java.io.Serializable;
import java.util.*;

public class LimitedQueue<E> implements Serializable {
    private int limit;
    private Queue<E> queue;

    public LimitedQueue(int limit){
        this.limit = limit;
        this.queue = new LinkedList<>();
    }

    public List<E> getQueue() {
        List<E> l = new ArrayList<>(this.queue);
        // do I need this reverse?
        //Collections.reverse(l);
        return l;
    }

    public void enq(E o){
        this.queue.add(o);
        while(this.queue.size() > limit) this.queue.remove();
    }

    public E deq(){
        return this.queue.remove();
    }

    public static void main(String[] args){
        LimitedQueue<String> queue = new LimitedQueue<>(3);
        queue.enq("1");
        queue.enq("2");
        queue.enq("3");
        queue.enq("4");
        System.out.println(queue.deq());
        System.out.println(queue.getQueue());
    }
}
