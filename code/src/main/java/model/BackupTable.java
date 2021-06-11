package model;

public class BackupTable {
    private int requestId;
    private Object response;

    public BackupTable(int rid, Object resp) {
        this.requestId = rid;
        this.response = resp;
    }

    public int getRequestId() {
        return requestId;
    }

    public Object getResponse() {
        return response;
    }
}