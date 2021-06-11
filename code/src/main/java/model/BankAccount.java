package model;

import utils.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BankAccount implements Serializable {
    private float balance;
    private List<Extract> history;

    public BankAccount (){
        this.balance=0;
        this.history = new ArrayList<>(Constants.extractLimit);
    }

    public BankAccount (float balance, List<Extract> history){
        this.balance = balance;
        this.history = history;
    }

    public float getBalance() {
        return this.balance;
    }

    public List<Extract> getHistory() {
        return this.history;
    }
}
