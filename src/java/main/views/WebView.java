package main.views;

import javafx.concurrent.Worker;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.stage.Screen;
import javafx.stage.Stage;
import main.Main;
import main.controllers.Controller;
import main.io.AuthFile;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class WebView implements View{
    public javafx.scene.web.WebView webViewId;
    public AnchorPane anchorId;
    private String apiKey;
    private Controller controller;
    private Scene ivleViewScene;

    public void initialize() {
        controller = Controller.getInstance();
        apiKey = controller.getApiKey();
        WebEngine engine = webViewId.getEngine();
        engine.load("https://ivle.nus.edu.sg/api/login/?apikey=" + apiKey);
        engine.getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) ->
                getAuthToken(engine, newValue));
    }

    private void getAuthToken(WebEngine engine, Worker.State newValue) {
        if (newValue != Worker.State.SUCCEEDED) return;
        String url = engine.getLocation();
        System.out.println(url);
        if (!url.contains("/api/login/login_result.ashx")) return;
        if (!url.contains("&r=0")) return;
        Document document = engine.getDocument();
        Node body = document.getElementsByTagName("body").item(0);
        String authToken = body.getTextContent();
        controller.setAuthToken(authToken);
        AuthFile.refreshScanner();
        AuthFile.nextLine();
        AuthFile.nextLine();
        String syncRootPath = AuthFile.nextLine();
        AuthFile.updateFile(apiKey, authToken, syncRootPath);
        Main.setIvleView(this);
        Stage stage = (Stage) anchorId.getScene().getWindow();
        stage.setScene(ivleViewScene);
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((primScreenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((primScreenBounds.getHeight() - stage.getHeight()) / 2);
    }

    @Override
    public void setWebView(Scene webViewScene) {}

    @Override
    public void setIvleView(Scene ivleViewScene) {
        this.ivleViewScene = ivleViewScene;
    }
}
