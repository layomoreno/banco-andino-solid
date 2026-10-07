/** R3: notificación push en la app. El texto vive aquí, no en el servicio que mueve el dinero. */
public class NotificadorPush implements NotificadorTransferencias {
    private final PushGateway gateway;
    public NotificadorPush(PushGateway gateway) { this.gateway = gateway; }

    @Override public void notificar(Transferencia t) {
        gateway.enviar(t.titularOrigen(),
            "Transferiste $" + t.monto() + " a la cuenta " + t.destino());
    }
}
