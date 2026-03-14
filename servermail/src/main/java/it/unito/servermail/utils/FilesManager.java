package it.unito.servermail.utils;

import it.unito.servermail.model.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;

public class FilesManager {

  public static final String FILES_PATH      = "data/";
  public static final String SERVER_PATH     = "data/server";
  public static final String USERS_FILE      = "data/server/users.dat";
  public static final String ID_COUNTER_FILE = "data/server/id_counter.dat";
  public static final String INBOX_FILENAME  = "/inbox.dat";
  public static final String SENT_FILENAME   = "/sent.dat";
  public static final String BIN_FILENAME    = "/bin.dat";

  public static void createFiles(User user) throws IOException {

    if (user == null)
      throw new IllegalArgumentException("[FilesManager.createFiles]: user cannot be null");

    Files.createDirectories(Paths.get(FILES_PATH + user.getEmail()));

    File inbox = new File(FILES_PATH + user.getEmail() + INBOX_FILENAME);
    File sent  = new File(FILES_PATH + user.getEmail() + SENT_FILENAME);
    File bin   = new File(FILES_PATH + user.getEmail() + BIN_FILENAME);

    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(inbox))) {
      out.writeObject(new LinkedList<Email>());
    }
    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(sent))) {
      out.writeObject(new LinkedList<Email>());
    }
    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(bin))) {
      out.writeObject(new LinkedList<Email>());
    }
  }

  public static void serverFiles() throws IOException {

    Files.createDirectories(Paths.get(FILES_PATH));
    Files.createDirectories(Paths.get(SERVER_PATH));

    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
      out.writeObject(new ArrayList<User>());
    }
    try (DataOutputStream out = new DataOutputStream(new FileOutputStream(ID_COUNTER_FILE))) {
      out.writeInt(0);
    }
  }

  public static boolean addUserToFile(User user) throws IllegalArgumentException, IOException, ClassNotFoundException {

    if (user == null)
      throw new IllegalArgumentException("[FilesManager.addUserToFile]: user cannot be null");

    ArrayList<User> accounts = getUserList();

    if (accounts.contains(user))
      return false;

    accounts.add(user);

    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
      out.writeObject(accounts);
    }

    createFiles(user);
    return true;
  }

  public static ArrayList<User> getUserList() {

    ArrayList<User> userList = new ArrayList<>();

    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(USERS_FILE))) {
      userList = (ArrayList<User>) in.readObject();
    } catch (IOException | ClassNotFoundException e) {
      System.out.println(e.getMessage());
    }

    return userList;
  }

  private static File getFile(String user, Folder folder) {

    if (user == null || folder == null)
      throw new IllegalArgumentException("[FilesManager.getFile]: arguments cannot be null");

    String path = FILES_PATH + user;
    switch (folder) {
      case INBOX -> path += INBOX_FILENAME;
      case BIN   -> path += BIN_FILENAME;
      case SENT  -> path += SENT_FILENAME;
    }

    return new File(path);
  }

  public static LinkedList<Email> getMailBox(String user, Folder folder) {

    if (user == null || folder == null)
      throw new IllegalArgumentException("[FilesManager.getMailBox]: arguments cannot be null");

    File mailBox = getFile(user, folder);
    LinkedList<Email> emailList = new LinkedList<>();

    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(mailBox))) {
      emailList = (LinkedList<Email>) in.readObject();
    } catch (IOException | ClassNotFoundException e) {
      e.printStackTrace();
    }

    return emailList;
  }

  private static void updateMailBox(File mailBox, LinkedList<Email> updated) {

    if (mailBox == null || updated == null)
      throw new IllegalArgumentException("[FilesManager.updateMailBox]: arguments cannot be null");

    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(mailBox))) {
      out.writeObject(updated);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void rmMailFromMailbox(Email toRemove, String email) {

    if (toRemove == null || email == null)
      throw new IllegalArgumentException("[FilesManager.rmMailFromMailbox]: arguments cannot be null");

    File fileSource = getFile(email, toRemove.getBelonging());

    synchronized (fileSource) {
      LinkedList<Email> mailList = getMailBox(email, toRemove.getBelonging());
      int idToRemove = toRemove.getId();
      boolean removed = false;
      for (int i = 0; i < mailList.size() && !removed; i++) {
        if (mailList.get(i).getId() == idToRemove) {
          mailList.remove(i);
          removed = true;
        }
      }
      if (removed)
        updateMailBox(fileSource, mailList);
    }
  }

  public static void moveMail(Email toMove, Folder destination, String user) throws Exception {

    if (toMove == null || destination == null || user == null)
      throw new IllegalArgumentException("[FilesManager.moveMail]: arguments cannot be null");

    rmMailFromMailbox(toMove, user);
    insMailToMailbox(toMove, destination, user, false);
  }

  public static void insMailToMailbox(Email toInsert, Folder folder, String user, boolean inc) {

    if (toInsert == null || folder == null || user == null)
      throw new IllegalArgumentException("[FilesManager.insMailToMailbox]: arguments cannot be null");

    boolean reply = (toInsert.getReply() != null);
    LinkedList<Email> mailBox;

    switch (folder) {

      case BIN -> {
        toInsert.setBelonging(Folder.BIN);
        toInsert.setRead(true);
        File file = getFile(user, Folder.BIN);

        synchronized (file) {
          mailBox = getMailBox(user, Folder.BIN);
          mailBox.addFirst(toInsert);
          updateMailBox(file, mailBox);
        }
      }

      case INBOX -> {
        toInsert.setBelonging(Folder.INBOX);
        toInsert.setRead(toInsert.getSender().equals(user));

        File fileInbox = getFile(user, Folder.INBOX);

        if (reply) {
          toInsert.setId(toInsert.getReply().getId());

          synchronized (fileInbox) {
            mailBox = getMailBox(user, Folder.INBOX);
            mailBox.removeIf(e -> e.getId() == toInsert.getId());
            mailBox.addFirst(toInsert);
            updateMailBox(fileInbox, mailBox);
          }
        } else {
          toInsert.setId(getID());

          synchronized (fileInbox) {
            mailBox = getMailBox(user, Folder.INBOX);
            mailBox.addFirst(toInsert);
            updateMailBox(fileInbox, mailBox);
          }

          if (inc)
            incrementID();
        }
      }

      case SENT -> {
        String sender = toInsert.getSender();
        if (sender == null)
          throw new IllegalArgumentException("[FilesManager.insMailToMailbox]: sender cannot be null");

        File file = getFile(sender, Folder.SENT);

        synchronized (file) {
          mailBox = getMailBox(sender, Folder.SENT);
          toInsert.setBelonging(Folder.SENT);
          toInsert.setRead(true);
          if (reply)
            mailBox.removeIf(e -> e.getId() == toInsert.getId());
          mailBox.addFirst(toInsert);
          updateMailBox(file, mailBox);
        }
      }
    }
  }

  public static boolean setMailRead(User user, Email toFlag) {

    if (toFlag == null || toFlag.getBelonging() != Folder.INBOX || user == null)
      throw new IllegalArgumentException("[FilesManager.setMailRead]: invalid arguments");

    File inbox = getFile(user.getEmail(), Folder.INBOX);
    boolean found = false;

    synchronized (inbox) {
      LinkedList<Email> mailBox = getMailBox(user.getEmail(), Folder.INBOX);
      for (Email email : mailBox) {
        if (email.getId() == toFlag.getId()) {
          email.setRead(true);
          found = true;
          updateMailBox(inbox, mailBox);
          break;
        }
      }
    }

    return found;
  }

  private static final File ID_COUNTER = new File(ID_COUNTER_FILE);

  private static int getID() {
    synchronized (ID_COUNTER) {
      try (DataInputStream in = new DataInputStream(new FileInputStream(ID_COUNTER_FILE))) {
        return in.readInt();
      } catch (IOException e) {
        System.out.println(e.getMessage());
        return 0;
      }
    }
  }

  private static void incrementID() {
    synchronized (ID_COUNTER) {
      try (DataInputStream in = new DataInputStream(new FileInputStream(ID_COUNTER_FILE))) {
        int id = in.readInt();
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(ID_COUNTER_FILE))) {
          out.writeInt(id + 1);
        }
      } catch (IOException e) {
        System.out.println(e.getMessage());
      }
    }
  }
}