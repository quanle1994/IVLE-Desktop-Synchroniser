package main.utils;

public class Constants {
    public static final String BASE_URL = "https://ivle.nus.edu.sg/api/Lapi.svc/";
    public static final String DOWNLOAD_URL = "https://ivle.nus.edu.sg/api/downloadfile.ashx";
    public static String CONFIG_PATH = "";
    public static String CONFIG_FILE = "";
    public static String FAV_FILE = "";

    public static void main(String[] args) {
        System.out.println(System.getProperty("user.home") + "/IVLEDesktopSync/IVLeSync");
    }

    public static void init() {
        String macAppData = System.getProperty("user.home") + "/Library/Application Support";
        String windowsAppData = System.getenv("APPDATA");
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            CONFIG_PATH = macAppData + "/IVLeSync";
            CONFIG_FILE = macAppData + "/IVLeSync/config.txt";
            FAV_FILE = macAppData + "/IVLeSync/favourite.txt";
            return;
        }
        CONFIG_PATH = windowsAppData + "\\IVLeSync";
        CONFIG_FILE = windowsAppData + "\\IVLeSync\\config.txt";
        FAV_FILE = windowsAppData + "\\IVLeSync\\favourite.txt";
    }
}
