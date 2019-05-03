import java.nio.ByteBuffer;



public class PacoteUDP {

    static final int headerPDU = 4;

    //cria o pacote com numero de sequencia e os dados
    public static byte[] gerarPacoteDados(int numSeq, byte[] dadosByte) {
        return ByteBuffer.allocate(headerPDU + dadosByte.length).putInt(numSeq).put(dadosByte).array();
    }

    //cria o pacote com numero de ACK
    public static byte[] gerarPacoteACK(int numAck) {
        return ByteBuffer.allocate(headerPDU).putInt(numAck).array();
    }
}
