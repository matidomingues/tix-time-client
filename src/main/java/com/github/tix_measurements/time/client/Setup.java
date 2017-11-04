package com.github.tix_measurements.time.client;

import com.github.tix_measurements.time.core.util.TixCoreUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.net.ssl.SSLContext;
import java.security.Key;
import java.security.KeyPair;
import java.util.Base64;

public class Setup {

    private static final Logger logger = LogManager.getLogger();

    public static LoginDetails login(String username, String password) {
        try {
            LoginDetails loginDetails = new LoginDetails();
            final SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (certificate, authType) -> true).build();

            final CloseableHttpClient client = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build();

            final HttpPost request = new HttpPost("https://tix.innova-red.net/api/login");
            final String json = "{\"username\": \"" + username + "\",\"password\": \"" + password + "\"}";
            final StringEntity params = new StringEntity(json, org.apache.http.entity.ContentType.APPLICATION_JSON);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(params);
            final HttpResponse response = client.execute(request);
            int responseStatusCode = response.getStatusLine().getStatusCode();
            if (responseStatusCode == 401) {
                // login details are incorrect
                logger.error("Incorrect username or password");
            } else if (responseStatusCode == 200) {
                try {
                    logger.info("Log in succeeded");
                    loginDetails.username = username;

                    final String entity = EntityUtils.toString(response.getEntity());
                    final JSONObject responseBodyJson = new JSONObject(entity);

                    final int userID = responseBodyJson.getInt("id");
                    loginDetails.userId = userID;
                    final String token = responseBodyJson.getString("token");
                    loginDetails.token = token;
                } catch (Exception e) {
                    logger.error("API responded to login with unexpected format " + e);
                }
            } else {
                logger.error("Connection to server failed");
            }
            return loginDetails;

        } catch (Exception ex) {
            logger.error("could not connect " + ex);
        }
        return null;
    }

    public static InstallationDetails install(final String installation, String token, int userID) {
        try {
            InstallationDetails details = new InstallationDetails();
            final SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (certificate, authType) -> true).build();

            final CloseableHttpClient client = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build();

            final byte[] keyPairBytes = SerializationUtils.serialize(TixCoreUtils.NEW_KEY_PAIR.get());
            final KeyPair keyPair = SerializationUtils.deserialize(keyPairBytes);
            details.keyPair = keyPair;
            final String installationInput = installation.trim().replace("\"", "\\\"");

            if (userID != 0 && keyPair != null && token != null && installationInput != null) {

                final HttpPost request = new HttpPost("https://tix.innova-red.net/api/user/" + userID + "/installation");

                final byte[] pubBytes = Base64.getEncoder().encode(keyPair.getPublic().getEncoded());
                final String publicString = new String(pubBytes);

                final String json = "{\"name\": \"" + installationInput + "\",\"publickey\": \"" + publicString + "\"}";
                final StringEntity params = new StringEntity(json, org.apache.http.entity.ContentType.APPLICATION_JSON);
                request.setHeader("Content-Type", "application/json");
                request.setHeader("Authorization", "JWT " + token);
                request.setEntity(params);

                final HttpResponse response = client.execute(request);

                final int responseStatusCode = response.getStatusLine().getStatusCode();

                if (responseStatusCode == 401) {
                    // new installation details are incorrect
                    logger.error("Verify data sent for installation creation.");
                } else if (responseStatusCode == 200) {
                    logger.info("Installation created succesfully");
                    final String entity = EntityUtils.toString(response.getEntity());
                    final JSONObject responseBodyJson = new JSONObject(entity);
                    final int installationID = responseBodyJson.getInt("id");
                    details.installationId = installationID;
                    return details;
                } else {
                    logger.error("Server connection failed when creating installation.");
                }
            }
        } catch (Exception ex) {
            logger.error("could not connect " + ex);
        }
        return null;
    }

    public static boolean cliLogin(final String username, final String password) {
        final LoginDetails responseStatusCode = login(username, password);
        return true;
    }

    public static boolean cliInstall(final String installation, final int port) {
        final InstallationDetails responseStatusCode = install(installation, "token", 12);
        Main.preferences.putInt("clientPort", port);
        return true;
    }

}
