import java.io.*;
import java.net.*;

public class AgenteUDP {

        public static void inicializarConexao(String nservidor) {

            DatagramSocket socket;

            InetAddress IPAddress = null;
            try {
                IPAddress = InetAddress.getByName(nservidor);
            } catch (UnknownHostException e) {
                System.out.println("O servidor em questão não é conhecido. Por favor tente de novo.");

            }


            try {
                socket = new DatagramSocket(7777, IPAddress);

                byte[] sendData;
                byte[] receiveData = new byte[1024];

                BufferedReader inFromUser = new BufferedReader(new InputStreamReader(
                        System.in));

                System.out.println("Digite o texto a ser enviado ao servidor: ");
                String sentence = inFromUser.readLine();
                sendData = sentence.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData,
                        sendData.length, IPAddress, 7777);

                System.out
                        .println("Enviando pacote UDP para " + nservidor + ":" + 7777);
                socket.send(sendPacket);

            } catch (SocketException e) {
                System.out.println("O servidor em questão não está a correr. Por favor tente de novo.");
            }
            catch (IOException e) {
            }



            /* DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);

            socket.receive(receivePacket);
            System.out.println("Pacote UDP recebido...");

            String modifiedSentence = new String(receivePacket.getData());

            System.out.println("Texto recebido do servidor:" + modifiedSentence);
            clientSocket.close();
            System.out.println("Socket cliente fechado!");*/


        }

        public static void aceitaConexao() {

            DatagramSocket socket;

            try {
                socket = new DatagramSocket(7777);

                byte[] receiveData = new byte[1024];
                byte[] sendData;

                while (true) {

                    DatagramPacket receivePacket = new DatagramPacket(receiveData,
                            receiveData.length);

                    socket.receive(receivePacket);

                    System.out.print("Datagrama UDP recebido...");

                    String sentence = new String(receivePacket.getData());
                    System.out.println(sentence);
                }
            } catch (Exception e) {
            }




                /*InetAddress IPAddress = receivePacket.getAddress();

                int port = receivePacket.getPort();

                String capitalizedSentence = sentence.toUpperCase();

                sendData = capitalizedSentence.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(sendData,
                        sendData.length, IPAddress, port);

                System.out.print("Enviando " + capitalizedSentence + "...");

                socket.send(sendPacket);
                System.out.println("OK\n");*/


        }

        public static void main(String[] args) throws Exception {

            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(
                    System.in));

            DatagramSocket clientSocket = new DatagramSocket();

            String servidor = "localhost";
            int porta = 7777;

            InetAddress IPAddress = InetAddress.getByName(servidor);

            byte[] sendData;
            byte[] receiveData = new byte[1024];

            System.out.println("Digite o texto a ser enviado ao servidor: ");
            String sentence = inFromUser.readLine();
            sendData = sentence.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData,
                    sendData.length, IPAddress, porta);

            System.out
                    .println("Enviando pacote UDP para " + servidor + ":" + porta);
            clientSocket.send(sendPacket);

            DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);

            clientSocket.receive(receivePacket);
            System.out.println("Pacote UDP recebido...");

            String modifiedSentence = new String(receivePacket.getData());

            System.out.println("Texto recebido do servidor:" + modifiedSentence);
            clientSocket.close();
            System.out.println("Socket cliente fechado!");
        }
    }
