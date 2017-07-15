package com.github.tix_measurements.time.client.ui;

import com.github.tix_measurements.time.client.Main;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Setup1Controller {

    @FXML
    private TextField email;
    @FXML
    private PasswordField password;
    @FXML
    private Text status;
    @FXML
    private Button connectButton;
    @FXML
    private Hyperlink cancelLink;

    @FXML
    private void connect() throws IOException, InterruptedException {
        String emailInput = email.getText();
        String passwordInput = password.getText();
        if (emailInput.isEmpty() || passwordInput.isEmpty()) {
            status.setText("Debe completar ambos campos");
        } else {
            try {
                status.setText("Iniciando sesión...");

                SSLContext sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(null, (certificate, authType) -> true).build();

                CloseableHttpClient client = HttpClients.custom()
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier(new NoopHostnameVerifier())
                        .build();

                HttpPost request = new HttpPost("https://tix.innova-red.net/api/login");
                String json = "{\"username\": \"" + emailInput + "\",\"password\": \"" + passwordInput + "\"}";
                StringEntity params = new StringEntity(json, org.apache.http.entity.ContentType.APPLICATION_JSON);
                request.setHeader("Content-Type", "application/json");
                request.setEntity(params);

                HttpResponse response = client.execute(request);
                final int responseStatusCode = response.getStatusLine().getStatusCode();

                if (responseStatusCode == 401) {
                    // login details are incorrect
                    status.setText("Verifique los datos ingresados");
                } else if (responseStatusCode == 200) {
                    try {
                        String entity = EntityUtils.toString(response.getEntity());
                        JSONObject responseBodyJson = new JSONObject(entity);

                        final int userID = responseBodyJson.getInt("id");
                        Main.preferences.putInt("userID", userID);
                        final String token = responseBodyJson.getString("token");
                        Main.preferences.put("token", token);
                    } catch (Exception e) {
                        status.setText("Verifique los datos ingresados");
                        System.out.println("API responded with unexpected format " + e);
                    }

                    // login succeeded
                    Main.preferences.put("username", emailInput.trim());
                    try {
                        Parent page = FXMLLoader.load(getClass().getResource("/fxml/setup2.fxml"));
                        connectButton.getScene().setRoot(page);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    status.setText("Falló la conexión con el servidor");
                }

            } catch (Exception ex) {
                System.out.println("could not connect " + ex);
            }
        }
    }

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

    @FXML
    private void cancel() {
        closeWindow(cancelLink);
    }

    private void closeWindow(Hyperlink link) {
        link.getScene().getWindow().hide();
    }

}
