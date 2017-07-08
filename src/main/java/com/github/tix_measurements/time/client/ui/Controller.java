package com.github.tix_measurements.time.client.ui;

import com.github.tix_measurements.time.core.util.TixCoreUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import org.apache.commons.lang3.SerializationUtils;
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
import java.security.KeyPair;
import java.util.prefs.Preferences;

public class Controller {

    @FXML
    private Button connectButton;
    @FXML
    private Button createInstallationButton;
    @FXML
    private Hyperlink cancelLink;
    @FXML
    private Button closeSetupButton;
    @FXML
    private TextField email;
    @FXML
    private PasswordField password;
    @FXML
    private Text loginStatus;
    @FXML
    private TextField installationName;

    @FXML
    private void connect() throws IOException, InterruptedException {
        Preferences prefs = Preferences.userRoot().node("/com/tix/client");
        String emailInput = email.getText();
        String passwordInput = password.getText();
        if (emailInput.isEmpty() || passwordInput.isEmpty()) {
            loginStatus.setText("Debe completar ambos campos");
        } else {
            try {
                System.out.println(loginStatus.getText());
                loginStatus.setText("Iniciando sesión...");

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

                String entity = EntityUtils.toString(response.getEntity());
                JSONObject responseBodyJson = new JSONObject(entity);
                System.out.println(responseBodyJson.getInt("id"));

                final int userID = responseBodyJson.getInt("id");
                prefs.putInt("userid", userID);
                final String token = responseBodyJson.getString("token");
                prefs.put("token", token);

                System.out.println(response.getStatusLine().getStatusCode());

                final int responseStatusCode = response.getStatusLine().getStatusCode();

                if (responseStatusCode == 401) {
                    // login details are incorrect
                    loginStatus.setText("Verifique los datos ingresados");
                } else if (responseStatusCode == 200) {
                    // login succeeded
                    prefs.put("username", emailInput.trim());
                    prefs.put("password", passwordInput.trim());
                    try {
                        Parent page = FXMLLoader.load(getClass().getResource("/fxml/setup2.fxml"));
                        connectButton.getScene().setRoot(page);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    loginStatus.setText("Falló la conexión con el servidor");
                }

            } catch (Exception ex) {
                System.out.println("could not connect " + ex);
            }
        }
    }

    @FXML
    private void createInstallation() throws IOException, InterruptedException {
        Preferences prefs = Preferences.userRoot().node("/com/tix/client");
        try {
            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (certificate, authType) -> true).build();

            CloseableHttpClient client = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build();

            final int userID = prefs.getInt("userid", 0);
            final byte[] keyPairBytes = prefs.getByteArray("keyPair", SerializationUtils.serialize(TixCoreUtils.NEW_KEY_PAIR.get()));
            final KeyPair keyPair = SerializationUtils.deserialize(keyPairBytes);
            final String token = prefs.get("token", null);
            final String installationInput = installationName.getText();

            if (userID != 0 && keyPair != null && token != null && installationInput != null) {

                HttpPost request = new HttpPost("https://tix.innova-red.net/api/user/" + userID + "/installation");
                String json = "{\"name\": \"" + installationInput + "\",\"publickey\": \"" + keyPair.getPublic().getEncoded() + "\"}";
                StringEntity params = new StringEntity(json, org.apache.http.entity.ContentType.APPLICATION_JSON);
                request.setHeader("Content-Type", "application/json");
                request.setHeader("Authorization", "JWT " + token);
                request.setEntity(params);

                HttpResponse response = client.execute(request);

                final int responseStatusCode = response.getStatusLine().getStatusCode();
                if (responseStatusCode == 401) {
                    // login details are incorrect
                    loginStatus.setText("Verifique los datos ingresados");
                } else if (responseStatusCode == 200) {
                    // login succeeded
                    String entity = EntityUtils.toString(response.getEntity());
                    JSONObject responseBodyJson = new JSONObject(entity);
                    System.out.println(responseBodyJson.getInt("id"));

                    final int installationID = responseBodyJson.getInt("id");
                    prefs.putInt("installationID", installationID);
                    try {
                        Parent page = FXMLLoader.load(getClass().getResource("/fxml/setup3.fxml"));
                        createInstallationButton.getScene().setRoot(page);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    loginStatus.setText("Falló la conexión con el servidor");
                }
            }
        } catch (Exception ex) {
            System.out.println("could not connect " + ex);
        }
    }

    @FXML
    private void cancel() {
        closeWindow(cancelLink);
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
    private void closeSetup() {
        closeWindow(closeSetupButton);
    }

    @FXML
    private void getUpdates() {
        try {
            Desktop.getDesktop().browse(new URI("http://tix.innova-red.net/"));
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
    }

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

    private void closeWindow(Hyperlink link) {
        link.getScene().getWindow().hide();
    }

    private void closeWindow(Button button) {
        button.getScene().getWindow().hide();
    }

}