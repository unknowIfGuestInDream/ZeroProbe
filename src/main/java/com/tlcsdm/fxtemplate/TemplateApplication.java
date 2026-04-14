package com.tlcsdm.fxtemplate;

import com.tlcsdm.fxtemplate.config.AppSettings;
import com.tlcsdm.fxtemplate.config.I18N;
import com.tlcsdm.fxtemplate.controller.MainController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Main JavaFX Application for the template tool.
 */
public class TemplateApplication extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateApplication.class);

    private MainController controller;

    @Override
    public void init() {
        // Apply saved theme before UI is created
        AppSettings.getInstance().applyInitialSettings();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        primaryStage.initStyle(StageStyle.UNDECORATED);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("main.fxml"));
        loader.setResources(I18N.getBundle());
        Parent root = loader.load();
        controller = loader.getController();
        controller.setPrimaryStage(primaryStage);

        Scene scene = new Scene(root, 900, 700);

        primaryStage.setTitle(I18N.get("app.title"));
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);

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
}
