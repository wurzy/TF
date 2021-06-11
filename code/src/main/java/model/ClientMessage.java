package model;

import utils.RequestType;

import java.io.Serializable;

public class ClientMessage implements Serializable {
    private RequestType type;
    private int account1;
    private int account2;
    private int amount;
    private String description;

    public ClientMessage(){
        this.type = RequestType.FEES;
    }

    public ClientMessage(int account, int amount, String description) {
        this.type = RequestType.MOVEMENT;
        this.account1 = account;
        this.amount = amount;
        this.description = description;
    }

    public ClientMessage(int src, int dest, int amount, String description) {
        this.type = RequestType.TRANSFER;
        this.account1 = src;
        this.account2 = dest;
        this.amount = amount;
        this.description = description;
    }

    public ClientMessage(int account) {
        this.type = RequestType.EXTRACT;
        this.account1 = account;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public int getAccount1() {
        return account1;
    }

    public void setAccount1(int account1) {
        this.account1 = account1;
    }

    public int getAccount2() {
        return account2;
    }

    public void setAccount2(int account2) {
        this.account2 = account2;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
