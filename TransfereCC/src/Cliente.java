import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Source-Code para a classe Cliente.
 * @author Diogo Araújo, Diogo Nogueira
 * @author Java Tutorial UDP Threads
 * @version 1.5
 */

class Cliente {

    /** Variável predefinida como o cabeçalho do pacote. **/
    /** Variável predefinida como o tamanho total do pacote. **/
    private static final int headerPDU = 4;
    private static final int tamanhoPDU = 1000;  // numSeq = 4Bytes e dados=1000Bytes -> 1004Bytes total Pacote.

    /** Variável para guardar o nº da janela corrente. **/
    /** Variável predefinida como o tamanho total da "janela deslizante". **/
    /** Variável para guardar a lista de pacotes da janela deslizante. **/
    private int base;
    private static final int windowSize = 5;
    private final List<byte[]> listaPacotes;

    /** Variável para guardar o próximo nº de sequência. **/
    private int proxNumSeq;

    /** O diretoriaDestino/diretoria do ficheiro que será enviado. **/
    private final String diretoriaDestino;     // diretoria + nome do arquivo.

    /** Temporizador a ser usado em caso de falha de resposta. **/
    private Timer temporizador; // temporizador para a espera de resposta.
    private final Semaphore permissao;

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
        this.diretoriaDestino = localDisco;
        listaPacotes = new ArrayList<>(windowSize);
        transferenciaCompleta = false;
        DatagramSocket socketSaida, socketEntrada;
        permissao = new Semaphore(1);

        StringBuilder estabelecimentoConexao = new StringBuilder("Conexão Cliente Estabelecida");
        estabelecimentoConexao.append("\n");
        estabelecimentoConexao.append("Porta Destino dos Dados - ");
        estabelecimentoConexao.append(portaDestino);
        estabelecimentoConexao.append("\n");
        estabelecimentoConexao.append("Ficheiro de Dados enviado - ");
        estabelecimentoConexao.append(localDisco);
        estabelecimentoConexao.append("\n");
        System.out.println(estabelecimentoConexao);
 
        try {
            // Criação dos Sockets de Entrada e Saída.
            // Cliente vai receber pacotes ACK do Servidor pelo socketEntrada.
            // Cliente vai enviar pacotes UPD para o Servidor pelo socketSaida.
            socketEntrada = new DatagramSocket(portaEntrada);
            socketSaida = new DatagramSocket();

            // Criação das threads que depois vão processar os dados enviados e recebidos.
            Thread9999 threadACK = new Thread9999(socketEntrada);
            Thread7777 threadPacotes = new Thread7777(socketSaida, portaDestino, enderecoIP);

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
         * Com este método conseguimos dar permissao às outras threads em termos de janela deslizante.
         */
        public void run() {
            try {
                permissao.acquire();
                System.out.println("Cliente: Tempo em espera demasiado.");
                proxNumSeq = base;  // Faz reset ao número de sequência.
                permissao.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A classe interna para enviar informações para o servidor em modo Thread.
     */
    class Thread7777 extends Thread {
 
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
        Thread7777(DatagramSocket socketSaida, int portaDestino, String enderecoIP) throws UnknownHostException {
            this.socketSaida = socketSaida;
            this.portaDestino = portaDestino;
            this.enderecoIP = InetAddress.getByName(enderecoIP);
        }

        /**
         * Método (override) necessário para a Thread correr e conterá o código a ser corrido pela mesma.
         */
        public void run() {
            try {

                try (FileInputStream ficheiro = new FileInputStream(new File(diretoriaDestino))) {
                    while (!transferenciaCompleta) {    // Envia pacotes ao Servidor caso a janela nao esteja cheia.

                        if (proxNumSeq < base + (windowSize * tamanhoPDU)) {

                            // Bloqueia o permissao para a thread/pacote.
                            permissao.acquire();

                            // Se o pacote é o primeiro da janela deslizante - inicia temporizador.
                            if (base == proxNumSeq) {
                                // Cancela-se o temporizador caso não esteja ativo.
                                if(temporizador != null) temporizador.cancel();
                                // Inicialização temporizador.
                                temporizador = new Timer();
                                temporizador.schedule(new Temporizador(), 5000);
                            }

                            byte[] enviaDados;
                            // Identifica se em si é o últmo pacote.
                            // Coloca-se a 0 quando não há mais bytes para enviar.
                            int ultimoNumSeq = -1;

                            if (proxNumSeq < listaPacotes.size()) {
                                enviaDados = listaPacotes.get(proxNumSeq);
                            } else {
                                byte[] dataBuffer = new byte[tamanhoPDU];
                                int tamanhoDados = ficheiro.read(dataBuffer, 0, tamanhoPDU);
                                if (tamanhoDados == -1) {   // Sem dados para enviar - Pacote Enviado Vazio.
                                    ultimoNumSeq = 0;
                                    enviaDados = PacoteUDP.gerarPacoteDados(proxNumSeq, new byte[0]);
                                } else {    // Existencia de dados para enviar - Pacote Não Vazio.
                                    byte[] dataBytes = Arrays.copyOfRange(dataBuffer, 0, tamanhoDados);
                                    enviaDados = PacoteUDP.gerarPacoteDados(proxNumSeq, dataBytes);
                                }
                                listaPacotes.add(enviaDados);
                            }

                            // Envia o pacote de dados para o Servidor consoante o número de ACK que recebeu do mesmo.
                            socketSaida.send(new DatagramPacket(enviaDados, enviaDados.length, enderecoIP, portaDestino));

                            if(proxNumSeq==0) {
                                System.out.println("Pacote Inicial Enviado. Sinal dado ao Servidor.");
                            }
                            else{
                                StringBuilder pacoteEnviado = new StringBuilder("Cliente > ");
                                pacoteEnviado.append("Pacote número ");
                                pacoteEnviado.append(proxNumSeq);
                                pacoteEnviado.append(" enviado.");
                                System.out.println(pacoteEnviado);
                            }

                            // Atualiza número de sequência caso não esteja no fim.
                            // Atualizamos a 0 quando não há mais dados a enviar.
                            if (ultimoNumSeq!=0) {
                                proxNumSeq += tamanhoPDU;
                            }

                            // Liberta permissao para as outras threads/pacotes.
                            permissao.release();
                        }

                        sleep(2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    // Cancela-se um eventual temporizador caso esteja ativo.
                    if(temporizador != null) temporizador.cancel();
                    socketSaida.close();
                    System.out.println("Cliente > Ficheiro totalmente enviado.");
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
    class Thread9999 extends Thread {
 
        private final DatagramSocket socketEntrada;

        /**
         * Construtor parametrizado para a criação do Thread de Entrada.
         * @param socketEntrada O socket para onde irá entrar os pacotes ACK do servidor.
         */
        Thread9999(DatagramSocket socketEntrada) {
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
                        System.out.println("Cliente > ACK " + numACK);

                        // Se o pacote ACK recebido for duplicado.
                        if (base == numACK + tamanhoPDU) {
                            System.out.println("ACK duplicado.");

                            // Bloqueia o permissao para a thread/pacote.
                            permissao.acquire();

                            if(temporizador != null) temporizador.cancel();

                            proxNumSeq = base;
                            // Liberta permissao para outras threads/pacotes.
                            permissao.release();
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

                            // Bloqueia o permissao para a thread/pacote.
                            permissao.acquire();
                            if (base == proxNumSeq) {
                                if(temporizador != null) temporizador.cancel();
                            } else {
                                if(temporizador != null) temporizador.cancel();
                                temporizador = new Timer();
                                temporizador.schedule(new Temporizador(), 5000);
                            }
                            // Liberta permissao para outras threads/pacotes.
                            permissao.release();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    socketEntrada.close();
                    System.out.println("Cliente > Conexão encerrada.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
}