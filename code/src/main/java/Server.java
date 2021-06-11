import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import controller.DBController;
import model.*;
import org.apache.commons.math3.util.Pair;
import utils.Constants;
import utils.LimitedQueue;
import utils.SerializeUtils;
import spread.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class Server {

    private Map<String, PrimaryTable> ptable;

    private DBController bank;

    private SpreadConnection conn;
    private List<SpreadGroup> view;

    private boolean isLeader;
    private List<SpreadGroup> leaderAfter;

    private NettyMessagingService ms;
    private ScheduledExecutorService es;
    private int primaryPort;

    private int stateTransferPhase;
    private LimitedQueue<Updates> requests;

    private ReentrantLock lockOnHoldQueue;
    private Queue<Updates> updatesOnHold;

    private String name;


    public Server(String name, int pport){
        this.name=name;
        this.bank = new DBController(name);
        this.isLeader = false;
        this.leaderAfter = new ArrayList<>();
        this.ptable = new HashMap<>();
        this.view = new ArrayList<>();
        this.primaryPort = pport;
        this.stateTransferPhase = 0;
        this.requests = new LimitedQueue<>(Constants.storedUpdates);
        this.lockOnHoldQueue = new ReentrantLock();
        this.updatesOnHold = new LinkedList<>();
    }

    public void start(){
        this.connectBackups(4803, this.name);
        this.messageProtocol();
    }

    public void connectBackups(int port, String name){
        try {
            this.conn = new SpreadConnection();
            this.conn.connect(InetAddress.getByName("localhost"), port, name, false, true);
            SpreadGroup group = new SpreadGroup();
            group.join(this.conn, "backups");
            System.out.println("JOINED [backups] group");
        } catch (SpreadException | UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void addAtomixHandler(){
        this.ms.registerHandler("request", (a,m) -> {
            ClientMessage msg = (ClientMessage) SerializeUtils.deserialize(m);
            if (msg==null) return;
            System.out.println("[REQUEST: " + a.toString() + "] " + msg.getType());
            computeRequest(msg, a);
        }, es);
    }

    public void send(byte[] content, String dest){
        try {
            SpreadMessage m = new SpreadMessage();
            m.setData(content);
            m.setSafe();
            m.addGroup(dest);
            this.conn.multicast(m);
        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }

    public void causedByJoin(MembershipInfo info){
        if (info.getJoined().equals(conn.getPrivateGroup())){
            if (info.getMembers().length==1){
                isLeader = true;
                stateTransferPhase = 2;
                System.out.println("[LEADER] Conecting to atomix...");
                this.es = Executors.newScheduledThreadPool(1);
                this.ms = new NettyMessagingService("bank", Address.from(this.primaryPort), new MessagingConfig());
                this.ms.start();
                addAtomixHandler();
            }
            else {
                int req = this.bank.getRequest();
                send(SerializeUtils.serialize(req), "backups");
                for (SpreadGroup m : info.getMembers()){
                    if (!m.equals(conn.getPrivateGroup())){
                        leaderAfter.add(m);
                    }
                }
            }
        }
    }

    public void sendUpdates(Updates up){
        byte[] bytes = SerializeUtils.serialize(up);
        send(bytes, "backups");
    }

    public void causedByCrash(MembershipInfo info){
        if (info.isCausedByLeave()){
            leaderAfter.remove(info.getLeft());
            this.ptable.forEach((k,v) -> {
                if (v.receivedAck(info.getLeft())){
                    System.out.println("[SERVER LEFT - ALL ACKS RECEIVED]" + k);
                    sendToClient(Address.from(k), v.getResponse());
                    this.ptable.remove(k);
                }
            });
        }
        if (info.isCausedByDisconnect()){
            leaderAfter.remove(info.getDisconnected());
            this.ptable.forEach((k,v) -> {
                if (v.receivedAck(info.getDisconnected())){
                    System.out.println("[SERVER LEFT - ALL ACKS RECEIVED]" + k);
                    sendToClient(Address.from(k), v.getResponse());
                    this.ptable.remove(k);
                }
            });
        }
        if (!isLeader){
            isLeader = leaderAfter.isEmpty();
            if (isLeader){
                System.out.println("[LEADER] Conecting to atomix...");
                this.es = Executors.newScheduledThreadPool(1);
                this.ms = new NettyMessagingService("bank", Address.from(this.primaryPort), new MessagingConfig());
                this.ms.start();
                addAtomixHandler();
            }
        }
    }

    public void computeBackups(Updates up, Address cli, Object r){
        List<SpreadGroup> correctReplicas = view.stream()
                .filter(p -> !p.equals(conn.getPrivateGroup()))
                .collect(Collectors.toList());
        if (correctReplicas.isEmpty()){
            sendToClient(cli, r);
            return;
        }
        this.ptable.put(cli.toString(), new PrimaryTable(correctReplicas, r));
        sendUpdates(up);
    }

    public void computeRequest(ClientMessage m, Address cli){
        Map<Integer,BankAccount> accountsUpdated = new HashMap<>();
        Updates up;
        switch (m.getType()){
            case MOVEMENT:
                Pair<Boolean, BankAccount> rm = this.bank.movement(m.getAccount1(), m.getDescription(), m.getAmount());
                if (!rm.getFirst()){
                    sendToClient(cli, rm.getFirst());
                    return;
                }
                accountsUpdated.put(m.getAccount1(),rm.getSecond());
                up = new Updates(cli.toString(), accountsUpdated);
                this.requests.enq(up);
                computeBackups(up, cli, rm.getFirst());
                break;
            case TRANSFER:
                Pair<Boolean, Pair<BankAccount, BankAccount>> rt = this.bank.transfer(m.getAccount1(), m.getAccount2(), m.getDescription(), m.getAmount());
                if (!rt.getFirst()){
                    sendToClient(cli, rt.getFirst());
                    return;
                }
                accountsUpdated.put(m.getAccount1(),rt.getSecond().getFirst());
                accountsUpdated.put(m.getAccount2(),rt.getSecond().getSecond());
                up = new Updates(cli.toString(), accountsUpdated);
                this.requests.enq(up);
                computeBackups(up, cli, rt.getFirst());
                break;
            case EXTRACT:
                List<Extract> r = this.bank.getHistory(m.getAccount1());
                sendToClient(cli, r);
                return;
            case FEES:
                accountsUpdated = this.bank.fees();
                up = new Updates(cli.toString(), accountsUpdated);
                this.requests.enq(up);
                computeBackups(up,cli, "Confirmed");
                return;
            default:
                sendToClient(cli, "Unkown operation");
        }
    }

    public void sendAck(String cli, SpreadGroup leader){
        byte[] bytes = SerializeUtils.serialize(cli);
        send(bytes, leader.toString());
    }

    public void updateReceived(SpreadMessage msg){
        Updates u = (Updates) SerializeUtils.deserialize(msg.getData());
        if (u==null) return;
        System.out.println("[UPDATE " + u.getClient() + "]");
        this.bank.replaceAccounts(u.getUpdates());
        sendAck(u.getClient(), msg.getSender());
    }

    public void sendToClient(Address cli, Object response){
        byte[] bytes = SerializeUtils.serialize(response);
        if (bytes==null) return;
        this.ms.sendAsync(cli, "response", bytes).thenRun(() -> System.out.println("[RESPONSE] " + cli.toString()));
    }

    public void ackReceived (SpreadMessage msg){
        String cli = (String) SerializeUtils.deserialize(msg.getData());
        if (cli==null) return;
        System.out.println("[ACK " + msg.getSender() + " " + cli + "] ");
        if (this.ptable.get(cli).receivedAck(msg.getSender())){
            System.out.println("[ALL ACKS RECEIVED]");
            sendToClient(Address.from(cli), this.ptable.get(cli).getResponse());
            this.ptable.remove(cli);
        }
    }

    public void sendAllState(SpreadGroup dest){
        byte[] bytes = SerializeUtils.serialize(this.bank.getBank());
        send(bytes, dest.toString());
    }

    public void sendIncrementalState(int diff, List<Updates> store, SpreadGroup dest){
        List<Updates> send = new ArrayList<>(store.subList(store.size()-diff,store.size()));
        send(SerializeUtils.serialize(send), dest.toString());
    }

    public void sendState(int nreq, SpreadGroup dest){
        int mreq = this.bank.getRequest();
        List<Updates> l = this.requests.getQueue();
        if (nreq > mreq){
            sendAllState(dest);
        }
        else{
            int diff = mreq - nreq;
            if (diff <= l.size()){
                sendIncrementalState(diff, l, dest);
            }
            else{
                sendAllState(dest);
            }
        }
    }

    public void computeOnHoldRequest(){
        this.lockOnHoldQueue.lock();
        while(!this.updatesOnHold.isEmpty()){
            this.lockOnHoldQueue.unlock();
            Updates up = this.updatesOnHold.remove();
            this.bank.replaceAccounts(up.getUpdates());
            this.lockOnHoldQueue.lock();
        }
        this.stateTransferPhase = 2;
        this.lockOnHoldQueue.unlock();
    }

    public void messageProtocol(){
        this.conn.add(new AdvancedMessageListener() {
            @Override
            public void regularMessageReceived(SpreadMessage spreadMessage) {
                Object obj = SerializeUtils.deserialize(spreadMessage.getData());
                if (spreadMessage.getSender().equals(conn.getPrivateGroup())){
                    if (obj instanceof Integer && !isLeader){
                        stateTransferPhase = 1;
                    }
                    return;
                };
                if (isLeader){
                    if (obj instanceof Integer){
                        System.out.println("[ASKED FOR STATE] " + obj);
                        sendState((Integer) obj, spreadMessage.getSender());
                    }
                    else{
                        ackReceived(spreadMessage);
                    }
                }
                else{
                    if (obj instanceof Integer) return;
                    if (obj instanceof Map){
                        if (stateTransferPhase == 1){
                            System.out.println("[STATE RECEIVED]");
                            @SuppressWarnings("unchecked")
                            Map<Integer, BankAccount> state = (Map<Integer, BankAccount>) obj;
                            bank.replaceAccounts(state);
                            computeOnHoldRequest();
                            return;
                        }
                    }
                    if (obj instanceof List){
                        if (stateTransferPhase == 1){
                            @SuppressWarnings("unchecked")
                            List<Updates> up = (List<Updates>) obj;
                            System.out.println("[INCREMENTAL STATE RECEIVED] " + up.size());
                            up.forEach(u -> bank.replaceAccounts(u.getUpdates()));
                            computeOnHoldRequest();
                        }
                    }
                    else{
                        if (stateTransferPhase == 0){
                            Updates up = (Updates) obj;
                            if (up == null) return;
                            sendAck(up.getClient(), spreadMessage.getSender());
                        }
                        lockOnHoldQueue.lock();
                        if (stateTransferPhase == 1){
                            Updates up = (Updates) obj;
                            if (up == null) return;
                            updatesOnHold.add(up);
                            lockOnHoldQueue.unlock();
                            sendAck(up.getClient(), spreadMessage.getSender());
                        }
                        lockOnHoldQueue.unlock();
                        if (stateTransferPhase == 2) {
                            updateReceived(spreadMessage);
                        }
                    }
                }
            }

            @Override
            public void membershipMessageReceived(SpreadMessage spreadMessage) {
                MembershipInfo info = spreadMessage.getMembershipInfo();
                if (info.isRegularMembership()){
                    if (info.isCausedByJoin()){
                        causedByJoin(info);
                        view = Arrays.asList(info.getMembers());
                    }
                    if (info.isCausedByDisconnect() || info.isCausedByLeave()){
                        causedByCrash(info);
                        view = Arrays.asList(info.getMembers());
                    }
                }
            }
        });
    }


    // input: [nome servidor] [porta atomix do primario]
    public static void main(String[] args){
        Server srv = new Server(args[0],Integer.parseInt(args[1]));
        srv.start();

        try {
            TimeUnit.DAYS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
