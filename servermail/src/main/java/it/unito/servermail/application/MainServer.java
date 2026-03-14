package it.unito.servermail.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class MainServer extends Application {

  @Override
  public void start(Stage stage) {
    try {
      FXMLLoader fxmlLoader = new FXMLLoader(MainServer.class.getResource("server.fxml"));
      Scene scene = new Scene(fxmlLoader.load());
      stage.setTitle("UniTo Mail — Server");
      stage.setScene(scene);
      stage.setResizable(true);
      stage.setMinWidth(400);
      stage.setMinHeight(300);
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    launch();
  }
}