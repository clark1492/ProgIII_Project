package it.unito.clientmail.controller;

import it.unito.clientmail.model.*;
import it.unito.servermail.model.Email;
import it.unito.servermail.model.Folder;
import it.unito.servermail.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ClientController {

  private ClientModel model;
  private final SimpleDateFormat dateForm = new SimpleDateFormat("dd/MM/yyyy");
  private static final String SEPARATOR = "*".repeat(75);

  @FXML private Label    lblUsername;
  @FXML private Label    lblFolder;
  @FXML private ListView<Email> lstEmails;

  @FXML private VBox      writeBox;
  @FXML private TextField subjectField;
  @FXML private TextField toField;
  @FXML private Label     fromLabelW;
  @FXML private TextArea  emailAreaW;

  @FXML private VBox     readBox;
  @FXML private Label    subjectLabel;
  @FXML private Label    fromLabelR;
  @FXML private Label    toLabelR;
  @FXML private Label    dateLabel;
  @FXML private TextArea emailAreaR;
  @FXML private Button   replyButton;
  @FXML private Button   replyAllButton;
  @FXML private Button   forwardButton;
  @FXML private Button   deleteButton;
  @FXML private Label    errorLabel;
  @FXML private ImageView errorImg;

  private boolean readMode = true;

  private void readView(boolean read) {
    readMode = read;
    readBox.setVisible(read);
    writeBox.setVisible(!read);
    if (read) {
      subjectField.setEditable(true);
      toField.setEditable(true);
      fromLabelW.setText(model.getUser().getEmail());
      subjectField.clear();
      toField.clear();
      emailAreaW.clear();
    } else {
      subjectLabel.setText("");
      fromLabelR.setText("");
      dateLabel.setText("");
      emailAreaR.clear();
      toLabelR.setText("");
    }
  }

  @FXML protected void newMailOnAction()  { new Thread(new Refresh(Folder.WRITE, null)).start(); }
  @FXML protected void inboxOnAction()    { new Thread(new Refresh(Folder.INBOX, null)).start(); }
  @FXML protected void sentMailOnAction() { new Thread(new Refresh(Folder.SENT,  null)).start(); }
  @FXML protected void trashOnAction()    { new Thread(new Refresh(Folder.BIN,   null)).start(); }
  @FXML protected void logoutOnAction()   { new Thread(new LogOutThread()).start(); }
  @FXML protected void replyOnAction()    { reply(false); }
  @FXML protected void replyAllOnAction() { reply(true);  }

  @FXML
  protected void forwardOnAction() {
    Email original  = model.getCurrentMail();
    Email toForward = new Email();
    toForward.setSender(model.getUser().getEmail());
    toForward.setSubject(original.getSubject().startsWith("FWD: ")
        ? original.getSubject()
        : "FWD: " + original.getSubject());
    String intro = "\n\n" + SEPARATOR + "\n\nBeing forwarded:\n\n"
        + "FROM: "    + original.getSender()               + "\n"
        + "SUBJECT: " + original.getSubject()              + "\n"
        + "DATE: "    + original.getDate()                 + "\n"
        + "TO: "      + getDestsString(original.getDests()) + "\n\n";
    toForward.setContent(intro + original.getContent());
    toForward.setReply(null);
    toForward.setBelonging(Folder.WRITE);
    new Thread(new Refresh(Folder.WRITE, toForward)).start();
  }

  @FXML
  protected void deleteOnAction() {
    new Thread(new DeleteThread(model.getCurrentMail())).start();
  }

  @FXML
  protected void sendOnAction() {
    Email toSend = composeMail(model.getCurrentMail());
    if (toSend != null)
      new Thread(new SendThread(toSend)).start();
  }

  @FXML
  protected void undoOnAction() {
    subjectField.clear();
    toField.clear();
    emailAreaW.clear();
    subjectField.setEditable(true);
    toField.setEditable(true);
    model.setCurrentMail(null);
  }

  private void reply(boolean all) {
    Email toReply      = model.getCurrentMail();
    String myEmail     = model.getUser().getEmail();
    String sender      = toReply.getSender();
    List<String> dests = toReply.getDests();

    Email reply = new Email();
    reply.setReply(toReply);
    reply.setSender(myEmail);
    reply.setId(toReply.getId());
    reply.setSubject(toReply.getSubject().startsWith("RE: ")
        ? toReply.getSubject()
        : "RE: " + toReply.getSubject());

    if (!sender.equals(myEmail)) {
      if (all) {
        List<String> replyDests = new ArrayList<>();
        // FIX: usa stream invece del loop manuale — più leggibile
        dests.stream()
            .filter(d -> !d.equals(myEmail))
            .forEach(replyDests::add);
        replyDests.add(sender);
        reply.setDests(replyDests);
      } else {
        reply.setDests(List.of(sender));
      }
    } else {
      reply.setDests(all ? dests : List.of(dests.get(0)));
    }

  
    String intro = "\n\n" + SEPARATOR + "\n\nOn " + toReply.getDate()
        + " " + sender + " wrote:\n\n";
    reply.setContent(intro + toReply.getContent());
    reply.setBelonging(Folder.WRITE);
    new Thread(new Refresh(Folder.WRITE, reply)).start();
  }

  private Email composeMail(Email replyTo) {

    if (subjectField.getText().isEmpty()) {
      model.infoNotification("Please add a subject to the email");
      return null;
    }
    if (toField.getText().isEmpty()) {
      model.infoNotification("Please add at least one recipient");
      return null;
    }

    List<String> dests = getDestsList(toField.getText());

    for (String dest : dests) {
      if (!User.validateEmail(dest)) {
        model.infoNotification(dest + " is not a valid email address");
        return null;
      }
      if (dest.equals(model.getUser().getEmail())) {
        model.infoNotification("You cannot send an email to yourself");
        return null;
      }
    }

    Email newMail = new Email();
    newMail.setSubject(subjectField.getText().trim());
    newMail.setDests(dests);
    newMail.setSender(model.getUser().getEmail());
    newMail.setContent(emailAreaW.getText());
    newMail.setDate(new Date());
    newMail.setReply(replyTo != null ? replyTo.getReply() : null);
    return newMail;
  }

  private List<String> getDestsList(String string) {
    return Arrays.stream(string.split("[ ,;]+"))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(java.util.stream.Collectors.toList());
  }

  private String getDestsString(List<String> list) {
    if (list == null || list.isEmpty()) return "";
    return String.join(", ", list);
  }

  public void initModel(ClientModel model) {
    if (this.model != null)
      throw new IllegalStateException("Model can only be initialized once");

    this.model = model;

    Thread update = new Thread(new Update());
    update.setDaemon(true);
    update.setName("ClientUpdate");
    update.start();

    readBox.setVisible(true);
    writeBox.setVisible(false);
    serverUnreachable(false);

    lblUsername.setText(model.getUser().getEmail());
    lstEmails.itemsProperty().bind(model.emailListProperty());

    lstEmails.getSelectionModel().selectedItemProperty()
        .addListener((obs, oldVal, newVal) -> model.setCurrentMail(newVal));

    model.currentMailProperty().addListener((obs, oldMail, newMail) -> {
      if (newMail == null) {
        lstEmails.getSelectionModel().clearSelection();
        updateDetailView(null);
      } else {
        lstEmails.getSelectionModel().select(newMail);
        updateDetailView(newMail);
      }
    });

    lstEmails.setCellFactory(lv -> new ListCell<>() {
      @Override
      public void updateItem(Email email, boolean empty) {
        super.updateItem(email, empty);
        if (empty || email == null) {
          setText(null);
        } else {
          String header = (email.getBelonging() != Folder.SENT)
              ? email.getSender()
              : getDestsString(email.getDests());
          setText(header + "\t\t\t" + dateForm.format(email.getDate())
              + "\n" + email.getSubject());
        }
      }
    });

    new Thread(new Refresh(Folder.INBOX, null)).start();
    lstEmails.getSelectionModel().clearSelection();
    updateDetailView(null);
  }

  private void updateDetailView(Email email) {
    if (email != null) {
      if (!readMode) {
        fromLabelW.setText(model.getUser().getEmail());
        subjectField.setText(email.getSubject());
        subjectField.setEditable(false);
        if (email.getReply() != null) {
          toField.setText(getDestsString(email.getDests()));
          toField.setEditable(false);
        } else {
          toField.clear();
          toField.setEditable(true);
        }
        emailAreaW.setText(email.getContent());
      } else {
        setReadButtons(false);
        subjectLabel.setText(email.getSubject());
        fromLabelR.setText(email.getSender());
        dateLabel.setText(dateForm.format(email.getDate()));
        emailAreaR.setText(email.getContent());
        toLabelR.setText(getDestsString(email.getDests()));
      }
    } else {
      if (!readMode) {
        subjectField.setEditable(true);
        toField.setEditable(true);
        fromLabelW.setText(model.getUser().getEmail());
        subjectField.clear();
        toField.clear();
        emailAreaW.clear();
      } else {
        setReadButtons(true);
        subjectLabel.setText("");
        fromLabelR.setText("");
        dateLabel.setText("");
        emailAreaR.clear();
        toLabelR.setText("");
      }
    }
  }

  private void setReadButtons(boolean disabled) {
    replyButton.setDisable(disabled);
    replyAllButton.setDisable(disabled);
    forwardButton.setDisable(disabled);
    deleteButton.setDisable(disabled);
  }

  
  private void newEmailNotification(Email email) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.INFORMATION);
      alert.setTitle("[" + model.getUser().getEmail() + "] New email");
      alert.setHeaderText("New mail from: " + email.getSender()
          + "\nSubject: " + email.getSubject());
      ButtonType showButton   = new ButtonType("Show",   ButtonBar.ButtonData.OK_DONE);
      ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
      alert.getButtonTypes().setAll(showButton, cancelButton);
      
      alert.showAndWait().ifPresent(resp -> {
        if (resp == showButton)
          new Thread(new Refresh(Folder.INBOX, email)).start();
      });
    });
  }

  private void serverUnreachable(boolean value) {
    Platform.runLater(() -> {
      errorImg.setVisible(value);
      errorLabel.setVisible(value);
    });
  }

  // ===== Inner classes =====

  private class Reconnect implements Runnable {
    @Override
    public void run() {
      while (true) {
        try {
          model.setConnection(new Connection());
          serverUnreachable(false);
          System.out.println("Connection re-established");
          return;
        } catch (IOException e) {
          System.out.println("Reconnect failed, retrying in 3s...");
          try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        }
      }
    }
  }

  private class Update implements Runnable {
    @Override
    public void run() {
      while (true) {
        try {
          Thread.sleep(3000);
          LinkedList<Email> news = model.getNews();
          serverUnreachable(false);
          for (Email toNotify : news)
            new Thread(new NotifyThread(toNotify)).start();
        } catch (InterruptedException | IOException | ClassNotFoundException e) {
          serverUnreachable(true);
          new Thread(new Reconnect()).start();
          // FIX: attende prima di riprovare — evita spam di thread Reconnect
          // se il server resta offline a lungo
          try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
        }
      }
    }
  }

  private class Refresh implements Runnable {
    private final Folder folder;
    private final Email  email;

    public Refresh(Folder folder, Email email) {
      this.folder = folder;
      this.email  = email;
    }

    @Override
    public void run() {
      try {
        LinkedList<Email> mailbox = model.mailboxRequest(folder);
        Platform.runLater(() -> {
          model.setCurrentFolder(folder);
          model.setCurrentEmailList(mailbox);
          lblFolder.setText(folder.toString());
          readView(folder != Folder.WRITE);
          model.setCurrentMail(email);
        });
      } catch (IOException | ClassNotFoundException e) {
        serverUnreachable(true);
      }
    }
  }

  private class SendThread implements Runnable {
    private final Email toSend;
    public SendThread(Email toSend) { this.toSend = toSend; }

    @Override
    public void run() {
      try {
        model.sendEmail(toSend);
        new Thread(new Refresh(Folder.SENT, null)).start();
      } catch (IOException | ClassNotFoundException e) {
        serverUnreachable(true);
      }
    }
  }

  private class DeleteThread implements Runnable {
    private final Email toDelete;
    public DeleteThread(Email toDelete) { this.toDelete = toDelete; }

    @Override
    public void run() {
      try {
        model.deleteEmail(toDelete);
        new Thread(new Refresh(Folder.BIN, null)).start();
      } catch (IOException | ClassNotFoundException e) {
        serverUnreachable(true);
      }
    }
  }

  private class NotifyThread implements Runnable {
    private final Email toNotify;
    public NotifyThread(Email toNotify) { this.toNotify = toNotify; }

    @Override
    public void run() {
      try {
        model.notifyReadEmail(toNotify);
        newEmailNotification(toNotify);
      } catch (IOException | ClassNotFoundException e) {
        serverUnreachable(true);
      }
    }
  }

  private class LogOutThread implements Runnable {
    @Override
    public void run() { model.logoutRequest(); }
  }
}