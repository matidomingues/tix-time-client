package com.github.tix_measurements.time.client.ui;

import com.github.tix_measurements.time.client.Main;
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
        try {
            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (certificate, authType) -> true).build();

            CloseableHttpClient client = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build();

            final int userID = Main.preferences.getInt("userID", 0);
            final byte[] keyPairBytes = SerializationUtils.serialize(TixCoreUtils.NEW_KEY_PAIR.get());
            Main.preferences.putByteArray("keyPair", keyPairBytes);
            final KeyPair keyPair = SerializationUtils.deserialize(keyPairBytes);
            final String token = Main.preferences.get("token", null);
            final String installationInput = installationName.getText().trim().replace("\"","\\\"");

            if (userID != 0 && keyPair != null && token != null && installationInput != null) {

                HttpPost request = new HttpPost("https://tix.innova-red.net/api/user/" + userID + "/installation");

                byte[] pubBytes = Base64.getEncoder().encode(keyPair.getPublic().getEncoded());
                String publicString = new String(pubBytes);

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
                    Main.preferences.putInt("installationID", installationID);
                    Main.preferences.put("installationName", installationInput);

                    Main.startReporting();

                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/setup3.fxml"));
                        Parent root = loader.load();
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
