package it.unito.servermail.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainServer extends Application{

  public void start(Stage stage) {

    FXMLLoader fxmlLoader = new FXMLLoader(MainServer.class.getResource("server.fxml"));
    Scene scene;
    try {
      scene = new Scene(fxmlLoader.load());
      stage.setTitle("Server");
      stage.setScene(scene);
      stage.setResizable(false);
      stage.show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    launch();
  }
}

