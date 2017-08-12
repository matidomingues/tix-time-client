package com.github.tix_measurements.time.client;

import com.github.tix_measurements.time.client.reporting.Reporter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.prefs.Preferences;

public class Main extends Application {

    public static Reporter reporter;
    public static Preferences preferences = Preferences.userRoot().node("/com/tix/client50");

    // application stage is stored so that it can be shown and hidden based on system tray icon operations.
    private Stage aboutStage;
    private Stage prefStage;

    /**
     * Sets up the javafx application.
     * The tray icon will appear, but the preferences window will only be shown
     * if there is no preferences stored in the user's computer.
     */
    @Override
    public void start(final Stage stage) throws IOException {
        setUIProperties(stage);

        if (!installationExists()) {
            showSetupStage();
        } else {
            startReporting();
            showLoggedInStage();
        }
    }

    public static void startReporting() {
        reporter = new Reporter();
        if (reporter.getState() != Worker.State.RUNNING) {
            reporter.start();
        }
    }

    /**
     * Sets up the main UI components, including the tray icon.
     */
    private void setUIProperties(final Stage stage) throws IOException {
        // stores a reference to the aboutStage.
        this.aboutStage = stage;
        aboutStage.setTitle("Sobre TiX");

        // instructs the javafx system not to exit implicitly when the last application window is shut.
        Platform.setImplicitExit(false);

        // sets up the tray icon (using awt code run on the swing thread).
        javax.swing.SwingUtilities.invokeLater(this::addAppToTray);

        Parent aboutParent = FXMLLoader.load(getClass().getResource("/fxml/about.fxml"));
        Scene aboutScene = new Scene(aboutParent);
        aboutStage.setScene(aboutScene);

        prefStage = new Stage();
        prefStage.setTitle("Preferencias");

        Parent prefParent = FXMLLoader.load(getClass().getResource("/fxml/setup1.fxml"));
        Scene prefScene = new Scene(prefParent);
        prefStage.setScene(prefScene);
    }

    /**
     * Checks whether user has already set up the application before.
     */
    private boolean installationExists() {

        int userID = 0;
        byte[] keyPair = null;
        int installationID = 0;
        try {
            userID = preferences.getInt("userID", 0);
            keyPair = preferences.getByteArray("keyPair", null);
            installationID = preferences.getInt("installationID", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (userID > 0) && (keyPair != null) && (installationID > 0);
    }

    /**
     * Sets up a system tray icon for the application.
     */
    private void addAppToTray() {
        try {
            // ensure awt toolkit is initialized.
            java.awt.Toolkit.getDefaultToolkit();

            // app requires system tray support, just exit if there is no support.
            if (!java.awt.SystemTray.isSupported()) {
                System.out.println("No system tray support, application exiting.");
                Platform.exit();
                System.exit(0);
            }

            // set up a system tray icon.
            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();

            java.awt.Image image = ImageIO.read(getClass().getClassLoader().getResource("images/logo.png"));
            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(image);

            // if the user double-clicks on the tray icon, show the main app aboutStage.
            trayIcon.addActionListener(event -> Platform.runLater(this::showAboutStage));

            // if the user selects the default menu item (which includes the app name),
            // show the main app aboutStage.
            java.awt.MenuItem openItem = new java.awt.MenuItem("Sobre TiX");
            openItem.addActionListener(event -> Platform.runLater(this::showAboutStage));

            java.awt.MenuItem openPrefs = new java.awt.MenuItem("Preferencias");
            openPrefs.addActionListener(event -> Platform.runLater(this::showSetupStage));

            // to really exit the application, the user must go to the system tray icon
            // and select the exit option, this will shutdown JavaFX and remove the
            // tray icon (removing the tray icon will also shut down AWT).
            java.awt.MenuItem exitItem = new java.awt.MenuItem("Salir");
            exitItem.addActionListener(event -> {
                Platform.exit();
                tray.remove(trayIcon);
                System.exit(0);
            });

            // setup the popup menu for the application.
            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(openItem);
            popup.addSeparator();
            popup.add(openPrefs);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

            // add the application tray icon to the system tray.
            tray.add(trayIcon);
        } catch (java.awt.AWTException | IOException e) {
            System.out.println("Unable to init system tray");
            e.printStackTrace();
        }
    }

    /**
     * Shows the application aboutStage and ensures that it is brought at the front of all stages.
     */
    private void showAboutStage() {
        if (aboutStage != null) {
            aboutStage.show();
            aboutStage.toFront();
        }
    }

    /**
     * Shows the application prefStage and ensures that it is brought at the front of all stages.
     */
    private void showSetupStage() {
        if (prefStage != null) {
            prefStage.show();
            prefStage.toFront();
        }
    }

    private void showLoggedInStage() throws IOException {
        if (prefStage != null) {
            Parent prefParent = FXMLLoader.load(getClass().getResource("/fxml/setup3.fxml"));
            Scene prefScene = new Scene(prefParent);
            prefStage.setScene(prefScene);
        }
    }

    public static void main(String[] args) throws IOException, java.awt.AWTException {
        Main.launch(args);
    }
}