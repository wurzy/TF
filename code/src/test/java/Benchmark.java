import com.google.common.base.Stopwatch;

import java.sql.Time;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class Benchmark {

    // input: [nr de clientes] [nr de ops]
    public static void main(String[] args) {
        int ncli = Integer.parseInt(args[0]);
        int nseq = Integer.parseInt(args[1]);

        float expected = 5*nseq*ncli;
        System.out.println("Accounts balance should be consistent and equal to " + expected + " €");
        int j=0;
        for(int i = 0; i < ncli; i++){
            new Thread(new ClientRunnable(new Client(8081+j++, 8080), 0, 1, nseq)).start();
            new Thread(new ClientRunnable(new Client(8081+j++, 8080), 1, 0, nseq)).start();
        }
    }
}

class ClientRunnable implements Runnable {
    private int account1;
    private int account2;
    private int nseq;
    private IClient client;

    public ClientRunnable(IClient cli, int a1, int a2, int n) {
        this.account1 = a1;
        this.account2 = a2;
        this.nseq = n;
        this.client = cli;
    }

    public void run() {
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i = 0; i < this.nseq; i++) {
            try {
                this.client.deposit(10, account1, "deposit" + i);
                this.client.withdraw(5, account1, "deposit" + i);
                this.client.transfer(5, account1, account2, "transfer" + i);
            }catch (TimeoutException e){
                e.printStackTrace();
            }
        }
        stopwatch.stop();

        long et = stopwatch.elapsed(TimeUnit.SECONDS);
        System.out.println("Time elapsed: "+ et + " seconds");
        System.out.println("Mean time per request: " + ((float)et)/(nseq* 3L) + " seconds");

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            System.out.println("[" + this.account1 + "]: " + this.client.extract(this.account1).get(0).getBalance() + " €");
            System.out.println("[" + this.account2 + "]: " + this.client.extract(this.account2).get(0).getBalance() + " €");
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }
}



