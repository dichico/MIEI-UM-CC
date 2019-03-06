import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class AgenteUDP {

    private DatagramSocket socket;
    private InetAddress address;

    private byte[] buffer;

    public AgenteUDP() {
        socket = new DatagramSocket();
        address = InetAddress.getByName("localhost");

    }

    public sendData(String string) {
        buffer = string.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, 7777);
        socket.send(packet);
    }

    public String receiveData() {
        DatagramPacket packetR = new DatagramPacket(buffer, buffer.length);
        socket.receive(packetR);
        String recebida = new String(packetR.getData(),0,packetR.getLength());
        return recebida;
    }


}
