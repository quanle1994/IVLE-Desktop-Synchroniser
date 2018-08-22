package main.io;

import main.utils.Constants;

import java.io.File;
import java.io.PrintWriter;
import java.util.Scanner;

public class AuthFile {
    private static File authFile = new File(Constants.CONFIG_FILE);
    private static File authPath = new File(Constants.CONFIG_PATH);
    private static Scanner sc = null;

    public static boolean createDirectory(){
        boolean result = authPath.exists();
        if (!result) authPath.mkdirs();
        return result;
    }
    public static boolean createFile(){
        try {
            return !authFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public static void updateFile(String apiKey, String authToken, String rootSyncPath){
        try {
            PrintWriter pw = new PrintWriter(Constants.CONFIG_FILE);
            pw.append(apiKey).append("\r\n");
            pw.append(authToken).append("\r\n");
            pw.append(rootSyncPath).append("\r\n");
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
            sc = new Scanner(authFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
