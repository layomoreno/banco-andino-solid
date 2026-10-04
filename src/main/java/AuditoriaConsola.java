import java.time.LocalDateTime;

public class AuditoriaConsola implements Auditoria {
    @Override public void registrar(Transferencia t) {
        System.out.println("[AUDITORIA] " + LocalDateTime.now() + " " + t.tipo()
            + " " + t.origen() + " -> " + t.destino() + " $" + t.monto());
    }
}
