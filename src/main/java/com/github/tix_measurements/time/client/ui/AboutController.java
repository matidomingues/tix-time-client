package com.github.tix_measurements.time.client.ui;

import javafx.fxml.FXML;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class AboutController {

    @FXML
    private void openWebsite() {
        try {
            Desktop.getDesktop().browse(new URI("http://tix.innova-red.net/"));
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
    }
}
