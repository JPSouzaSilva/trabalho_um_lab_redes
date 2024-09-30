import java.io.*;
import java.net.*;

public class UDPClient {
  private static final String SERVER_ADDRESS = "127.0.0.1"; // Endereço do servidor
  private static final int SERVER_PORT = 12345; // Porta do servidor

  public static void main(String[] args) {
    try (DatagramSocket socket = new DatagramSocket(); // Cria socket UDP
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

      InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS); // Obtem endeço do servidor

      System.out.print("Para entrar no servidor UDP, digite seu username para começar: ");
      String username = console.readLine(); // Lê o username que o usuário mandou
      sendMessage(socket, username, serverAddress); // Envia o nome do usuário ao servidor

      // Thread para receber mensagens do servidor.
      new Thread(() -> {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (true) {
          try {
            socket.receive(packet); // Recebe mensagem do servidor
            String serverMessage = new String(packet.getData(), 0, packet.getLength());

            // Caso for um arquivo, chama metodo para recebe-lo
            if (serverMessage.startsWith("FILE")) {
              receiveFile(socket, packet);
            } else {
              System.out.println(serverMessage);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }).start();
      // Lê as mensagens do console e as envia para o servidor
      String clientMessage;
      while ((clientMessage = console.readLine()) != null) {
        sendMessage(socket, clientMessage, serverAddress);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  // Envia uma mensagem ao servidor    
  private static void sendMessage(DatagramSocket socket, String message, InetAddress serverAddress) throws IOException {
    byte[] buffer = message.getBytes(); // Converte em bytes
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, SERVER_PORT); // Cria pacote
    socket.send(packet); // Envia pacote
  }

  private static void receiveFile(DatagramSocket socket, DatagramPacket packet) throws IOException {
    // Extrai informações do arquivo
    String[] fileInfo = new String(packet.getData(), 0, packet.getLength()).split(" ");
    String fileName = fileInfo[1];

    // Informa ao usuário que o arquivo está sendo recebido
    System.out.println("Recebendo arquivo: " + fileName);

    // Cria um arquivo local para salvar o arquivo recebido
    File receivedFile = new File(System.currentTimeMillis() + "_" + fileName);
    try (FileOutputStream fileOutputStream = new FileOutputStream(receivedFile)) {
      byte[] buffer = new byte[1024];
      int bytesRead;

      // Loop para receber o conteúdo do arquivo em pacotes
      while (true) {
        DatagramPacket filePacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(filePacket);
        bytesRead = filePacket.getLength();

        // Se o último pacote for menor que 1024 bytes, escreve e termina porque é o ultimo
        if (bytesRead < 1024) {
          fileOutputStream.write(buffer, 0, bytesRead);
          break;
        }

        // Escreve os dados do pacote no arquivo
        fileOutputStream.write(buffer, 0, bytesRead);
      }
    }
     // Informa ao usuário que o arquivo foi recebido com sucesso
    System.out.println("Arquivo " + fileName + " recebido com sucesso.");
  }
}