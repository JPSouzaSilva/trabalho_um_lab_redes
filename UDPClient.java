import java.io.*;
import java.net.*;

public class UDPClient {
  private static final String SERVER_ADDRESS = "127.0.0.1";
  private static final int SERVER_PORT = 12345;

  public static void main(String[] args) {
    try (DatagramSocket socket = new DatagramSocket();
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

      InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);

      System.out.print("Para entrar no servidor UDP, digite seu username para começar: ");
      String username = console.readLine();
      sendMessage(socket, username, serverAddress);

      new Thread(() -> {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (true) {
          try {
            socket.receive(packet);
            String serverMessage = new String(packet.getData(), 0, packet.getLength());
            System.out.println(serverMessage);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }).start();

      String clientMessage;
      while ((clientMessage = console.readLine()) != null) {
        sendMessage(socket, clientMessage, serverAddress);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void sendMessage(DatagramSocket socket, String message, InetAddress serverAddress) throws IOException {
    byte[] buffer = message.getBytes();
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, SERVER_PORT);
    socket.send(packet);
  }
}