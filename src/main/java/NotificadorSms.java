/** Aquí vive el texto del SMS: cambiarlo no toca el servicio que mueve el dinero. */
public class NotificadorSms implements NotificadorTransferencias {
    private final SmsGateway gateway;
    public NotificadorSms(SmsGateway gateway) { this.gateway = gateway; }

    @Override public void notificar(Transferencia t) {
        gateway.enviar(t.titularOrigen(),
            "Transferiste $" + t.monto() + " a la cuenta " + t.destino());
    }
}
