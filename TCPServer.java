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

        username = in.readLine();
        synchronized (users) {
          users.put(username, client);
        }
        out.println("Registrado como: " + username);
        System.out.println(username + " conectado.");

        out.println();
        out.println("Bem vindo ao servidor TCP");
        out.println("Para ver todos os comandos do servidor, digite: /HELP");

        String clientMessage;

        while ((clientMessage = in.readLine()) != null) {
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
      boolean helpCommand = message.equals("/HELP");
      boolean listUsersCommand = message.equals("/LIST");

      if (messageComand) {
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
          String userToSend = parts[1];
          String msg = parts[2];
          sendMessage(userToSend, msg);
          out.println("Mensagem enviada com sucesso");
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

      if (helpCommand) {
        allCommands();
      }

      if (listUsersCommand) {
        listAllUsers();
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
          File originalFile = new File(fileName);

          boolean originalFileExist = originalFile.exists();

          if (!originalFileExist) {
            out.println("O arquivo " + fileName + " não foi encontrado.");
            return;
          }

          String newFileName = System.currentTimeMillis() + "_" + originalFile.getName();
          File newFile = new File(newFileName);

          try (BufferedReader fileReader = new BufferedReader(new FileReader(originalFile));
              BufferedWriter fileWriter = new BufferedWriter(new FileWriter(newFile))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
              fileWriter.write(line);
              fileWriter.newLine();
            }
          }

          try (BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(newFile));
              BufferedOutputStream targetOut = new BufferedOutputStream(target.getOutputStream())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileInput.read(buffer)) != -1) {
              targetOut.write(buffer, 0, bytesRead);
            }
            targetOut.flush();
          }

          out.println("Arquivo " + originalFile.getName() + " enviado para " + userToSend + ".");

        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        out.println("O usuário " + userToSend + " não está online.");
      }
    }

    private void allCommands() {
      out.println();
      out.println("Comandos disponíveis no servidor:");
      out.println("/HELP - Listar todos os comandos.");
      out.println("/LIST - Listar todos os usuários conectados.");
      out.println("/MSG <username> <mensagem> - Enviar mensagem a um usuário.");
      out.println("/FILE <username> <caminho do arquivo> - Enviar arquivo a um usuário.");
    }

    private void listAllUsers() {
      out.println();
      synchronized (users) {
        boolean haveUsers = !users.isEmpty();
        if (haveUsers) {
          out.println("Usuários conectados:");
          for (String user : users.keySet()) {
            out.println("- " + user);
          }
        } else {
          out.println("Não tem usuários conectados.");
        }
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