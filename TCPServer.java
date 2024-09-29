import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class TCPServer {
  private static final int SERVER_PORT = 12345;
  private static Map<String, Socket> users = new HashMap<>();

  public static void main(String[] args) {
    try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
      System.out.println("Servidor TCP iniciado na porta " + SERVER_PORT);

      while (true) {
        Socket clientSocket = serverSocket.accept();
        new ClientHandler(clientSocket).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static class ClientHandler extends Thread {
    private Socket client;
    private String username;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
      this.client = socket;
    }

    public void run() {
      try {
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true);

        out.println("Digite seu username: ");
        username = in.readLine();
        synchronized (users) {
          users.put(username, client);
        }
        out.println("Registrado como: " + username);
        System.out.println(username + " conectado.");

        String clientMessage;

        boolean haveMessage = (clientMessage = in.readLine()) != null;

        while (haveMessage) {
          processClientMessage(clientMessage);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        disconnectUser();
      }
    }

    private void processClientMessage(String message) {
      boolean messageComand = message.startsWith("/MSG");
      boolean fileCommand = message.startsWith("/FILE");
      
      if (messageComand) {
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
          String userToSend = parts[1];
          String msg = parts[2];
          sendMessage(userToSend, msg);
        }
      }
      
      if (fileCommand) {
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
          String userToSend = parts[1];
          String fileName = parts[2];
          sendFile(userToSend, fileName);
        }
      }
    }

    private void sendMessage(String userToSend, String msg) {
      Socket target = users.get(userToSend);

      boolean userIsOnline = target != null;

      if (userIsOnline) {
        try {
          PrintWriter targetOut = new PrintWriter(target.getOutputStream(), true);
          targetOut.println(username + ": " + msg);
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        out.println("O usuário " + userToSend + " não está online.");
      }
    }

    private void sendFile(String userToSend, String fileName) {
      Socket target = users.get(userToSend);

      boolean userIsOnline = target != null;

      if (userIsOnline) {
        try {
          PrintWriter targetOut = new PrintWriter(target.getOutputStream(), true);
          targetOut.println(username + " enviou um arquivo: " + fileName);

          BufferedReader fileReader = new BufferedReader(new FileReader(fileName));
          String line;
          while ((line = fileReader.readLine()) != null) {
            targetOut.println(line);
          }
          fileReader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        out.println("O usuário " + userToSend + " não está online.");
      }
    }

    private void disconnectUser() {
      boolean userExist = username != null;

      if (userExist) {
        synchronized (users) {
          users.remove(username);
        }
        System.out.println(username + " desconectado.");
        try {
          client.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}