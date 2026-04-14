package com.tlcsdm.zeroprobe;

import com.tlcsdm.zeroprobe.config.AppSettings;
import com.tlcsdm.zeroprobe.config.I18N;
import com.tlcsdm.zeroprobe.controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Main JavaFX Application for ZeroProbe.
 */
public class ZeroProbeApplication extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(ZeroProbeApplication.class);

    private MainController controller;
    private static Stage primaryStageRef;

    /**
     * Get the application logo image, or null if not found.
     */
    public static Image loadLogo() {
        InputStream is = ZeroProbeApplication.class.getResourceAsStream("logo.png");
        return is != null ? new Image(is) : null;
    }

    @Override
    public void init() {
        // Apply saved theme before UI is created
        AppSettings.getInstance().applyInitialSettings();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStageRef = primaryStage;
        primaryStage.initStyle(StageStyle.UNDECORATED);

        Image logo = loadLogo();
        if (logo != null) {
            primaryStage.getIcons().add(logo);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        loader.setResources(I18N.getBundle());
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root, 900, 700);

        primaryStage.setTitle(I18N.get("app.title"));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

        controller.setPrimaryStage(primaryStage);

        primaryStage.setOnCloseRequest(event -> {
            if (controller != null) {
                controller.shutdown();
            }
            Platform.exit();
        });

        primaryStage.show();
        LOG.info("Application started");
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Restart the application by closing current stage and opening a new one.
     */
    public static void restart() {
        if (primaryStageRef != null) {
            primaryStageRef.close();
        }
        Platform.runLater(() -> {
            try {
                ZeroProbeApplication app = new ZeroProbeApplication();
                Stage newStage = new Stage();
                app.init();
                app.start(newStage);
            } catch (Exception e) {
                LOG.error("Failed to restart application", e);
                Platform.exit();
            }
        });
    }
}
