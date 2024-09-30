import java.io.*;
import java.net.Socket;

public class TCPClient {
  private static final String SERVER_ADDRESS = "127.0.0.1"; // Endereço do servidor
  private static final int SERVER_PORT = 12345; // Porta

  public static void main(String[] args) {
    try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

      // Solicita o nome de usuário do cliente
      System.out.print("Para entrar no servidor TCP, digite seu username para começar: ");
      String username = console.readLine();
      out.println(username);

      // Thread para receber mensagens do servidor
      new Thread(() -> {
        try {
          String serverMessage;
          while ((serverMessage = in.readLine()) != null) {
            System.out.println(serverMessage); // Exibe mensagens recebidas do servidor
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }).start();

      // Loop para ler comandos e enviá-las ao servidor
      String clientMessage;
      while ((clientMessage = console.readLine()) != null) {
        out.println(clientMessage);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}