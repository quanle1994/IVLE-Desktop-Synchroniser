package main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import main.utils.Constants;
import main.views.View;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Constants.init();
        FXMLLoader configLoader = new FXMLLoader(getClass().getResource("views/ConfigView.fxml"));
        Parent configView = configLoader.load();
        primaryStage.setTitle("IVLe Sync");
        primaryStage.setScene(new Scene(configView, 450, 275));
        primaryStage.show();
    }

    public static void setWebView(View view) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("views/WebView.fxml"));
            Parent webView = loader.load();
            Scene webViewScene = new Scene(webView, 600, 400);
            webViewScene.setUserData(loader);
            view.setWebView(webViewScene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setIvleView(View view) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("views/IvleView.fxml"));
            Parent ivleView = loader.load();
            Scene ivleViewScene = new Scene(ivleView, 1300, 600);
            ivleViewScene.setUserData(loader);
            view.setIvleView(ivleViewScene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
