package main.utils;

public class Constants {
    public static final String BASE_URL = "https://ivle.nus.edu.sg/api/Lapi.svc/";
    public static final String DOWNLOAD_URL = "https://ivle.nus.edu.sg/api/downloadfile.ashx";
    public static final String CONFIG_PATH = System.getenv("APPDATA") + "\\IVLeSync";
    public static final String CONFIG_FILE = System.getenv("APPDATA") + "\\IVLeSync\\config.txt";
    public static final String FAV_FILE = System.getenv("APPDATA") + "\\IVLeSync\\favourite.txt";

    public static void main(String[] args) {
        System.out.println(System.getenv("APPDATA") + "/IVLEDesktopSync/IVLeSync");
    }
}
