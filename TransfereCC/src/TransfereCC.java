import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import agenteUDP.*;

public class TransfereCC {


    public static void main(String[] args) {
        BufferedReader in
                = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("PUT (1) ou GET (2) ?");
        try {
            String escolha = in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*switch (escolha) {
            case 1:

        }*/
    }
}
