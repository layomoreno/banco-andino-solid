import java.util.List;

/** Sirve para cuentas, CDT, tarjetas y créditos: solo conoce Extractable. */
public class GeneradorExtractos {
    public void imprimir(List<? extends Extractable> productos) {
        for (Extractable p : productos) System.out.println(p.generarExtracto());
    }
}
