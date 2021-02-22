package Message;

import java.io.Serializable;

public class Message<T> implements Serializable {
    public int type; // 0 = deposit ; 1 = balance
    public T info;

    public Message(int type,T object){
        this.type = type;
        this.info = object;
    }
}
