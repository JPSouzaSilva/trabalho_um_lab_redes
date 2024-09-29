import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPClient {
  private static final String SERVER_ADDRESS = "127.0.0.1";
  private static final int SERVER_PORT = 12346;

  public static void main(String[] args) {
    try (DatagramSocket socket = new DatagramSocket()) {
      InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);

      while (true) {
        System.out.print("Digite a mensagem: ");
        byte[] messageBytes = System.console().readLine().getBytes();

        DatagramPacket packet = new DatagramPacket(messageBytes, messageBytes.length, serverAddress, SERVER_PORT);
        socket.send(packet);

        byte[] buffer = new byte[1024];
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(responsePacket);
        String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
        System.out.println("Resposta do servidor: " + response);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
