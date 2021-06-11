package model;

import java.io.Serializable;
import java.util.Map;

public class Updates implements Serializable {
    private String client;
    private Map<Integer, BankAccount> updates;

    public Updates(String client, Map<Integer, BankAccount> updates) {
        this.client = client;
        this.updates = updates;
    }

    public String getClient() {
        return client;
    }

    public Map<Integer, BankAccount> getUpdates() {
        return updates;
    }

}
