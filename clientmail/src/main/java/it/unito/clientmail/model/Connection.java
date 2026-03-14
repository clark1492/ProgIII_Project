package it.unito.clientmail.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Connection {

  private static final String HOST = "127.0.0.1";
  private static final int    PORT = 8189;

  private final Socket server;
  private final ObjectOutputStream outputStream;
  private final ObjectInputStream  inputStream;

  public Connection() throws IOException {
    this(HOST, PORT);
  }

  public Connection(String host, int port) throws IOException {
    server = new Socket(host, port);
    outputStream = new ObjectOutputStream(server.getOutputStream());
    outputStream.flush();
    inputStream = new ObjectInputStream(server.getInputStream());
  }

  public void write(Object obj) throws IOException {
    outputStream.writeObject(obj);
    outputStream.flush();
  }

  public Object read() throws IOException, ClassNotFoundException {
    return inputStream.readObject();
  }

  
  public boolean isConnected() {
    return server != null && !server.isClosed() && server.isConnected();
  }

  public void closeConnection() {
    try {
      if (outputStream != null) outputStream.close();
      if (inputStream  != null) inputStream.close();
      if (server != null)       server.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}