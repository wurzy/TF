package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Extract implements Serializable {
    private String description;
    private LocalDateTime date;
    private float value;
    private float balance;

    public Extract(String desc, LocalDateTime d, float v, float b){
        this.description = desc;
        this.date = d;
        this.value = v;
        this.balance = b;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public float getValue() {
        return value;
    }

    public float getBalance() {
        return balance;
    }

    public String toString() {
        return date + "     " + description + "     " + value + " €" + "     " + balance + " €";
    }
}
