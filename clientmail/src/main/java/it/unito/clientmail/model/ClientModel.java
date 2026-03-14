package it.unito.clientmail.model;

import it.unito.servermail.model.Email;
import it.unito.servermail.model.Folder;
import it.unito.servermail.model.User;
import it.unito.clientmail.application.Login;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.util.LinkedList;

public class ClientModel {

  private final User user;
  private volatile Connection connection;

  private final ListProperty<Email> emailListProperty;
  private final ObservableList<Email> currentEmailList;
  private final ObjectProperty<Folder> currentFolder;
  private final ObjectProperty<Email> currentMail;

  public ClientModel(User user, Connection connection) {
    this.user = user;
    this.connection = connection;
    currentFolder = new SimpleObjectProperty<>();
    currentMail = new SimpleObjectProperty<>();
    currentEmailList = FXCollections.observableList(new LinkedList<>());
    emailListProperty = new SimpleListProperty<>(currentEmailList);
  }

  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public void setCurrentEmailList(LinkedList<Email> list) {

    Platform.runLater(() -> {
      if (list == null)
        currentEmailList.clear();
      else
        currentEmailList.setAll(list);
    });
  }

  public User getUser() {
    return user;
  }

  public Email getCurrentMail() {
    return currentMail.get();
  }

  public ObjectProperty<Email> currentMailProperty() {
    return currentMail;
  }

  public ListProperty<Email> emailListProperty() {
    return emailListProperty;
  }

  public void setCurrentMail(Email mail) {
    currentMail.set(mail);
  }

  public void setCurrentFolder(Folder folder) {
    currentFolder.set(folder);
  }

  public LinkedList<Email> getNews() throws IOException, ClassNotFoundException {

    Object data;
    synchronized (connection) {
      connection.write("KEEP_UPDATE");
      connection.write(user);
      data = connection.read();
    }

    if (!(data instanceof LinkedList<?>))
      throw new InvalidObjectException("[getNews]: unexpected response type");

    return (LinkedList<Email>) data;
  }

  public LinkedList<Email> mailboxRequest(Folder folder) throws IOException, ClassNotFoundException {

    if (folder == Folder.WRITE)
      return new LinkedList<>();

    Object data;
    synchronized (connection) {
      connection.write("MAILBOX_REQUEST");
      connection.write(folder);
      data = connection.read();
    }

    if (!(data instanceof LinkedList<?>))
      throw new InvalidObjectException("[mailboxRequest]: unexpected response type");

    return (LinkedList<Email>) data;
  }

  public boolean sendEmail(Email toSend) throws IOException, ClassNotFoundException {

    if (toSend == null)
      throw new IllegalArgumentException("[sendEmail]: toSend is null");

    String resp;
    synchronized (connection) {
      connection.write("SEND_EMAIL");
      connection.write(toSend);
      resp = serverResponse();
    }

    switch (resp) {
      case "SERVER_SUCCESS" -> {
        infoNotification("Email sent successfully");
        return true;
      }
      case "USER_NOT_EXIST" -> {
        infoNotification("One or more recipients do not exist");
        return false;
      }
      default -> {
        return false;
      }
    }
  }

  public boolean deleteEmail(Email toDelete) throws IOException, ClassNotFoundException {

    if (toDelete == null)
      throw new IllegalArgumentException("[deleteEmail]: toDelete is null");

    String resp;
    synchronized (connection) {
      connection.write("DEL_EMAIL");
      connection.write(toDelete);
      resp = serverResponse();
    }

    switch (resp) {
      case "SERVER_SUCCESS" -> {
        infoNotification("Email permanently deleted");
        return true;
      }
      case "MOVED_BIN" -> {
        infoNotification("Email moved to bin");
        return true;
      }
      default -> {
        return false;
      }
    }
  }

  public boolean notifyReadEmail(Email toNotify) throws IOException, ClassNotFoundException {

    if (toNotify == null)
      throw new IllegalArgumentException("[notifyReadEmail]: toNotify is null");

    String resp;
    synchronized (connection) {
      connection.write("READ_FLAG");
      connection.write(toNotify);
      resp = serverResponse();
    }

    return resp.equals("SERVER_SUCCESS");
  }

  public void logoutRequest() {
    Stage stage = Login.getStage();
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
      alert.setTitle("Logout");
      alert.setHeaderText("You are about to logout.");
      alert.showAndWait().ifPresent(response -> {
        if (response == ButtonType.OK) {
          try {
            synchronized (connection) {
              connection.write("LOG_OUT");
              connection.closeConnection();
            }
            stage.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
    });
  }

  private String serverResponse() throws IOException, ClassNotFoundException {
    return (String) connection.read();
  }

  public void infoNotification(String message) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("Info");
      alert.setHeaderText(message);
      alert.showAndWait();
    });
  }
}