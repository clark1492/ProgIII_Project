package it.unito.servermail.model;

public enum Folder {

  INBOX("Inbox"),
  SENT("Sent"),
  BIN("Bin"),
  WRITE("Write");

  private final String label;

  Folder(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return label;
  }
}