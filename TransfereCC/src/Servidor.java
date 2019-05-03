import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

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
     * @param portaEntrada Porta UDP usada para receber o(s) pacote(s) UDP. (7777)
     * @param portaDestino Porta ACK usada para enviar os pacotes ACK durante a transferência. (9999)
     * @param localDisco A diretoria para onde o ficheiro a ser recebido irá ser colocado.
     */
    public Servidor(int portaEntrada, int portaDestino, String localDisco) {

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
                // Preparação do pacote de dados vindo do Cliente.
                byte[] recebeDados = new byte[tamanhoPDU];
                DatagramPacket recebePacote = new DatagramPacket(recebeDados, recebeDados.length);
 
                FileOutputStream fileStream = null;

                // ciclo a decorrer durante a transferência.
                while (!transferCompleta) {

                    // Receber o pacote vindo do Cliente.
                    socketEntrada.receive(recebePacote);
                    InetAddress ipAddress = recebePacote.getAddress();

                    // receber o nº de sequência através da leitura do header do pacote.
                    int seqACK = ByteBuffer.wrap(recebeDados, 0, headerPDU).getInt();
                    System.out.println("Servidor: Numero de sequencia recebido " + seqACK);
 
                    //se o pacote for recebido em ordem
                    if (seqACK == proxNumACK) {
                        //se for ultimo pacote (sem dados), enviar ack para encerrar o processo de transferencia
                        if (recebePacote.getLength() == headerPDU) {

                            byte[] pacoteFinal = gerarPacoteACK(-2);     //ack de encerramento
                            socketSaida.send(new DatagramPacket(pacoteFinal, pacoteFinal.length, ipAddress, portaDestino));
                            transferCompleta = true;
                            System.out.println("Servidor: Todos pacotes foram recebidos! Ficheiro recebido e criado!");
                        } else {
                            // atualiza proximo numero de sequencia à base do tamanho do pacote.
                            proxNumACK = seqACK + tamanhoPDU - headerPDU;
                            // envia o ACK de que recebeu aquele nº de sequência.
                            byte[] pacoteACK = gerarPacoteACK(proxNumACK);
                            socketSaida.send(new DatagramPacket(pacoteACK, pacoteACK.length, ipAddress, portaDestino));

                            System.out.println("Servidor: ACK enviado " + proxNumACK);
                        }
 
                        //se for o primeiro pacote da transferencia 
                        if (seqACK == 0 && ultimoNumSeq == -1) {
                            //cria ficheiro
                            fileStream = new FileOutputStream(new File(localDisco));
                        }
                        //escreve dados no ficheiro
                        fileStream.write(recebeDados, headerPDU, recebePacote.getLength() - headerPDU);
 
                        ultimoNumSeq = seqACK; //atualiza o ultimo numero de sequencia enviado/recebido

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
            System.exit(-1);
        }
    }
}