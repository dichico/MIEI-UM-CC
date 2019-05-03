import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Código-fonte para a Classe Servidor, que tem metade do código, concretamente da parte de receber os dados (GET)
 * @author Diogo Araújo, Diogo Nogueira
 * @version 1.5
 */

public class Servidor extends PacoteUDP {

    static final int headerPDU = 4; // Inteiro são 4.
    static final int tamanhoPDU = 1000 + headerPDU;

    /**
     * Construtor parametrizado para a criação do Servidor (GET)
     * @param portaEntrada Porta UDP usada para receber o(s) pacote(s) UDP.
     * @param portaDestino Porta ACK usada para enviar os pacotes ACK durante a transferência.
     * @param diretoria A diretoria para onde o ficheiro a ser recebido irá ser colocado.
     */
    public Servidor(int portaEntrada, int portaDestino, String diretoria) {

        DatagramSocket socketEntrada, socketSaida;

        System.out.println("Servidor com porta UDP: " + portaEntrada);
 
        int ultimoNumSeq = -1;
        int proxNumACK = 0;  
        boolean transferCompleta = false; 

        try {
            // Criação dos Sockets de Entrada e Saída.
            socketEntrada = new DatagramSocket(portaEntrada);
            socketSaida = new DatagramSocket();

            System.out.println("Conexão Estabelecida.");

            try {
                byte[] recebeDados = new byte[tamanhoPDU];
                DatagramPacket recebePacote = new DatagramPacket(recebeDados, recebeDados.length);
 
                FileOutputStream fileStream = null;
 
                while (!transferCompleta) {

                    socketEntrada.receive(recebePacote);
                    InetAddress ipAddress = recebePacote.getAddress();
 
                    int seqACK = ByteBuffer.wrap(Arrays.copyOfRange(recebeDados, 0, headerPDU)).getInt();
                    System.out.println("Servidor: Numero de sequencia recebido " + seqACK);
 
                    //se o pacote for recebido em ordem
                    if (seqACK == proxNumACK) {
                        //se for ultimo pacote (sem dados), enviar ack para encerrar o processo de transferencia
                        if (recebePacote.getLength() == headerPDU) {

                            byte[] pacoteAck = gerarPacoteACK(-2);     //ack de encerramento
                            socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, ipAddress, portaDestino));
                            transferCompleta = true;
                            System.out.println("Servidor: Todos pacotes foram recebidos! Arquivo criado!");

                        } else {
                            // atualiza proximo numero de sequencia à base do tamanho do pacote.
                            proxNumACK = seqACK + tamanhoPDU - headerPDU;
                            byte[] pacoteACK = gerarPacoteACK(proxNumACK);
                            socketSaida.send(new DatagramPacket(pacoteACK, pacoteACK.length, ipAddress, portaDestino));

                            System.out.println("Servidor: ACK enviado " + proxNumACK);

                        }
 
                        //se for o primeiro pacote da transferencia 
                        if (seqACK == 0 && ultimoNumSeq == -1) {
                            //cria file
                            File file = new File(diretoria);
                            if (!file.exists()) {
                                file.createNewFile();
                            }
                            fileStream = new FileOutputStream(file);
                        }
                        //escreve dados no ficheiro
                        fileStream.write(recebeDados, headerPDU, recebePacote.getLength() - headerPDU);
 
                        ultimoNumSeq = seqACK; //atualiza o ultimo numero de sequencia enviado
                    } else {    //se pacote estiver fora de ordem, mandar duplicado
                        byte[] pacoteAck = gerarPacoteACK(ultimoNumSeq);
                        socketSaida.send(new DatagramPacket(pacoteAck, pacoteAck.length, ipAddress, portaDestino));
                        System.out.println("Servidor: Ack duplicado enviado " + ultimoNumSeq);
                    }
 
                }
                fileStream.close();
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
}