package utils;

import org.apache.commons.math3.util.Pair;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AccountLock {
    private ReentrantLock lock;
    private int readers;
    private int writers;
    private Queue<Pair<String, Condition>> waiters;

    public AccountLock (){
        this.lock = new ReentrantLock();
        this.readers = 0;
        this.writers = 0;
        this.waiters = new LinkedList<>();
    }

    public void readlock(){
        this.lock.lock();
        if (this.writers > 0 || !this.waiters.isEmpty()){
            try {
                Condition wait = this.lock.newCondition();
                waiters.add(new Pair<>("read", wait));
                wait.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.readers++;
        this.lock.unlock();
    }

    public void writelock(){
        this.lock.lock();
        if (this.writers > 0 || this.readers > 0 || !this.waiters.isEmpty()){
            try {
                Condition wait = this.lock.newCondition();
                waiters.add(new Pair<>("write", wait));
                wait.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.writers++;
        this.lock.unlock();
    }

    public void readunlock(){
        this.lock.lock();
        this.readers--;
        if (this.readers == 0) this.awake();
        this.lock.unlock();
    }

    public void writeunlock(){
        this.lock.lock();
        this.writers--;
        this.awake();
        this.lock.unlock();
    }

    private void awake(){
        Pair<String, Condition> p = this.waiters.peek();
        if (p==null) return;
        int count = 0;
        while(p.getFirst().equals("read")){
            p = this.waiters.remove();
            p.getSecond().signal();
            p = this.waiters.peek();
            if (p==null) return;
            count++;
        }
        if (count == 0){
            p = this.waiters.remove();
            p.getSecond().signal();
        }
    }
}
