package view;

import model.Extract;
import org.apache.commons.math3.util.Pair;

import java.util.List;
import java.util.Scanner;

public class Menus {

    public static void startupMessage(){
        System.out.println("===============================================================");
        System.out.println("==================      Welcome to BANK      ==================");
        System.out.println("===============================================================");
    }

    public static int home(){
        System.out.println();
        System.out.println();
        System.out.println("Possible operations:");
        System.out.println();
        System.out.println("[1] Movement");
        System.out.println("[2] Transfer");
        System.out.println("[3] Extract");
        System.out.println("[4] Fees");
        System.out.println();
        System.out.println("[0] Leave");

        Scanner scanner = new Scanner(System.in);
        return scanner.nextInt();
    }

    public static int movementMenu(){
        System.out.println();
        System.out.println();
        System.out.println("===== Movement ======");
        System.out.println();
        System.out.println("[1] Deposit");
        System.out.println("[2] Withdraw");
        System.out.println();
        System.out.println("[0] Back");

        Scanner scanner = new Scanner(System.in);
        return scanner.nextInt();
    }

    public static String[] movementArgs(){
        String[] args = new String[3];
        System.out.println();
        System.out.println();
        System.out.println("Account:");
        Scanner scanner = new Scanner(System.in);
        args[0] = scanner.nextLine();
        System.out.println("Value:");
        args[1] = scanner.nextLine();
        System.out.println("Description:");
        args[2] = scanner.nextLine();

        return args;
    }

    public static String[] transferArgs(){
        String[] args = new String[4];
        System.out.println();
        System.out.println();
        System.out.println("Source:");
        Scanner scanner = new Scanner(System.in);
        args[0] = scanner.nextLine();
        System.out.println("Destiny:");
        args[1] = scanner.nextLine();
        System.out.println("Value:");
        args[2] = scanner.nextLine();
        System.out.println("Description:");
        args[3] = scanner.nextLine();

        return args;
    }

    public static int extractArgs(){
        System.out.println();
        System.out.println();
        System.out.println("Account:");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextInt();
    }

    public static void showExtract(List<Extract> l, int account){
        System.out.println();
        System.out.println();

        System.out.println("Account: " + account);
        System.out.println();
        l.forEach(e -> System.out.println(e.toString()));
    }
}
