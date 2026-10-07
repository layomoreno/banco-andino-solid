import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** R3: SMS y push por cada transferencia. */
class NotificacionesTest {
    private final List<String> sms = new ArrayList<>();
    private final List<String> push = new ArrayList<>();

    private TransaccionService servicio() {
        NotificadorTransferencias canalSms = t -> sms.add(t.titularOrigen());
        NotificadorTransferencias canalPush = t -> push.add(t.titularOrigen());
        return new TransaccionService(
            List.of(new ReglaMontoPositivo()),
            new CalculadorComisionPorTipo(List.of(new ComisionMismoBanco())),
            t -> { }, t -> { },
            new NotificadorCompuesto(List.of(canalSms, canalPush)),
            t -> { });
    }

    @Test
    void cadaTransferenciaExitosaNotificaUnaVezPorSmsYUnaVezPorPush() {
        servicio().transferir(new CuentaAhorros("1", "Ana", 100_000),
                              new CuentaAhorros("2", "Luis", 0), 10_000, "MISMO_BANCO");
        assertEquals(1, sms.size());
        assertEquals(1, push.size());
    }

    @Test
    void unaTransferenciaRechazadaNoNotificaPorNingunCanal() {
        assertThrows(IllegalStateException.class, () ->
            servicio().transferir(new CuentaAhorros("1", "Ana", 1_000),
                                  new CuentaAhorros("2", "Luis", 0), 10_000, "MISMO_BANCO"));
        assertTrue(sms.isEmpty());
        assertTrue(push.isEmpty());
    }

    @Test
    void elTextoPushLlevaElMontoYElDestino() {
        List<String> mensajes = new ArrayList<>();
        PushGateway gateway = new PushGateway() {
            @Override public void enviar(String destinatario, String mensaje) { mensajes.add(destinatario + "|" + mensaje); }
        };
        new NotificadorPush(gateway).notificar(new Transferencia("MISMO_BANCO", "1", "Ana", "2", 10_000, 0));
        assertEquals("Ana|Transferiste $10000.0 a la cuenta 2", mensajes.get(0));
    }
}
