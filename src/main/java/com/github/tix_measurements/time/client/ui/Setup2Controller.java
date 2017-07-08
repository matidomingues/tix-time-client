package com.github.tix_measurements.time.client.ui;

import com.github.tix_measurements.time.core.util.TixCoreUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
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
import java.util.Base64;
import java.util.prefs.Preferences;

public class Setup2Controller {

    @FXML
    private Button createInstallationButton;
    @FXML
    private TextField installationName;
    @FXML
    private Text status;
    @FXML
    private Hyperlink cancelLink;

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

            final int userID = prefs.getInt("userID", 0);
            final byte[] keyPairBytes = SerializationUtils.serialize(TixCoreUtils.NEW_KEY_PAIR.get());
            prefs.putByteArray("keyPair", keyPairBytes);
            final KeyPair keyPair = SerializationUtils.deserialize(keyPairBytes);
            final String token = prefs.get("token", null);
            final String installationInput = installationName.getText();

            if (userID != 0 && keyPair != null && token != null && installationInput != null) {

                HttpPost request = new HttpPost("https://tix.innova-red.net/api/user/" + userID + "/installation");

                byte[] pubBytes = Base64.getEncoder().encode(keyPair.getPublic().getEncoded());
                String publicString = new String(pubBytes);
                System.out.println(publicString);

                String json = "{\"name\": \"" + installationInput + "\",\"publickey\": \"" + publicString + "\"}";
                StringEntity params = new StringEntity(json, org.apache.http.entity.ContentType.APPLICATION_JSON);
                request.setHeader("Content-Type", "application/json");
                request.setHeader("Authorization", "JWT " + token);
                request.setEntity(params);

                HttpResponse response = client.execute(request);

                final int responseStatusCode = response.getStatusLine().getStatusCode();

                if (responseStatusCode == 401) {
                    // login details are incorrect
                    status.setText("Verifique los datos ingresados");
                } else if (responseStatusCode == 200) {
                    // login succeeded
                    String entity = EntityUtils.toString(response.getEntity());
                    JSONObject responseBodyJson = new JSONObject(entity);
                    final int installationID = responseBodyJson.getInt("id");
                    prefs.putInt("installationID", installationID);
                    try {

                        // Start main client
                        //new TixTimeClient(TixTimeClient.DEFAULT_SERVER_ADDRESS,TixTimeClient.DEFAULT_CLIENT_PORT);

                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/setup3.fxml"));
                        Parent root = loader.load();
                        Setup3Controller setup3Controller = loader.getController();
                        setup3Controller.setInstallationName(installationName.getText());
                        setup3Controller.setUsername(prefs.get("username"," "));
                        createInstallationButton.getScene().setRoot(root);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    status.setText("Falló la conexión con el servidor");
                }
            }
        } catch (Exception ex) {
            System.out.println("could not connect " + ex);
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
