package it.unito.clientmail.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Login extends Application {

  // FIX: volatile — stg viene scritto dal thread JavaFX e letto da altri thread
  // (es. LoginModel.clientScene())
  private static volatile Stage stg;

  public static Stage getStage() {
    return stg;
  }

  @Override
  public void start(Stage stage) {
    stg = stage;
    try {
      FXMLLoader fxmlLoader = new FXMLLoader(Login.class.getResource("login.fxml"));
      Scene scene = new Scene(fxmlLoader.load());
      stage.setTitle("UniTo Mail — Login");
      stage.setScene(scene);
      stage.setResizable(false);
      stage.show();
    } catch (IOException e) {
      System.err.println("[Login] Failed to load login.fxml: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    launch();
  }
}