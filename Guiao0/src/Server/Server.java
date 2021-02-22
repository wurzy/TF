package Server;


import Message.Message;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String... args) throws Exception{
        Bank b = new Bank();
        ServerSocket ss = new ServerSocket(12345);
        Socket s = ss.accept();
        ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(s.getInputStream());
        while(true){
            Message m = (Message) in.readObject();
            Message<Integer> reply;
            switch(m.type){
                case 0:
                    boolean result = b.movement((Integer) m.info);
                    Message<Boolean> mov = new Message<>(2, result);
                    out.writeObject(mov);
                    break;
                case 1:
                    int x = b.balance();
                    Message<Integer> bal = new Message<>(2, x);
                    out.writeObject(bal);
                    break;
                default:
                    break;
            }
        }
    }
}
