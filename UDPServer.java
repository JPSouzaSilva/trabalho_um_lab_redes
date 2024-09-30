import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class UDPServer {
  private static final int SERVER_PORT = 12345;
  private static Map<String, InetSocketAddress> users = new HashMap<>();
  private static DatagramSocket socket;

  public static void main(String[] args) {
    try {
      socket = new DatagramSocket(SERVER_PORT);
      System.out.println("Servidor UDP iniciado na porta " + SERVER_PORT);

      while (true) {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        new ClientHandler(packet).start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static class ClientHandler extends Thread {
    private DatagramPacket packet;
    private String username;
    private InetSocketAddress clientAddress;

    public ClientHandler(DatagramPacket packet) {
      this.packet = packet;
      this.clientAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
    }

    public void run() {
      try {
        String message = new String(packet.getData(), 0, packet.getLength());

        if (users.containsValue(clientAddress)) {
          for (Map.Entry<String, InetSocketAddress> entry : users.entrySet()) {
            if (entry.getValue().equals(clientAddress)) {
              username = entry.getKey();
            }
          }
          processClientMessage(message);
        } else {
          username = message;
          synchronized (users) {
            users.put(username, clientAddress);
          }
          sendMessage("Registrado como: " + username, clientAddress);
          sendMessage("Bem vindo ao servidor UDP", clientAddress);
          sendMessage("Para ver todos os comandos do servidor, digite: /HELP", clientAddress);
          System.out.println(username + " conectado.");
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    private void processClientMessage(String message) throws IOException {
      boolean messageCommand = message.startsWith("/MSG");
      boolean fileCommand = message.startsWith("/FILE");
      boolean helpCommand = message.equals("/HELP");
      boolean listUsersCommand = message.equals("/LIST");

      if (messageCommand) {
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

      if (helpCommand) {
        allCommands();
      }

      if (listUsersCommand) {
        listAllUsers();
      }
    }

    private void sendMessage(String userToSend, String msg) throws IOException {
      InetSocketAddress target = users.get(userToSend);

      boolean userOnline = target != null;

      if (userOnline) {
        sendMessage(username + ": " + msg, target);
        sendMessage("Mensagem enviada com sucesso", clientAddress);
      } else {
        sendMessage("O usuário " + userToSend + " não está online.", clientAddress);
      }
    }

    private void sendFile(String userToSend, String fileName) throws IOException {
      InetSocketAddress target = users.get(userToSend);

      boolean userIsOnline = target != null;

      if (userIsOnline) {
        File originalFile = new File(fileName);

        if (!originalFile.exists()) {
          sendMessage("O arquivo " + fileName + " não foi encontrado.", clientAddress);
          return;
        }

        sendMessage("FILE " + originalFile.getName(), target);

        try (FileInputStream fileInputStream = new FileInputStream(originalFile)) {
          byte[] fileBuffer = new byte[1024];
          int bytesRead;
          while ((bytesRead = fileInputStream.read(fileBuffer)) != -1) {
            DatagramPacket filePacket = new DatagramPacket(fileBuffer, bytesRead, target.getAddress(),target.getPort());
            socket.send(filePacket);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }

        sendMessage("Arquivo " + fileName + " enviado com sucesso para " + userToSend, clientAddress);
      } else {
        sendMessage("O usuário " + userToSend + " não está online.", clientAddress);
      }
    }

    private void allCommands() throws IOException {
      sendMessage("Comandos disponíveis no servidor:", clientAddress);
      sendMessage("/HELP - Listar todos os comandos.", clientAddress);
      sendMessage("/LIST - Listar todos os usuários conectados.", clientAddress);
      sendMessage("/MSG <username> <mensagem> - Enviar mensagem a um usuário.", clientAddress);
      sendMessage("/FILE <username> <caminho do arquivo> - Enviar arquivo a um usuário.", clientAddress);
    }

    private void listAllUsers() throws IOException {
      synchronized (users) {
        StringBuilder userList = new StringBuilder("Usuários conectados:");
        for (String user : users.keySet()) {
          userList.append("\n- ").append(user);
        }
        sendMessage(userList.toString(), clientAddress);
      }
    }

    private void sendMessage(String message, InetSocketAddress target) throws IOException {
      byte[] buffer = message.getBytes();
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, target.getAddress(), target.getPort());
      socket.send(packet);
    }
  }
}