import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class TCPServer {
  private static final int SERVER_PORT = 12345; // Porta do servidor
  // Mapeia usuários conectados e seus respectivos sockets
  private static Map<String, Socket> users = new HashMap<>();

  public static void main(String[] args) {
    try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) { // Inicia servidor TCP
      System.out.println("Servidor TCP iniciado na porta " + SERVER_PORT);

      while (true) {
        Socket clientSocket = serverSocket.accept(); // Aceita uma nova conexão de cliente
        new ClientHandler(clientSocket).start(); // Cria uma nova thread para lidar com o cliente
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Classe que gerencia a comunicação com um cliente
  private static class ClientHandler extends Thread {
    private Socket client; // Socket do cliente
    private String username; // Nome de usuário do cliente
    private BufferedReader in; // Stream de entrada para ler mensagens do cliente
    private PrintWriter out; // Stream de saída para enviar mensagens ao cliente

    public ClientHandler(Socket socket) {
      this.client = socket;
    }

    public void run() {
      try {
        // Inicializa os streams de entrada e saída
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintWriter(client.getOutputStream(), true);

        // Lê o nome de usuário do cliente e adiciona o usuário ao mapa
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

        // Loop para processar mensagens do cliente
        while ((clientMessage = in.readLine()) != null) {
          processClientMessage(clientMessage);
        }
      } catch (IOException e) {
        e.printStackTrace();
      } finally {
        disconnectUser();
      }
    }

    // Processa mensagens recebidas do cliente
    private void processClientMessage(String message) {
      boolean messageComand = message.startsWith("/MSG");
      boolean fileCommand = message.startsWith("/FILE");
      boolean helpCommand = message.equals("/HELP");
      boolean listUsersCommand = message.equals("/LIST");

      // Envio de mensagem para outro usuário
      if (messageComand) {
        String[] parts = message.split(" ", 3);
        if (parts.length == 3) {
          String userToSend = parts[1];
          String msg = parts[2];
          sendMessage(userToSend, msg);
          out.println("Mensagem enviada com sucesso");
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

    // Envia uma mensagem para um usuário específico
    private void sendMessage(String userToSend, String msg) {
      Socket target = users.get(userToSend); // Busca o socket do destinatário

      boolean userIsOnline = target != null; // Verifica se o usuário está online

      if (userIsOnline) {
        try {
          PrintWriter targetOut = new PrintWriter(target.getOutputStream(), true);
          targetOut.println(username + ": " + msg); // Envia a mensagem ao destinatário
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        // Caso o usuário não esteja online, avisa para o remetente
        out.println("O usuário " + userToSend + " não está online.");
      }
    }

    private void sendFile(String userToSend, String fileName) {
      Socket target = users.get(userToSend); // Busca o socket do destinatário

      boolean userIsOnline = target != null; // Verifica se o usuário está online

      if (userIsOnline) {
        try {
          File originalFile = new File(fileName); // Carrega o arquivo

          boolean originalFileExist = originalFile.exists(); // Verifica se o arquivo existe

          if (!originalFileExist) { 
            out.println("O arquivo " + fileName + " não foi encontrado.");
            return;
          }

          // Avisa o destinatario que o remetente mandou um arquivo
          PrintWriter targetOutUser = new PrintWriter(target.getOutputStream(), true);
          targetOutUser.println(username + " enviou o arquivo: " + originalFile.getName());

          // Cria novo arquio
          String newFileName = System.currentTimeMillis() + "_" + originalFile.getName();
          File newFile = new File(newFileName);

          // Lê o conteúdo do arquivo original e grava no novo arquivo
          try (BufferedReader fileReader = new BufferedReader(new FileReader(originalFile));
              BufferedWriter fileWriter = new BufferedWriter(new FileWriter(newFile))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
              fileWriter.write(line);
              fileWriter.newLine();
            }
          }

          // Avisa o remetente que enviou
          out.println("Arquivo " + originalFile.getName() + " enviado para " + userToSend + ".");

        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        // Caso o usuário não esteja online, avisa para o remetente
        out.println("O usuário " + userToSend + " não está online.");
      }
    }

    // Lista todos os comandos disponíveis no servidor
    private void allCommands() {
      out.println("Comandos disponíveis no servidor:");
      out.println("/HELP - Listar todos os comandos.");
      out.println("/LIST - Listar todos os usuários conectados.");
      out.println("/MSG <username> <mensagem> - Enviar mensagem a um usuário.");
      out.println("/FILE <username> <caminho do arquivo> - Enviar arquivo a um usuário.");
    }

    // Lista todos os usuários conectados ao servidor
    private void listAllUsers() {
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

    // Disconecta o usuário e remove do mapa
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