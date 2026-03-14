package it.unito.servermail.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Email implements Serializable {

  private static final long serialVersionUID = 123L;

  private int id;
  private String sender;
  private List<String> dests;
  private String subject;
  private String content;
  private Folder folder;
  private boolean read;
  private Email reply;
  private Date date;

  public Email() {
  }

  public Email(int id, String sender, List<String> dests, String subject, Date date) {
    this.id = id;
    this.sender = sender;
    this.dests = dests;
    this.subject = subject;
    this.date = date;
    this.folder = Folder.WRITE;
    this.read = false;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String s) {
    this.sender = s;
  }

  public List<String> getDests() {
    return dests;
  }

  public void setDests(List<String> d) {
    this.dests = d;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String s) {
    this.subject = s;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String c) {
    this.content = c;
  }

  public Folder getBelonging() {
    return folder;
  }

  public void setBelonging(Folder f) {
    this.folder = f;
  }

  public boolean isRead() {
    return read;
  }

  public void setRead(boolean read) {
    this.read = read;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = (date != null) ? new Date(date.getTime()) : null;
  }

  public Email getReply() {
    return reply;
  }

  public void setReply(Email reply) {
    this.reply = reply;
  }

  @Override
  public String toString() {
    String s = (subject != null) ? subject : "(no subject)";
    String f = (sender != null) ? sender : "(unknown)";
    return s + " - " + f;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Email))
      return false;
    return this.id == ((Email) o).id;
  }

  @Override
  public int hashCode() {
    return Integer.hashCode(id);
  }
}