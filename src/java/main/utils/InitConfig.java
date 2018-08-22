package main.utils;

import main.io.AuthFile;
import main.io.FavFile;

public class InitConfig {
    private String apiKey = "GdHCDEBG1u5Y0MNwxpedW";
    private String authToken = "";
    private boolean exists;
    public InitConfig() {
        exists = AuthFile.createDirectory();
        exists = exists && AuthFile.createFile();
        FavFile.createFile();
        if (exists) return;
        AuthFile.updateFile(apiKey, authToken, "");
    }

    public boolean exists() {
        return exists;
    }
}
