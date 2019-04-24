import java.util.Scanner;

/**
 * Source-Code para a classe TransfereCC.
 * Classe responsável por ler os comandos e executá-los.
 * @author Diogo Araújo, Diogo Nogueira
 * @version 1.0
 */

public class TransfereCMD {

    // Portas UDP e de Estado (para trocas dos ACK).
    static final int portaUDP = 7777;
    static final int portaEstado = 9999;

    public static void main(String[] args) {

        Scanner teclado = new Scanner(System.in);

        String texto = teclado.nextLine();
        String[] argumentos = texto.split(" ");

        switch (argumentos[0]) {
            case "GET":
                // GET c:/users/cocos/....
                Servidor servidor = new Servidor(portaUDP, portaEstado, argumentos[1]);
                break;
            case "PUT":
                // PUT localhost c:/users/cocos/....
                Cliente cliente = new Cliente(portaUDP, portaEstado, argumentos[2], argumentos[1]);
                break;
        }
    }
}

