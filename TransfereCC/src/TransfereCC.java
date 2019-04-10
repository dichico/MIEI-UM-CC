import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class TransfereCC {

    /*public TransfereCC() {

    }*/

    public static void main(String[] args) {

        BufferedReader in
                = new BufferedReader(new InputStreamReader(System.in));


        System.out.println("Modo servidor ou cliente?");
        try {
            String escolha = in.readLine();

            switch (escolha) {
                case "Cliente":
                    System.out.println("Qual é o servidor que deseja criar conexão?");
                    String servidor = in.readLine();
                    AgenteUDP.inicializarConexao(servidor);
                case "Servidor":
                    AgenteUDP.aceitaConexao();
                    break;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*public static void main (String[] args) {
        (new Thread(new TransfereCC())).run();
    }*/
}
