package utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BankLock {
    private Map<Integer, AccountLock> accounts;

    public BankLock(int n){
        this.accounts = new HashMap<>();
        for(int i = 0; i < n; i++){
            this.accounts.put(i, new AccountLock());
        }
    }

    public void readlock(int i){
        this.accounts.get(i).readlock();
    }

    public void readunlock(int i){
        this.accounts.get(i).readunlock();
    }

    public void writelock(int i){
        this.accounts.get(i).writelock();
    }

    public void writeunlock(int i){
        this.accounts.get(i).writeunlock();
    }

    public void readlock(int i, int j){
        if (j > i) {
            this.accounts.get(i).readlock();
            this.accounts.get(j).readlock();
        }
        else{
            this.accounts.get(j).readlock();
            this.accounts.get(i).readlock();
        }
    }

    public void readunlock(int i, int j){
        this.accounts.get(i).readunlock();
        this.accounts.get(j).readunlock();
    }

    public void writelock(int i, int j){
        if (j > i) {
            this.accounts.get(i).writelock();
            this.accounts.get(j).writelock();
        }
        else{
            this.accounts.get(j).writelock();
            this.accounts.get(i).writelock();
        }
    }

    public void writeunlock(int i, int j){
        this.accounts.get(i).writeunlock();
        this.accounts.get(j).writeunlock();
    }

    public void readlock(){
        this.accounts.forEach((k,v) -> v.readlock());
    }

    public void readunlock(){
        this.accounts.forEach((k,v) -> v.readunlock());
    }

    public void writelock(){
        this.accounts.forEach((k,v) -> v.writelock());
    }

    public void writeunlock(){
        this.accounts.forEach((k,v) -> v.writeunlock());
    }

    public void readlock(List<Integer> l){
        Collections.sort(l);
        l.forEach(this::readlock);
    }

    public void readunlock(List<Integer> l){
        l.forEach(this::readunlock);
    }

    public void writelock(List<Integer> l){
        Collections.sort(l);
        l.forEach(this::writelock);
    }

    public void writeunlock(List<Integer> l){
        l.forEach(this::writeunlock);
    }
}
