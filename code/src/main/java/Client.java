import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import model.ClientMessage;
import model.Extract;
import utils.SerializeUtils;
import view.Menus;

import java.util.List;
import java.util.concurrent.*;


public class Client implements IClient{
    private NettyMessagingService ms;
    private ScheduledExecutorService es;
    private CompletableFuture<Object> requestcf;
    private int primaryPort;

    public Client(int port, int pport) {
        this.es = Executors.newScheduledThreadPool(1);
        this.ms = new NettyMessagingService("bank", Address.from(port), new MessagingConfig());
        this.ms.start();
        this.primaryPort = pport;
        this.messageProtocol();
    }


    private Object makeRequest(ClientMessage msg) throws TimeoutException{
        try {
            this.requestcf = new CompletableFuture<>();
            byte[] bytes = SerializeUtils.serialize(msg);
            if (bytes==null) return null;
            this.ms.sendAsync(Address.from(this.primaryPort), "request", bytes);
            return this.requestcf.get(20, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }


    private void messageProtocol(){
        this.ms.registerHandler("response", (a,m) -> {
            Object obj = SerializeUtils.deserialize(m);
            if (obj==null) return;
            this.requestcf.complete(obj);
        }, es);
    }


    public boolean withdraw(int amount, int account, String desc) throws TimeoutException {
        ClientMessage cm = new ClientMessage(account, -amount, desc);
        Object r;
        r = makeRequest(cm);
        return (Boolean) r;
    }

    public boolean deposit(int amount, int account, String desc) throws TimeoutException {
        ClientMessage cm = new ClientMessage(account, amount, desc);
        Object r;
        r = makeRequest(cm);
        return (Boolean) r;
    }

    public boolean transfer(int amount, int src, int dest, String desc) throws TimeoutException {
        ClientMessage cm = new ClientMessage(src, dest, amount, desc);
        Object r;
        r = makeRequest(cm);
        return (Boolean) r;
    }

    public List<Extract> extract(int account) throws TimeoutException {
        ClientMessage cm = new ClientMessage(account);
        Object r;
        r = makeRequest(cm);
        @SuppressWarnings("unchecked")
        List<Extract> ret = (List<Extract>) r;
        return ret;
    }


    public void fees() throws TimeoutException {
        ClientMessage cm = new ClientMessage();
        makeRequest(cm);
    }

    private void movementMenu(){
        int c = Menus.movementMenu();
        String[] args;
        boolean b = false;
        switch (c){
            case 0:
                return;
            case 1:
                args = Menus.movementArgs();
                try {
                    b = deposit(Integer.parseInt(args[1]), Integer.parseInt(args[0]), args[2]);
                }catch (TimeoutException e){
                    System.out.println("[TimeOut] Dont' know that happened");
                }
                if (b) System.out.println("Deposit confirmed");
                break;
            case 2:
                args = Menus.movementArgs();
                try {
                    b = withdraw(Integer.parseInt(args[1]), Integer.parseInt(args[0]), args[2]);
                } catch (TimeoutException e) {
                    System.out.println("[TimeOut] Dont' know that happened");
                }
                if (b) System.out.println("Withdraw confirmed");
                break;
            default:
                System.out.println("Bad request");
                movementMenu();
        }
    }

    private void transferMenu(){
        String[] args = Menus.transferArgs();
        boolean b = false;
        try {
            b = transfer(Integer.parseInt(args[2]), Integer.parseInt(args[0]), Integer.parseInt(args[1]), args[3]);
        } catch (TimeoutException e) {
            System.out.println("[TimeOut] Dont' know that happened");
        }
        if (b) System.out.println("Transfer confirmed");
    }

    private void extractMenu(){
        int account = Menus.extractArgs();
        List<Extract> l = null;
        try {
            l = extract(account);
        } catch (TimeoutException e) {
            System.out.println("[TimeOut] Dont' know that happened");
        }
        if (l != null) Menus.showExtract(l, account);
    }

    private void feesMenu(){
        try {
            fees();
            System.out.println("Fees confirmed");
        } catch (TimeoutException e) {
            System.out.println("[TimeOut] Dont' know that happened");
        }
    }

    private void mainMenu(){
        int h = Menus.home();
        switch (h){
            case 0:
                return;
            case 1:
                movementMenu();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                transferMenu();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case 3:
                extractMenu();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case 4:
                feesMenu();
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("Bad request");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
        mainMenu();
    }

    private void menu(){
        Menus.startupMessage();
        mainMenu();
    }

    // input: [porta do cliente] [porta atomix do primario]
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        int pport = Integer.parseInt(args[1]);

        Client cli = new Client(port, pport);

        cli.menu();

        System.exit(0);
    }
}