package it.unito.servermail.model;

import it.unito.servermail.handler.HandleClient;
import it.unito.servermail.utils.FilesManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ServerModel implements Runnable {

  private static final int PORT = 8189;

  private final ServerSocket serverSocket;
  private final ArrayList<User> usersList;
  private final SimpleStringProperty logText;

  public ServerModel() throws IOException {
    serverSocket = new ServerSocket(PORT);
    usersList = new ArrayList<>();
    logText = new SimpleStringProperty("");
  }

  public SimpleStringProperty logTextProperty() {
    return logText;
  }

  public void setLogText(String string) {
    Platform.runLater(() -> logText.setValue(string));
  }

  public synchronized boolean addUser(User user) {
    if (user == null)
      throw new IllegalArgumentException("User cannot be null");
    try {
      if (FilesManager.addUserToFile(user)) {

        usersList.add(user);
        return true;
      }
    } catch (IOException | ClassNotFoundException e) {
      System.out.println(e.getMessage());
    }
    return false;
  }

  private synchronized void loadUsersList() {

    usersList.clear();
    usersList.addAll(FilesManager.getUserList());
  }

  public synchronized boolean userExist(String email) {
    if (email == null || email.isEmpty())
      throw new IllegalArgumentException("Email cannot be null or empty");
    for (User user : usersList) {
      if (user.getEmail().equals(email))
        return true;
    }
    return false;
  }

  public synchronized boolean passwordIsCorrect(String email, String password) {
    if (email == null || password == null)
      throw new IllegalArgumentException("Email and password cannot be null");
    User user = getUser(email);

    if (user == null)
      return false;
    return user.getPassword().equals(password);
  }

  public synchronized User getUser(String email) {
    for (User user : usersList) {
      if (user.getEmail().equals(email))
        return user;
    }
    return null;
  }

  private void initialize() {
    try {
      if (!Files.exists(Paths.get(FilesManager.FILES_PATH)))
        FilesManager.serverFiles();

      loadUsersList();

      for (User user : usersList) {
        if (!Files.exists(Paths.get(FilesManager.FILES_PATH + user.getEmail())))
          FilesManager.createFiles(user);
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }

  @Override
  public void run() {
    Thread.currentThread().setName("Server");
    initialize();
    setLogText("[SERVER] : Waiting for client connections...\n");

    try {
      while (true) {
        Socket incoming = serverSocket.accept();
        Thread clientThread = new Thread(new HandleClient(this, incoming));
        clientThread.setName("HandleClient-" + incoming.getInetAddress());
        clientThread.start();
      }
    } catch (IOException e) {
      System.out.println(e.getMessage());
    }
  }
}