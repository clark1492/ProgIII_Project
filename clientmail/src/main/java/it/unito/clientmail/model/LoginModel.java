package it.unito.clientmail.model;

import it.unito.clientmail.application.Login;
import it.unito.clientmail.controller.ClientController;
import it.unito.servermail.model.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LoginModel {

  private Connection connection;
  private SimpleStringProperty message ;
  private ClientModel model;

  private static final String EMAIL_PATTERN = "^\\w+@[a-zA-Z_]+?\\.[a-zA-Z]{2,3}$";

  public LoginModel() throws IOException, ClassNotFoundException {

    connection = new Connection("127.0.0.1", 8189);
    message = new SimpleStringProperty("");
    model = null;
  }
  public SimpleStringProperty messageProperty() {
    return message;
  }

  public void loginRequest(String email,String password) throws IOException, ClassNotFoundException {


    if (email.isEmpty() || password.isEmpty()) {
      message.set("Please insert a valid email and password");
      return;
    }
    String serverResp;

    synchronized (connection) {
      connection.write("LOGIN_REQUEST");
      connection.write(email);
      connection.write(password);
      serverResp = (String) connection.read();
    }

    switch(serverResp){
      case "UNCORRECT_PASSSWORD" -> {
        message.set("Wrong password");
      }
      case "USER_NOT_EXIST" -> {
        message.set("User does not exist ");
      }
      case "SERVER_SUCCESS" -> {
        User user = (User) connection.read();
        model = new ClientModel(user, connection);
        clientScene(model);
      }
    }
  }

  public void signUpRequest(String email,String password,String confermation) throws IOException, ClassNotFoundException {

    if(password.isEmpty() || email.isEmpty()){
      message.set("Please choose a valid email and a password ");
      return;
    }

    if(!password.equals(confermation)){
      message.set("Password does not match");
      return;
    }

    Pattern emailPattern = Pattern.compile(EMAIL_PATTERN, Pattern.CASE_INSENSITIVE);
    Matcher emailMatcher = emailPattern.matcher(email);

    if(!emailMatcher.find()){
      message.set("Email pattern is not correct");
    }

    else {
      String serverResp;

      synchronized (connection) {
        connection.write("SIGNIN_REQUEST");
        connection.write(email);
        connection.write(password);

        serverResp = (String) connection.read();
      }
      switch (serverResp) {
        case "SERVER_SUCCESS" -> {
          message.set("Registration completed");
        }

        case "USER_ALREADY_REGISTERED" -> {
          message.set("User already registered");
        }

        case "SERVER_UNSUCCES" -> {
          message.set("Registration problems");
        }
      }
    }
  }

  public void clientScene(ClientModel model) throws IOException {

    Stage stage = Login.getStage();

    FXMLLoader fxmlLoader = new FXMLLoader(Login.class.getResource("client.fxml"));
    Scene scene = new Scene(fxmlLoader.load());
    ClientController controller = fxmlLoader.getController();
    controller.initModel(model);
    stage.setTitle("Unito - " + model.getUser().getEmail());
    stage.setScene(scene);
    stage.setResizable(false);
    stage.show();

    stage.setOnCloseRequest(event -> {
      model.logoutRequest();
      event.consume();});
  }
}


