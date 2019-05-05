import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Source-Code para a classe Cliente.
 * @author Diogo Araújo, Diogo Nogueira
 * @version 1.5
 */

public class Cliente {

    /** Variável predefinida como o cabeçalho do pacote. **/
    static final int headerPDU = 4;

    /** Variável predefinida como o tamanho total do pacote. **/
    static final int tamanhoPDU = 1000;  // (numSeq:4, dados=1000) Bytes : 1004 Bytes total

    /** Variável predefinida como o tamanho total da "janela deslizante". **/
    static final int windowSize = 10;

    /** Variável para guardar o nº da janela corrente. **/
    int base;
    /** Variável para guardar o próximo nº de sequência. **/
    int proxNumSeq;
    /** O caminho/diretoria do ficheiro que será enviado. **/
    String caminho;     //diretoria + nome do arquivo.
    /** Variável para guardar a lista de pacotes da janela deslizante. **/
    List<byte[]> listaPacotes;
    /** Temporizador a ser usado em caso de falha de resposta. **/
    Timer temporizador; // temporizador para a espera de resposta.

    Semaphore acesso;
    /** Booleano final para confirmar se a transferência ocorreu. **/
    boolean transferenciaCompleta;

    /**
     * Construtor parametrizado para a criação do Cliente que irá fazer PUT no servidor.
     * @param portaDestino Porta UDP que o servidor tem aberta para receber o(s) pacote(s) UDP (7777)
     * @param portaEntrada Porta ACK usada para receber os pacotes ACK vindos do servidor durante a transferência (9999)
     * @param localDisco A diretoria do ficheiro a ser enviado.
     * @param enderecoIP Endereço IP do servidor a enviar o ficheiro.
     */
    public Cliente(int portaDestino, int portaEntrada, String localDisco, String enderecoIP) {
        base = 0;
        proxNumSeq = 0;
        this.caminho = localDisco;
        listaPacotes = new ArrayList<>(windowSize);
        transferenciaCompleta = false;
        DatagramSocket socketSaida, socketEntrada;
        acesso = new Semaphore(1);
        System.out.println("Cliente: porta de destino: " + portaDestino + ", porta de entrada: " + portaEntrada + ", localDisco: " + localDisco);
 
        try {
            // criando sockets
            socketSaida = new DatagramSocket();
            socketEntrada = new DatagramSocket(portaEntrada);
 
            // criando threads para processar os dados enviados e recebidos.
            ThreadEntrada threadACK = new ThreadEntrada(socketEntrada);
            ThreadSaida threadPacotes = new ThreadSaida(socketSaida, portaDestino, enderecoIP);
            threadACK.start();
            threadPacotes.start();
 
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Classe interna para a criação do Temporizador e a sua execução.
     */
    public class Temporizador extends TimerTask {

        /**
         * Método necessário para correr, vindo da interface Runnable do Java.
         */
        public void run() {
            try {
                acesso.acquire();
                System.out.println("Cliente: Tempo em espera demasiado.");
                proxNumSeq = base;  //reseta numero de sequencia
                acesso.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Método para controlar o Temporizador.
     * @param novoTimer um booleano, que caso seja TRUE, reinicia-se o temporizador.
     */
    public void modificarTemporizador(boolean novoTimer) {
        if (temporizador != null) {
            temporizador.cancel();
        }
        if (novoTimer) {
            temporizador = new Timer();
            temporizador.schedule(new Temporizador(), 1000); // criação de novo timer com 1000 milisegundos, ou seja, 1 segundo.
        }
    }

    /**
     * A classe interna para enviar informações para o servidor em modo Thread.
     */
    public class ThreadSaida extends Thread {
 
        private DatagramSocket socketSaida;
        private int portaDestino;
        private InetAddress enderecoIP;

        /**
         * Construtor parametrizado para a criação do objeto de ThreadSaída.
         * @param socketSaida O socket para onde irá sair os pacotes para o servidor.
         * @param portaDestino A porta do servidor para onde serão enviados os pacotes.
         * @param enderecoIP O endereço IP do servidor para onde serão enviados os pacotes.
         * @throws UnknownHostException Uma exceção para quando não se conhece o endereço IP fornecido.
         */
        public ThreadSaida(DatagramSocket socketSaida, int portaDestino, String enderecoIP) throws UnknownHostException {
            this.socketSaida = socketSaida;
            this.portaDestino = portaDestino;
            this.enderecoIP = InetAddress.getByName(enderecoIP);
        }

        /**
         * Método (override) necessário para a Thread correr e conterá o código a ser corrido pela mesma.
         */
        public void run() {
            try {

                try (FileInputStream fis = new FileInputStream(new File(caminho))) {
                    while (!transferenciaCompleta) {    //envia pacotes se a janela nao estiver cheia
                        if (proxNumSeq < base + (windowSize * tamanhoPDU)) {
                            acesso.acquire();
                            if (base == proxNumSeq) {   //se for primeiro pacote da janela, inicia temporizador
                                modificarTemporizador(true);
                            }
                            byte[] enviaDados = new byte[headerPDU];
                            boolean ultimoNumSeq = false;

                            if (proxNumSeq < listaPacotes.size()) {
                                enviaDados = listaPacotes.get(proxNumSeq);
                            } else {
                                byte[] dataBuffer = new byte[tamanhoPDU];
                                int tamanhoDados = fis.read(dataBuffer, 0, tamanhoPDU);
                                if (tamanhoDados == -1) {   //sem dados para enviar, envia pacote vazio
                                    ultimoNumSeq = true;
                                    enviaDados = PacoteUDP.gerarPacoteDados(proxNumSeq, new byte[0]);
                                } else {    //ainda ha dados para enviar
                                    byte[] dataBytes = Arrays.copyOfRange(dataBuffer, 0, tamanhoDados);
                                    enviaDados = PacoteUDP.gerarPacoteDados(proxNumSeq, dataBytes);
                                }
                                listaPacotes.add(enviaDados);
                            }
                            //enviando pacotes
                            socketSaida.send(new DatagramPacket(enviaDados, enviaDados.length, enderecoIP, portaDestino));
                            System.out.println("Cliente: Numero de sequencia enviado " + proxNumSeq);

                            //atualiza numero de sequencia se nao estiver no fim
                            if (!ultimoNumSeq) {
                                proxNumSeq += tamanhoPDU;
                            }
                            acesso.release();
                        }
                        sleep(5);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    modificarTemporizador(false);
                    socketSaida.close();
                    System.out.println("Cliente: Socket de saida fechado!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    /**
     * A classe interna para receber os pacotes de ACK enviados pelo servidor em modo Thread.
     */
    public class ThreadEntrada extends Thread {
 
        private DatagramSocket socketEntrada;

        /**
         * Construtor parametrizado para a criação do Thread de Entrada.
         * @param socketEntrada O socket para onde irá entrar os pacotes ACK do servidor.
         */
        public ThreadEntrada(DatagramSocket socketEntrada) {
            this.socketEntrada = socketEntrada;
        }

        /**
         * Método (override) necessário para a Thread correr e conterá o código a ser corrido pela mesma.
         */
        public void run() {
            try {

                byte[] recebeDados = new byte[headerPDU];  //pacote ACK sem dados

                DatagramPacket pacoteRecebido = new DatagramPacket(recebeDados, recebeDados.length);

                try {
                    while (!transferenciaCompleta) {

                        socketEntrada.receive(pacoteRecebido);

                        int numACK = PacoteUDP.getACK(recebeDados);
                        System.out.println("Cliente: ACK RECEBIDO " + numACK);

                        //se for ACK duplicado
                        if (base == numACK + tamanhoPDU) {
                            System.out.println("ACK duplicado.");
                            acesso.acquire();
                            modificarTemporizador(false); // cancelar o temporizador
                            proxNumSeq = base;
                            acesso.release();
                        } else if (numACK == -5) {
                            transferenciaCompleta = true;
                        } //ACK normal
                        else {
                            // avançar a janela para o numACK mais o tamanho.
                            base = numACK + tamanhoPDU;
                            acesso.acquire();
                            if (base == proxNumSeq) {
                                modificarTemporizador(false);
                            } else {
                                modificarTemporizador(true);
                            }
                            acesso.release();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    socketEntrada.close();
                    System.out.println("Cliente: Socket de entrada fechado!");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
}