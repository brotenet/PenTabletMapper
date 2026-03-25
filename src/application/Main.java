package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
    	stage.getIcons().add(new Image(getClass().getResource("/resources/img/icon.png").toString()));
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/resources/FrmMain.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 700, 400);
        scene.getStylesheets().add(getClass().getResource("/resources/css/modena-dark.css").toExternalForm());
        stage.setTitle("X-Input: Map Pen Tablet to Display(s)");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
