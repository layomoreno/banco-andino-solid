import java.util.List;

/** Notifica por todos los canales configurados (SMS, push, ...). Un canal nuevo = una clase nueva + una línea en Main. */
public class NotificadorCompuesto implements NotificadorTransferencias {
    private final List<NotificadorTransferencias> canales;
    public NotificadorCompuesto(List<NotificadorTransferencias> canales) { this.canales = canales; }

    @Override public void notificar(Transferencia t) {
        for (NotificadorTransferencias canal : canales) canal.notificar(t);
    }
}
