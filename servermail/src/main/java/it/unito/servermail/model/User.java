package it.unito.servermail.model;

import java.io.Serializable;
import java.util.regex.Pattern;

public class User implements Serializable {

  private static final long serialVersionUID = 456L;

  private static final Pattern EMAIL_PATTERN = Pattern.compile(
      "^[\\w.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}$",
      Pattern.CASE_INSENSITIVE);

  private String email;
  private String password;

  public User(String email, String password) {
    this.email = email;
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public static boolean validateEmail(String email) {
    if (email == null)
      return false;
    return EMAIL_PATTERN.matcher(email).matches();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof User))
      return false;
    return this.email != null && this.email.equalsIgnoreCase(((User) o).email);
  }

  @Override
  public int hashCode() {
    return email != null ? email.toLowerCase().hashCode() : 0;
  }

  @Override
  public String toString() {
    return "User{email='" + email + "'}";
  }
}