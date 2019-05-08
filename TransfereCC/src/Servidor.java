import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

/**
 * Código-fonte para a Classe Servidor, que tem metade do código, concretamente da parte de receber os dados (GET)
 * @author Diogo Araújo, Diogo Nogueira
 * @version 1.5
 */
class Servidor extends PacoteUDP {

    /** Variável predefinida como o cabeçalho do pacote. **/
    private static final int headerPDU = 4; // Inteiro são 4.

    /** Variável predefinida como o tamanho total do pacote. **/
    private static final int tamanhoPDU = 1000 + headerPDU;

    /**
     * Construtor parametrizado para a criação do Servidor (GET)
     * @param portaEntrada Porta UDP usada para receber o(s) pacote(s) UDP (7777).
     * @param portaDestino Porta ACK usada para enviar os pacotes ACK durante a transferência (9999).
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
            // Servidor vai receber pacotes UPD do Cliente pelo socketEntrada.
            // Servidor vai mandar pacotes ACK do Cliente pelo socketSaida.
            socketEntrada = new DatagramSocket(portaEntrada);
            socketSaida = new DatagramSocket();

            System.out.println("Conexão Estabelecida.");

            try {
                // Preparação do pacote de dados vindo do Cliente.
                byte[] recebeDados = new byte[tamanhoPDU];
                DatagramPacket recebePacote = new DatagramPacket(recebeDados, recebeDados.length);
 
                FileOutputStream fileStream = null;

                // Ciclo que é executado até transferência ser efetuada.
                while (!transferCompleta) {

                    // Recebe o pacote vindo do Cliente.
                    socketEntrada.receive(recebePacote);
                    InetAddress ipAddress = recebePacote.getAddress();

                    // Retira o número de sequência do pacote recebido.
                    // Obtém esse número retirando apenas a parte do cabeçalho do pacote.
                    int seqACK = ByteBuffer.wrap(recebeDados, 0, headerPDU).getInt();
                    System.out.println("Servidor: Numero de sequencia recebido " + seqACK);
 
                    // Se pacote foi recebido de forma ordenada.
                    if (seqACK == proxNumACK) {

                        // Se pacote é o primerio de toda a sequência.
                        if (seqACK == 0 && ultimoNumSeq == -1) {
                            // Criar o ficheiro em si.
                            fileStream = new FileOutputStream(new File(localDisco));
                        }

                        // Se pacote é o último (não há mais dados) - enviar ACK -5 que define fim de transferência.
                        if (recebePacote.getLength() == headerPDU) {

                            byte[] pacoteFinal = gerarPacoteACK(-5);     //ack de encerramento
                            socketSaida.send(new DatagramPacket(pacoteFinal, pacoteFinal.length, ipAddress, portaDestino));
                            System.out.println("Servidor: ACK final enviado " + -5);
                            transferCompleta = true;
                            System.out.println("Servidor: Todos pacotes foram recebidos! Ficheiro recebido e criado!");
                        }
                        // Se pacote nem é o último nem o primeiro.
                        else {
                            // Atualiza o próximo número de sequência à base do tamanho do pacote.
                            proxNumACK = seqACK + (tamanhoPDU - headerPDU);
                            // Envia o ACK de que recebeu aquele número de sequência.
                            byte[] pacoteACK = gerarPacoteACK(proxNumACK);
                            socketSaida.send(new DatagramPacket(pacoteACK, pacoteACK.length, ipAddress, portaDestino));

                            System.out.println("Servidor: ACK enviado " + proxNumACK);
                        }

                        // Escreve os dados no ficheiro.
                        fileStream.write(recebeDados, headerPDU, recebePacote.getLength() - headerPDU);
                        ultimoNumSeq = seqACK; // Atualiza o último número de sequência enviado/recebido.
                    }

                    // Se pacote foi recebido de forma desordenada - torna a mandar (duplicado).
                    else {
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