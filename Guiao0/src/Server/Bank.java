package Server;

public class Bank {
    private int balance;

    public Bank(){
        balance = 0;
    }

    public synchronized boolean movement(int x){
        if (x < 0 && balance + x < 0) {
            return false;
        }
        int prev = balance;
        balance += x;
        System.out.println("Movement: " + x +". Now " + balance + " (previous: " + prev + ")");
        return true;
    }

    public synchronized int balance(){
        System.out.println("Balance: " + balance);
        return balance;
    }
}
