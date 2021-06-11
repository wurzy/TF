package controller;

import model.BankAccount;
import org.apache.commons.math3.util.Pair;
import utils.BankLock;
import utils.Constants;
import model.Extract;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;


public class DBController {
    private Connection conn;
    private BankLock lock;
    private ReentrantLock lockReq;


    public DBController(String name){
        try {
            this.conn = DriverManager.getConnection("jdbc:hsqldb:file:db/" + name + "/bank", "SA", "");
            this.lock = new BankLock(Constants.accounts);
            this.lockReq = new ReentrantLock();
            this.createSchema();
            this.createTables();
            this.initDB();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


    private void initDB(){
        for (int i = 0; i < Constants.accounts; i++){
            if (!exists(i)){
                insert(i, new BankAccount());
            }
        }
        initRequest();
    }

    private void createSchema(){
        try {
            Statement stm = this.conn.createStatement();
            stm.execute("SET FILES WRITE DELAY FALSE");
            String sql = "CREATE SCHEMA IF NOT EXISTS bank;";
            stm.executeUpdate(sql);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void createTables(){
        try {
            Statement stm = this.conn.createStatement();
            String str = "CREATE TABLE IF NOT EXISTS bank.Bank (" +
                    "account INT NOT NULL, " +
                    "balance FLOAT NOT NULL, " +
                    "PRIMARY KEY (account));";
            stm.executeUpdate(str);
            str = "CREATE TABLE IF NOT EXISTS bank.History (" +
                    "account INT NOT NULL, " +
                    "description VARCHAR(255) NOT NULL, " +
                    "date DATETIME NOT NULL, " +
                    "value FLOAT NOT NULL, " +
                    "balance FLOAT NOT NULL);";
            stm.executeUpdate(str);
            str = "ALTER TABLE bank.History ADD CONSTRAINT IF NOT EXISTS History_fk0 FOREIGN KEY (account) REFERENCES Bank(account) ON DELETE CASCADE ;";
            stm.executeUpdate(str);
            str = "CREATE TABLE IF NOT EXISTS bank.Log (request INT NOT NULL);";
            stm.executeUpdate(str);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void updateRequest(){
        this.lockReq.lock();
        try{
            Statement stm = this.conn.createStatement();
            String sql = "SELECT * FROM bank.Log";
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()){
                int newr = rs.getInt("request") + 1;
                sql = "UPDATE bank.Log SET request = " + newr;
                stm.executeUpdate(sql);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        finally {
            this.lockReq.unlock();
        }
    }

    private void initRequest(){
        try{
            Statement stm = this.conn.createStatement();
            String sql = "SELECT * FROM bank.Log";
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()) return;
            sql = "INSERT INTO bank.Log VALUES (" + 0 + ");";
            stm.executeUpdate(sql);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public int getRequest(){
        this.lockReq.lock();
        try{
            Statement stm = this.conn.createStatement();
            String sql = "SELECT * FROM bank.Log";
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()){
                this.lockReq.unlock();
                return rs.getInt("request");
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            this.lockReq.unlock();
        }
        this.lockReq.unlock();
        return -1;
    }

    private boolean exists(int id){
        try{
            Statement stm = this.conn.createStatement();
            String qry = "SELECT account, balance FROM bank.Bank WHERE account = " + id + ";";
            ResultSet rs = stm.executeQuery(qry);
            return rs.next();
        }catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    private void insert(int id, BankAccount b){
        try{
            Statement stm = this.conn.createStatement();
            String str = "INSERT INTO bank.Bank VALUES (" + id + ", " + b.getBalance() + ");";
            stm.executeUpdate(str);
            b.getHistory().forEach(e -> {
                try {
                    String sql = "INSERT INTO bank.History VALUES (" +
                            id + ", " +
                            "\'" + e.getDescription() + "\', " +
                            "\'" + e.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) + "\', " +
                            e.getValue() + ", " +
                            e.getBalance() + ");";
                    stm.executeUpdate(sql);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            });
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    private void update(int id, BankAccount b){
        try{
            Statement stm = this.conn.createStatement();
            String sql = "DELETE FROM bank.Bank WHERE account = " + id + ";";
            stm.executeUpdate(sql);
            insert(id, b);
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void replaceAccounts(Map<Integer, BankAccount> m){
        this.lock.writelock(new ArrayList<>(m.keySet()));
        m.forEach((k,v) -> {
            this.put(k,v);
            this.lock.writeunlock(k);
        });
        updateRequest();
    }

    private void put(int id, BankAccount b) {
        if (exists(id)){
            update(id, b);
        }
        else{
            insert(id, b);
        }
    }

    private BankAccount getAccount(int id){
        try{
            Statement stm = this.conn.createStatement();
            String sql = "SELECT balance FROM bank.Bank WHERE account = " + id + ";";
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()){
                float balance = rs.getFloat("balance");
                return new BankAccount(balance, db_getHistory(id));
            }
            return null;
        } catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    public Map<Integer, BankAccount> getBank(){
        this.lock.readlock();
        Map<Integer, BankAccount> ret = new HashMap<>();
        for (int i = 0; i < Constants.accounts; i++){
            ret.put(i, getAccount(i));
            this.lock.readunlock(i);
        }
        return ret;
    }

    public Pair<Boolean, BankAccount> movement(int id, String desc, float value){
        this.lock.writelock(id);
        Pair<Boolean, BankAccount> ret = db_movement(id, desc, value);
        this.lock.writeunlock(id);
        if (ret.getFirst()) updateRequest();
        return ret;
    }

    private Pair<Boolean, BankAccount> db_movement (int id, String desc, float value){
        try{
            Statement stm = this.conn.createStatement();
            String sql = "SELECT balance FROM bank.Bank WHERE account = " + id + ";";
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()){
                float balance = rs.getFloat("balance");
                balance += value;
                if (balance >= 0){
                    sql = "UPDATE bank.Bank SET balance=" + balance + " WHERE account = " + id + ";";
                    stm.executeUpdate(sql);
                    sql = "INSERT INTO bank.History VALUES (" +
                            id + ", " +
                            "\'" + desc + "\', " +
                            "\'" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) + "\', " +
                            value + ", " +
                            balance + ");";
                    stm.executeUpdate(sql);
                    Pair<Boolean, BankAccount> ret = new Pair<>(true, getAccount(id));
                    return ret;
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return new Pair<>(false, null);
    }

    public Pair<Boolean, Pair<BankAccount, BankAccount>> transfer(int src, int dst, String desc, float value){
        this.lock.writelock(src, dst);
        if (value < 0){
            this.lock.writeunlock(src, dst);
            return new Pair<>(false, null);
        }
        String s = "Transfer[" + src + " -> " + dst + "]: " + desc;
        Pair<Boolean, BankAccount> rsrc = this.db_movement(src, s, -value);
        if (rsrc.getFirst()){
            Pair<Boolean, BankAccount> rdst = this.db_movement(dst, s, value);
            Pair<Boolean, Pair<BankAccount, BankAccount>> ret = new Pair<>(rdst.getFirst(), new Pair<>(rsrc.getSecond(), rdst.getSecond()));
            this.lock.writeunlock(src, dst);
            updateRequest();
            return ret;
        }
        this.lock.writeunlock(src, dst);
        return new Pair<>(false, null);
    }

    public Map<Integer, BankAccount> fees(){
        Map<Integer, BankAccount> ret = new HashMap<>();
        this.lock.writelock();
        for (int i = 0; i < Constants.accounts; i++){
            boolean b = fees(i);
            if (b) ret.put(i, getAccount(i));
            this.lock.writeunlock(i);
        }
        updateRequest();
        return ret;
    }

    private boolean fees(int id){
        try{
            Statement stm = this.conn.createStatement();
            String sql = "SELECT balance FROM bank.Bank WHERE account = " + id + ";";
            ResultSet rs = stm.executeQuery(sql);
            if (rs.next()){
                float balance = rs.getFloat("balance");
                if (balance == 0) return false;
                balance += balance*(Constants.fees/100);
                sql = "UPDATE bank.Bank SET balance=" + balance + " WHERE account = " + id + ";";
                stm.executeUpdate(sql);
                sql = "INSERT INTO bank.History VALUES (" +
                        id + ", " +
                        "\'" + Constants.fees + "% fees" + "\', " +
                        "\'" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) + "\', " +
                        balance*(Constants.fees/100) + ", " +
                        balance + ");";
                stm.executeUpdate(sql);
                return true;
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public List<Extract> getHistory(int id){
        this.lock.readlock(id);
        List<Extract> ret = db_getHistory(id);
        this.lock.readunlock(id);
        return ret;
    }

    private List<Extract> db_getHistory(int id){
        try{
            List<Extract> ret = new ArrayList<>();
            Statement stm = this.conn.createStatement();
            String sql = "SELECT description, date, value, balance FROM bank.History WHERE account = " + id + " ORDER BY date DESC LIMIT " + Constants.extractLimit + ";";
            ResultSet rs = stm.executeQuery(sql);
            while(rs.next()){
                Extract e = new Extract(rs.getString("description"), rs.getTimestamp("date").toLocalDateTime(), rs.getFloat("value"), rs.getFloat("balance"));
                ret.add(e);
            }
            return ret;
        }catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    public void printTables(){
        try {
            Statement stm = this.conn.createStatement();
            String sql = "SELECT * FROM bank.Bank";
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next()){
                System.out.println("[" + rs.getInt("account") + "] " + rs.getInt("balance"));
            }
            sql = "SELECT * FROM bank.History";
            rs = stm.executeQuery(sql);
            while (rs.next()){
                System.out.println("[" + rs.getInt("account") + "] " + rs.getString("description") + " " + rs.getString("date") + " " + rs.getFloat("value") + " " + rs.getFloat("balance"));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void main(String[] args) {
        DBController db = new DBController("server1");

        System.out.println(db.movement(0, "deposito 1", 10));
        db.fees();
        System.out.println(db.movement(0, "bad", -100000));
        System.out.println(db.transfer(0,1, "pagamento", 1));

        for (Extract e : db.getHistory(0)){
            System.out.println(e.toString());
        }
        for (Extract e : db.getHistory(1)){
            System.out.println(e.toString());
        }
    }
}
