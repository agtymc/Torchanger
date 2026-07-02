package org.agty.torchanger.torchanger.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.agty.torchanger.torchanger.ui.TorchangerController;

import java.io.IOException;

public class TorchangerApplication extends Application {
    private TorchangerController controller;

    @Override
    public void start(Stage stage) throws IOException {
        Platform.setImplicitExit(false);
        FXMLLoader fxmlLoader = new FXMLLoader(TorchangerApplication.class.getResource("/org/agty/torchanger/torchanger/main-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1240, 760);
        scene.getStylesheets().add(TorchangerApplication.class.getResource("/org/agty/torchanger/torchanger/theme.css").toExternalForm());
        controller = fxmlLoader.getController();
        stage.getIcons().add(AppIconFactory.createFxIcon());
        controller.attachStage(stage);
        stage.setTitle(AppInfo.displayName());
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
        Platform.runLater(() -> {
            Rectangle2D bounds = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight())
                    .stream()
                    .findFirst()
                    .orElse(Screen.getPrimary())
                    .getVisualBounds();
            stage.setX(bounds.getMinX() + (bounds.getWidth() - stage.getWidth()) / 2.0);
            stage.setY(bounds.getMinY() + (bounds.getHeight() - stage.getHeight()) / 2.0);
        });
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
        Platform.setImplicitExit(true);
    }
}
