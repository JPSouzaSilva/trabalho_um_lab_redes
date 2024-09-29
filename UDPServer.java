import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPServer {
  private static final int SERVER_PORT = 12346;

  public static void main(String[] args) {
    try (DatagramSocket socket = new DatagramSocket(SERVER_PORT)) {
      System.out.println("Servidor UDP iniciado na porta " + SERVER_PORT);

      byte[] buffer = new byte[1024];
      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

      while (true) {
        socket.receive(packet);
        String message = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Mensagem recebida de " + packet.getAddress() + ": " + message);

        String response = "Mensagem recebida: " + message;
        DatagramPacket responsePacket = new DatagramPacket(
            response.getBytes(),
            response.length(),
            packet.getAddress(),
            packet.getPort());
        socket.send(responsePacket);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
