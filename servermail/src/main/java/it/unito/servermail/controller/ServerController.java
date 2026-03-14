package it.unito.servermail.controller;

import it.unito.servermail.model.ServerModel;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import java.io.IOException;

public class ServerController {

  @FXML
  private TextArea logsArea;
  private ServerModel model;

  @FXML
  public void initialize() {

    if (this.model != null)
      throw new IllegalStateException("Model can only be initialized once");

    try {
      model = new ServerModel();
    } catch (IOException e) {

      e.printStackTrace();
      logsArea.appendText("[ERROR] Failed to start server: " + e.getMessage() + "\n");
      return;
    }

    model.logTextProperty().addListener((obs, oldString, newString) -> {
      if (newString != null)
        logsArea.appendText(newString);
    });

    Thread server = new Thread(model);
    server.setDaemon(true);

    server.setName("ServerMain");
    server.start();
  }
}