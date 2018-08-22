package main.io;

import main.utils.Constants;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

public class FavFile {
    private static File favFile = new File(Constants.FAV_FILE);
    private static Scanner sc = null;

    public static boolean createFile(){
        try {
            return !favFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public static void updateFile(List<String> notificationItemIds){
        try {
            PrintWriter pw = new PrintWriter(Constants.FAV_FILE);
            for (String notificationItemId : notificationItemIds) {
                pw.append(notificationItemId).append("\r\n");
            }
            pw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String nextLine() {
        try {
            if (sc == null) refreshScanner();
            return sc.nextLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void refreshScanner() {
        try {
            sc = new Scanner(favFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
