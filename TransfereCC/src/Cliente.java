import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Source-Code para a classe Cliente.
 * @author Diogo Araújo, Diogo Nogueira
 * @version 1.5
 */

class Cliente {

    /** Variável predefinida como o cabeçalho do pacote. **/
    private static final int headerPDU = 4;

    /** Variável predefinida como o tamanho total do pacote. **/
    private static final int tamanhoPDU = 1000;  // numSeq = 4Bytes e dados=1000Bytes - 1004Bytes total Pacote.

    /** Variável predefinida como o tamanho total da "janela deslizante". **/
    private static final int windowSize = 10;

    /** Variável para guardar o nº da janela corrente. **/
    private int base;

    /** Variável para guardar o próximo nº de sequência. **/
    private int proxNumSeq;

    /** O caminho/diretoria do ficheiro que será enviado. **/
    private final String caminho;     // diretoria + nome do arquivo.

    /** Variável para guardar a lista de pacotes da janela deslizante. **/
    private final List<byte[]> listaPacotes;

    /** Temporizador a ser usado em caso de falha de resposta. **/
    private Timer temporizador; // temporizador para a espera de resposta.

    private final Semaphore acesso;

    /** Booleano final para confirmar se a transferência foi concluída. **/
    private boolean transferenciaCompleta;

    /**
     * Construtor parametrizado para a criação do Cliente que irá fazer PUT no servidor.
     * @param portaDestino Porta UDP que o servidor tem aberta para receber o(s) pacote(s) UDP (7777).
     * @param portaEntrada Porta ACK usada para receber os pacotes ACK vindos do servidor durante a transferência (9999).
     * @param localDisco A diretoria do ficheiro a ser enviado.
     * @param enderecoIP Endereço IP do servidor a enviar o ficheiro.
     */
    Cliente(int portaDestino, int portaEntrada, String localDisco, String enderecoIP) {
        base = 0;
        proxNumSeq = 0;
        this.caminho = localDisco;
        listaPacotes = new ArrayList<>(windowSize);
        transferenciaCompleta = false;
        DatagramSocket socketSaida, socketEntrada;
        acesso = new Semaphore(1);
        System.out.println("Cliente: porta de destino: " + portaDestino + ", porta de entrada: " + portaEntrada + ", localDisco: " + localDisco);
 
        try {
            // Criação dos Sockets de Entrada e Saída.
            // Cliente vai receber pacotes ACK do Servidor pelo socketEntrada.
            // Cliente vai enviar pacotes UPD para o Servidor pelo socketSaida.
            socketEntrada = new DatagramSocket(portaEntrada);
            socketSaida = new DatagramSocket();

            // Criação das threads que depois vão processar os dados enviados e recebidos.
            ThreadEntrada threadACK = new ThreadEntrada(socketEntrada);
            ThreadSaida threadPacotes = new ThreadSaida(socketSaida, portaDestino, enderecoIP);

            // Inicialização das threads.
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
    class Temporizador extends TimerTask {

        /**
         * Método necessário para correr, vindo da interface Runnable do Java.
         * Com este método conseguimos dar acesso às outras threads em termos de janela deslizante.
         */
        public void run() {
            try {
                acesso.acquire();
                System.out.println("Cliente: Tempo em espera demasiado.");
                proxNumSeq = base;  // Faz reset ao número de sequência.
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
    private void modificarTemporizador(boolean novoTimer) {
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
    class ThreadSaida extends Thread {
 
        private final DatagramSocket socketSaida;
        private final int portaDestino;
        private InetAddress enderecoIP;

        /**
         * Construtor parametrizado para a criação do objeto de ThreadSaída.
         * @param socketSaida O socket para onde irá sair os pacotes para o Servidor.
         * @param portaDestino A porta do servidor para onde serão enviados os pacotes.
         * @param enderecoIP O endereço IP do servidor para onde serão enviados os pacotes.
         * @throws UnknownHostException Uma exceção para quando não se conhece o endereço IP fornecido.
         */
        ThreadSaida(DatagramSocket socketSaida, int portaDestino, String enderecoIP) throws UnknownHostException {
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
                    while (!transferenciaCompleta) {    // Envia pacotes ao Servidor caso a janela nao esteja cheia.

                        if (proxNumSeq < base + (windowSize * tamanhoPDU)) {

                            // Bloqueia o acesso para a thread/pacote.
                            acesso.acquire();

                            // Se o pacote é o primeiro da janela deslizante - incia temporizador.
                            if (base == proxNumSeq) {
                                modificarTemporizador(true);
                            }

                            byte[] enviaDados;
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

                            // Envia o pacote de dados para o Servidor consoante o número de ACK que recebeu do mesmo.
                            socketSaida.send(new DatagramPacket(enviaDados, enviaDados.length, enderecoIP, portaDestino));
                            System.out.println("Cliente: Numero de sequencia enviado " + proxNumSeq);

                            // Atualiza número de sequência caso não esteja no fim.
                            if (!ultimoNumSeq) {
                                proxNumSeq += tamanhoPDU;
                            }

                            // Liberta acesso para as outras threads/pacotes.
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
    class ThreadEntrada extends Thread {
 
        private final DatagramSocket socketEntrada;

        /**
         * Construtor parametrizado para a criação do Thread de Entrada.
         * @param socketEntrada O socket para onde irá entrar os pacotes ACK do servidor.
         */
        ThreadEntrada(DatagramSocket socketEntrada) {
            this.socketEntrada = socketEntrada;
        }

        /**
         * Método (override) necessário para a Thread correr e conterá o código a ser corrido pela mesma.
         */
        public void run() {
            try {

                // Preparação do pacote ACK vindo do Servidor.
                byte[] recebeDados = new byte[headerPDU];

                DatagramPacket pacoteRecebido = new DatagramPacket(recebeDados, recebeDados.length);

                try {
                    while (!transferenciaCompleta) {

                        socketEntrada.receive(pacoteRecebido);

                        // Obtém o número ACK do pacote que recebeu do Servidor.
                        int numACK = PacoteUDP.getACK(recebeDados);
                        System.out.println("Cliente: ACK RECEBIDO " + numACK);

                        // Se o pacote ACK recebido for duplicado.
                        if (base == numACK + tamanhoPDU) {
                            System.out.println("ACK duplicado.");

                            // Bloqueia o acesso para a thread/pacote.
                            acesso.acquire();
                            modificarTemporizador(false); // Cancelar o temporizador.
                            proxNumSeq = base;
                            // Liberta acesso para outras threads/pacotes.
                            acesso.release();
                        }

                        // Caso o pacote ACK tenha número -5.
                        // Último pacote - transferência completa.
                        if (numACK == -5) {
                            transferenciaCompleta = true;
                        }

                        // Caso seja um pacote normal.
                        else {
                            // Avanço do pacote base da janela para o numACK + os 1000 de tamanho.
                            base = numACK + tamanhoPDU;

                            // Bloqueia o acesso para a thread/pacote.
                            acesso.acquire();
                            if (base == proxNumSeq) {
                                modificarTemporizador(false);
                            } else {
                                modificarTemporizador(true);
                            }
                            // Liberta acesso para outras threads/pacotes.
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