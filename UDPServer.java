import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class UDPServer {
  private static final int SERVER_PORT = 12345; // Porta do servidor
  // Mapeia o nome de usuário para o endereço de rede (IP + Porta).
  private static Map<String, InetSocketAddress> users = new HashMap<>();
  private static DatagramSocket socket;

  public static void main(String[] args) {
    try {
      socket = new DatagramSocket(SERVER_PORT); // Inicia o server
      System.out.println("Servidor UDP iniciado na porta " + SERVER_PORT);

      while (true) {
        byte[] buffer = new byte[1024]; // Buffer para receber os dados dos clientes
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet); // Recebe o pacote enviado pelo cliente
        new ClientHandler(packet).start(); // Cria uma nova thread para processar a mensagem do cliente.
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Classe que gerencia a comunicação com um cliente
  private static class ClientHandler extends Thread {
    private DatagramPacket packet; // Pacote recebido do cliente.
    private String username; // Nome do usuário do cliente.
    private InetSocketAddress clientAddress; // Endereço do cliente

    public ClientHandler(DatagramPacket packet) {
      this.packet = packet;
      this.clientAddress = new InetSocketAddress(packet.getAddress(), packet.getPort()); // Obtem o endereço do cliente
    }

    public void run() {
      try {
        // Transforma os dados do pacote recebido em String
        String message = new String(packet.getData(), 0, packet.getLength());

        // Verifica se o cliente já está registrado
        if (users.containsValue(clientAddress)) {
          for (Map.Entry<String, InetSocketAddress> entry : users.entrySet()) {
            if (entry.getValue().equals(clientAddress)) {
              username = entry.getKey();
            }
          }
          processClientMessage(message); // Processa a mensagem recebida
        } else {
          username = message; // Caso o cliente não esteja registrado, registra ele
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
      // Processa os comandos que o cliente mandar
      boolean messageCommand = message.startsWith("/MSG");
      boolean fileCommand = message.startsWith("/FILE");
      boolean helpCommand = message.equals("/HELP");
      boolean listUsersCommand = message.equals("/LIST");

      // Envio de mensagem para outro usuário
      if (messageCommand) {
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
          String userToSend = parts[1];
          String msg = parts[2];
          sendMessage(userToSend, msg);
        }
      }

      // Envio de arquivo para outro usuário
      if (fileCommand) {
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
          String userToSend = parts[1];
          String fileName = parts[2];
          sendFile(userToSend, fileName);
        }
      }

      // Lista dos os comandos que tem
      if (helpCommand) {
        allCommands();
      }

      // Lista todos os clientes online
      if (listUsersCommand) {
        listAllUsers();
      }
    }

    private void sendMessage(String userToSend, String msg) throws IOException {
      InetSocketAddress target = users.get(userToSend); // Pega o endereço do destinatario

      boolean userOnline = target != null; // Vê se ele está online

      if (userOnline) {
        sendMessage(username + ": " + msg, target); // Envia a mensagem para o destinatario
        sendMessage("Mensagem enviada com sucesso", clientAddress); // Confirma que a mensagem foi enviada
      } else {
        // Caso o usuário não esteja online, avisa para o remetente
        sendMessage("O usuário " + userToSend + " não está online.", clientAddress);
      }
    }

    private void sendFile(String userToSend, String fileName) throws IOException {
      InetSocketAddress target = users.get(userToSend); // Pega o endereço do destinatario

      boolean userIsOnline = target != null; // Vê se ele está online

      if (userIsOnline) {
        File originalFile = new File(fileName); // Carrega o arquivo

        boolean originalFileExist = originalFile.exists();

        if (!originalFileExist) {
          // Verifica se encontrou o arquivo, caso não encontre envia a mensagem que não encontrou o arquivo
          sendMessage("O arquivo " + fileName + " não foi encontrado.", clientAddress);
          return;
        }

        // Notifica o destinatário que o arquivo está sendo enviado
        sendMessage("FILE " + originalFile.getName(), target);

        // Envia o arquivo em pacotes de 1024 bytes.
        try (FileInputStream fileInputStream = new FileInputStream(originalFile)) {
          byte[] fileBuffer = new byte[1024];
          int bytesRead;
          while ((bytesRead = fileInputStream.read(fileBuffer)) != -1) {
            DatagramPacket filePacket = new DatagramPacket(fileBuffer, bytesRead, target.getAddress(),target.getPort());
            socket.send(filePacket); // Envia os pacotes
          }
        } catch (IOException e) {
          e.printStackTrace();
        }

        // Confirma ao remetente que o arquivo foi enviado.
        sendMessage("Arquivo " + fileName + " enviado com sucesso para " + userToSend, clientAddress);
      } else {
        // Caso o usuário não esteja online, avisa para o remetente
        sendMessage("O usuário " + userToSend + " não está online.", clientAddress);
      }
    }

    private void allCommands() throws IOException {
      // Envia ao cliente uma lista com todos os comandos disponíveis
      sendMessage("Comandos disponíveis no servidor:", clientAddress);
      sendMessage("/HELP - Listar todos os comandos.", clientAddress);
      sendMessage("/LIST - Listar todos os usuários conectados.", clientAddress);
      sendMessage("/MSG <username> <mensagem> - Enviar mensagem a um usuário.", clientAddress);
      sendMessage("/FILE <username> <caminho do arquivo> - Enviar arquivo a um usuário.", clientAddress);
    }

    private void listAllUsers() throws IOException {
      // Lista todos os usuários conectados no servidor
      synchronized (users) {
        StringBuilder userList = new StringBuilder("Usuários conectados:");
        for (String user : users.keySet()) {
          userList.append("\n- ").append(user);
        }
        sendMessage(userList.toString(), clientAddress);
      }
    }

    private void sendMessage(String message, InetSocketAddress target) throws IOException {
      // Manda mensagem para um endereço especifico
      byte[] buffer = message.getBytes();
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length, target.getAddress(), target.getPort());
      socket.send(packet);
    }
  }
}