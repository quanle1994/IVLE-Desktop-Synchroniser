package main.controllers;

public class Controller {
    private static final Controller CONTROLLER = new Controller();
    private String apiKey;
    private String authToken;
    private String syncRootDirectory;

    public static Controller getInstance() {
        return CONTROLLER;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getSyncRootDirectory() {
        return syncRootDirectory;
    }

    public void setSyncRootDirectory(String syncRootDirectory) {
        this.syncRootDirectory = syncRootDirectory;
    }
}
