package it.unito.clientmail.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Login extends Application {

  private static Stage stg;

  public static Stage getStage() {
    return stg;
  }

  @Override
  public void start(Stage stage) {
    try {

      stg = stage;

      FXMLLoader fxmlLoader = new FXMLLoader(Login.class.getResource("login.fxml"));
      Scene scene = new Scene(fxmlLoader.load());
      stage.setTitle("Log In");
      stage.setScene(scene);
      stage.setResizable(false);
      stage.show();
    } catch (IOException e) {
      System.out.println("Finish");
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    launch();
  }
}