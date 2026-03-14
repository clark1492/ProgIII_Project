package it.unito.clientmail.model;

import it.unito.clientmail.application.Login;
import it.unito.clientmail.controller.ClientController;
import it.unito.servermail.model.User;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class LoginModel {

  private Connection connection;
  private final SimpleStringProperty message;
  private ClientModel model;

  public LoginModel() throws IOException {
    connection = new Connection();
    message = new SimpleStringProperty("");
    model = null;
  }

  public SimpleStringProperty messageProperty() {
    return message;
  }

  public void loginRequest(String email, String password) throws IOException, ClassNotFoundException {

    // FIX: tutti i message.set() e le operazioni UI wrappati in Platform.runLater
    // — questo metodo viene chiamato da un thread in background
    if (email.isEmpty() || password.isEmpty()) {
      Platform.runLater(() -> message.set("Please insert a valid email and password"));
      return;
    }

    String serverResp;
    synchronized (connection) {
      connection.write("LOGIN_REQUEST");
      connection.write(email);
      connection.write(password);
      serverResp = (String) connection.read();
    }

    switch (serverResp) {
      case "INCORRECT_PASSWORD" -> Platform.runLater(() -> message.set("Wrong password"));
      case "USER_NOT_EXIST"     -> Platform.runLater(() -> message.set("User does not exist"));
      case "SERVER_SUCCESS"     -> {
        // La lettura dell'utente avviene ancora nel thread di rete
        User user = (User) connection.read();
        ClientModel clientModel = new ClientModel(user, connection);
        // FIX: clientScene tocca lo Stage — deve stare su Platform.runLater
        Platform.runLater(() -> {
          try {
            clientScene(clientModel);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
      }
    }
  }

  public void signUpRequest(String email, String password, String confirmation)
      throws IOException, ClassNotFoundException {

    if (email.isEmpty() || password.isEmpty()) {
      Platform.runLater(() -> message.set("Please choose a valid email and a password"));
      return;
    }

    if (!password.equals(confirmation)) {
      Platform.runLater(() -> message.set("Passwords do not match"));
      return;
    }

    if (!User.validateEmail(email)) {
      Platform.runLater(() -> message.set("Email format is not valid"));
      return;
    }

    String serverResp;
    synchronized (connection) {
      connection.write("SIGNIN_REQUEST");
      connection.write(email);
      connection.write(password);
      serverResp = (String) connection.read();
    }

    // FIX: lo switch aggiorna message — deve stare su Platform.runLater
    final String resp = serverResp;
    Platform.runLater(() -> {
      switch (resp) {
        case "SERVER_SUCCESS"          -> message.set("Registration completed");
        case "USER_ALREADY_REGISTERED" -> message.set("User already registered");
        case "SERVER_UNSUCCESS"        -> message.set("Registration failed, please try again");
      }
    });
  }

  public void clientScene(ClientModel model) throws IOException {

    Stage stage = Login.getStage();

    FXMLLoader fxmlLoader = new FXMLLoader(Login.class.getResource("client.fxml"));
    Scene scene = new Scene(fxmlLoader.load());
    ClientController controller = fxmlLoader.getController();
    controller.initModel(model);

    stage.setTitle("UniTo Mail — " + model.getUser().getEmail());
    stage.setScene(scene);
    stage.setResizable(true);
    stage.setMinWidth(900);
    stage.setMinHeight(600);
    stage.show();

    stage.setOnCloseRequest(event -> {
      model.logoutRequest();
      event.consume();
    });
  }
}