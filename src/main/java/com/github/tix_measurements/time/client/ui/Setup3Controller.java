package com.github.tix_measurements.time.client.ui;

import com.github.tix_measurements.time.client.Main;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Setup3Controller {

    @FXML
    private Text savedUsername;

    @FXML
    private Text savedInstallationName;

    @FXML
    private Button closeSetupButton;

    @FXML
    public void initialize(){
        setUsername(Main.preferences.get("username", " "));
        setInstallationName(Main.preferences.get("installationName", ""));
    }

    @FXML
    public void setUsername(String name) {
        savedUsername.setText(name);
    }

    @FXML
    public void setInstallationName(String name) {
        savedInstallationName.setText(name);
    }

    @FXML
    private void closeSetup() { closeWindow(closeSetupButton); }

    @FXML
    private void help() {
        try {
            Desktop.getDesktop().browse(new URI("http://tix.innova-red.net/"));
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
    }

    private void closeWindow(Button button) {
        button.getScene().getWindow().hide();
    }
}
