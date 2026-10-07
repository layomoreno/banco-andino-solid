/** R4: cada transacción exitosa se envía al sistema antifraude del banco (simulado en consola). */
public class AuditoriaAntifraude implements Auditoria {
    @Override public void registrar(Transferencia t) {
        System.out.println("[ANTIFRAUDE] Enviando transacción al sistema antifraude: " + t.tipo()
            + " " + t.origen() + " -> " + t.destino() + " $" + t.monto());
    }
}
