package model;

import spread.SpreadGroup;

import java.util.List;

public class PrimaryTable {
    private List<SpreadGroup> acksReceived;
    private Object response;

    public PrimaryTable(List<SpreadGroup> ar, Object resp) {
        this.acksReceived = ar;
        this.response = resp;
    }

    public boolean receivedAck(SpreadGroup sg){
        this.acksReceived.remove(sg);
        return this.acksReceived.isEmpty();
    }

    public List<SpreadGroup> getAcksReceived() {
        return acksReceived;
    }

    public Object getResponse() {
        return response;
    }
}
