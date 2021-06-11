package utils;

import java.io.*;

public class SerializeUtils {

    public static byte[] serialize(Object object) {
        try{
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(object);
            return bos.toByteArray();
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    public static Object deserialize(byte[] bytes){
        try{
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInputStream in = new ObjectInputStream(bis);
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
