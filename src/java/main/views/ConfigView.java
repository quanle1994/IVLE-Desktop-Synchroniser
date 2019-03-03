package main.views;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.event.ActionEvent;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;
import main.Main;
import main.controllers.Controller;
import main.controllers.IvleDownloader;
import main.io.AuthFile;
import main.utils.InitConfig;

import java.io.File;
import java.util.Map;

public class ConfigView implements View{
    public TextField pathTextfield;
    public AnchorPane anchorId;
    public Button proceedButton;
    private String rootDirectoryPath;
    private Scene webViewScene;
    private Scene ivleViewScene;

    public void initialize(){
        InitConfig init = new InitConfig();
        if (init.exists()) {
            proceedButton.setVisible(true);
            AuthFile.refreshScanner();
            AuthFile.nextLine();
            AuthFile.nextLine();
            rootDirectoryPath= AuthFile.nextLine();
            pathTextfield.setText(rootDirectoryPath);
            setConfig(false);
        }
        else proceedButton.setVisible(false);
    }

    public void browseDirectory(ActionEvent actionEvent) {
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        Stage stage = (Stage) anchorId.getScene().getWindow();
        File file = directoryChooser.showDialog(stage);
        if (file != null) {
            rootDirectoryPath = file.getAbsolutePath();
            pathTextfield.setText(rootDirectoryPath);
            proceedButton.setVisible(true);
        }
    }

    public void setAuthenticationAttributes(ActionEvent actionEvent) {
        setConfig(true);
    }

    private void setConfig(boolean writeFile) {
        Controller controller = Controller.getInstance();
        controller.setSyncRootDirectory(rootDirectoryPath.replace("\\", "/"));
        AuthFile.refreshScanner();
        String apiKey = AuthFile.nextLine();
        String authToken = AuthFile.nextLine();
        controller.setApiKey(apiKey);
        controller.setAuthToken(authToken);
        if (!writeFile) return;
        AuthFile.updateFile(apiKey, authToken, rootDirectoryPath);
        String validateString = new IvleDownloader().validateToken();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> response
                    = objectMapper.readValue(validateString, new TypeReference<Map<String, Object>>() {
            });
            boolean success = Boolean.parseBoolean(response.get("Success").toString());
            System.out.println("Success = " + success);
            Stage stage = (Stage) anchorId.getScene().getWindow();
            if (!authToken.isEmpty() && success) {
                Main.setIvleView(this);
                stage.setScene(ivleViewScene);
            }
            else {
                Main.setWebView(this);
                stage.setScene(webViewScene);
            }
            Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
            stage.setX((primScreenBounds.getWidth() - stage.getWidth()) / 2);
            stage.setY((primScreenBounds.getHeight() - stage.getHeight()) / 2);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void setWebView(Scene webViewScene) {
        this.webViewScene = webViewScene;
    }

    @Override
    public void setIvleView(Scene ivleViewScene) {
        this.ivleViewScene = ivleViewScene;
    }
}
