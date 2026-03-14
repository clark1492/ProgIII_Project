package it.unito.clientmail.controller;

import it.unito.clientmail.model.LoginModel;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

  @FXML private AnchorPane    loginPane;
  @FXML private AnchorPane    signInPane;
  @FXML private Label         messageLabel;
  @FXML private TextField     emailField;
  @FXML private PasswordField passwordField;
  @FXML private TextField     newEmailField;
  @FXML private PasswordField newPasswordField;
  @FXML private PasswordField newPassConf;
  @FXML private ImageView     attentionImg;

  private LoginModel model;

  @FXML
  protected void loginRequest() {
    // FIX: i campi vengono letti QUI sul thread UI, prima di entrare nel thread
    // — TextField non può essere letto da thread in background
    String email    = emailField.getText();
    String password = passwordField.getText();

    new Thread(() -> {
      try {
        model.loginRequest(email, password);
      } catch (IOException | ClassNotFoundException e) {
        handleConnectionError(e);
      }
    }).start();
  }

  @FXML
  protected void newUserOnAction() {
    emailField.clear();
    passwordField.clear();
    messageLabel.setText("");
    loginPane.setVisible(false);
    signInPane.setVisible(true);
  }

  @FXML
  protected void signInOnAction() {
    // FIX: stessa cosa — lettura dei campi sul thread UI
    String email        = newEmailField.getText();
    String password     = newPasswordField.getText();
    String confirmation = newPassConf.getText();

    new Thread(() -> {
      try {
        model.signUpRequest(email, password, confirmation);
        Platform.runLater(() -> {
          newEmailField.clear();
          newPasswordField.clear();
          newPassConf.clear();
        });
      } catch (IOException | ClassNotFoundException e) {
        handleConnectionError(e);
      }
    }).start();
  }

  @FXML
  protected void backOnAction() {
    signInPane.setVisible(false);
    loginPane.setVisible(true);
    messageLabel.setText("");
    newEmailField.clear();
    newPasswordField.clear();
    newPassConf.clear();
  }

  private void noService() {
    loginPane.setVisible(false);
    signInPane.setVisible(false);
    messageLabel.setText("SERVER UNREACHABLE");
    attentionImg.setVisible(true);
  }

  private void onService() {
    messageLabel.setText("");
    attentionImg.setVisible(false);
    loginPane.setVisible(true);
    signInPane.setVisible(false);
  }

  private void errorNotification() {
    noService();
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Error");
    alert.setHeaderText("Server unreachable");
    ButtonType retryButton = new ButtonType("Retry");
    alert.getButtonTypes().setAll(retryButton);
    alert.showAndWait().ifPresent(response -> {
      if (response == retryButton) {
        try {
          model = new LoginModel();
          model.messageProperty().bindBidirectional(messageLabel.textProperty());
          onService();
        } catch (IOException e) {
          Platform.runLater(this::errorNotification);
        }
      }
    });
  }

  private void handleConnectionError(Exception e) {
    System.out.println(e.getMessage());
    Platform.runLater(this::errorNotification);
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    if (this.model != null)
      throw new IllegalStateException("Model can only be initialized once");

    try {
      model = new LoginModel();
      model.messageProperty().bindBidirectional(messageLabel.textProperty());
      onService();
    } catch (IOException e) {
      errorNotification();
      System.out.println(e.getMessage());
    }
  }
}