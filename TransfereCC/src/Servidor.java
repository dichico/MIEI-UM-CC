/**
 * @author Diogo Araújo, Diogo Nogueira
 * @version 1.0
 */

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;
  
public class Servidor { 
    static final int headerPDU = 4; // Inteiro são 4.
    static final int tamanhoPDU = 1000 + headerPDU;
    static final int portaUDP = 7777;
    static final int portaEstado = 9999;

    /**
     * Construtor parametrizado para a criação do Servidor.
     * @param portaEntrada
     * @param portaDestino
     * @param diretoria
     */
    public Servidor(int portaEntrada, int portaDestino, String diretoria) {
        DatagramSocket socketEntrada, socketSaida; // inicializar sockets
        //AgenteUDP udp = new AgenteUDP(portaEntrada, portaDestino);

        System.out.println("Servidor com porta UDP: " + portaEntrada);
 
        int ultimoNumSeq = -1;
        int proxNumSeq = 0;  //proximo numero de sequencia
        boolean transferenciaCompleta = false;  //flag caso a transferencia nao for completa
 
        // Criação dos sockets
        try {
            socketEntrada = new DatagramSocket(portaEntrada);
            socketSaida = new DatagramSocket();
            System.out.println("Servidor Conectado...");
            try {
                byte[] recebeDados = new byte[tamanhoPDU];
                DatagramPacket recebePacote = new DatagramPacket(recebeDados, recebeDados.length);
 
                FileOutputStream fileStream = null;
 
                while (!transferenciaCompleta) {
                    int i = 0;
                    socketEntrada.receive(recebePacote);
                    InetAddress enderecoIP = recebePacote.getAddress();
 
                    int numSeq = ByteBuffer.wrap(Arrays.copyOfRange(recebeDados, 0, headerPDU)).getInt();
                    System.out.println("Servidor: Numero de sequencia recebido " + numSeq);
 
                    //se o pacote for recebido em ordem
                    if (numSeq == proxNumSeq) {
                        //se for ultimo pacote (sem dados), enviar ack de encerramento
                        if (recebePacote.getLength() == headerPDU) {
                            byte[] pacoteAck = gerarPacote(-2);     //ack de encerramento
                            socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                            transferenciaCompleta = true;
                            System.out.println("Servidor: Todos pacotes foram recebidos! Arquivo criado!");
                        } else {
                            proxNumSeq = numSeq + tamanhoPDU - headerPDU;  //atualiza proximo numero de sequencia
                            byte[] pacoteAck = gerarPacote(proxNumSeq);
                                socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                                System.out.println("Servidor: Ack enviado " + proxNumSeq);
                          }
 
                        //se for o primeiro pacote da transferencia 
                        if (numSeq == 0 && ultimoNumSeq == -1) {
                            //cria file
                            File file = new File(diretoria);
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                            fileStream = new FileOutputStream(file);
                        }
                        //escreve dados no arquivo
                        fileStream.write(recebeDados, headerPDU, recebePacote.getLength() - headerPDU);
 
                        ultimoNumSeq = numSeq; //atualiza o ultimo numero de sequencia enviado
                    } else {    //se pacote estiver fora de ordem, mandar duplicado
                        byte[] pacoteAck = gerarPacote(ultimoNumSeq);
                        socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, enderecoIP, portaDestino));
                        System.out.println("Servidor: Ack duplicado enviado " + ultimoNumSeq);
                    }
 
                }
                if (fileStream != null) {
                    fileStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            } finally {
                socketEntrada.close();
                socketSaida.close();
                System.out.println("Servidor: Socket de entrada fechado!");
                System.out.println("Servidor: Socket de saida fechado!");
            }
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
    }
    //fim do construtor
 
    //gerar pacote de ACK
    public byte[] gerarPacote(int numAck) {
        byte[] numAckBytes = ByteBuffer.allocate(headerPDU).putInt(numAck).array();
        ByteBuffer bufferPacote = ByteBuffer.allocate(headerPDU);
        bufferPacote.put(numAckBytes);
        return bufferPacote.array();
    }
 
    public static void main(String[] args) {
        Scanner teclado = new Scanner(System.in);
        System.out.println("Bem vindo ao Servidor");
        System.out.print("Digite a diretoria do arquivo a ser criado. (Ex: C:/Users/cocos/): ");
        String diretorio = teclado.nextLine();
        System.out.print("Digite o nome do ficheiro para onde irá ser guardado o recebido: (Ex: teste2.txt): ");
        String nome = teclado.nextLine();
 
        Servidor servidor = new Servidor(portaUDP, portaEstado, diretorio + nome);
    }
}