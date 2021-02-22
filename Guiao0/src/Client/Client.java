package Client;

import Message.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client {
    public static void main(String... args) throws Exception{
        Socket s = new Socket("localhost",12345);
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(s.getInputStream());
        while(true) {
            Message<Integer> req = new Message<>(0,10);
            out.writeObject(req);
            Message<Boolean> m = (Message<Boolean>) in.readObject();
            System.out.println(m.info);
            Thread.sleep(1000);
        }
    }
}
