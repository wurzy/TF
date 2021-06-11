import model.Extract;

import java.util.List;
import java.util.concurrent.TimeoutException;

public interface IClient {
    boolean withdraw(int amount, int account, String desc) throws TimeoutException;
    boolean deposit(int amount, int account, String desc) throws TimeoutException;
    boolean transfer(int amount, int src, int dest, String desc) throws TimeoutException;
    List<Extract> extract(int account) throws TimeoutException;
    void fees() throws TimeoutException;
}